package pers.wxm.process;

import pers.wxm.clock.Clock_thread;
import pers.wxm.jobRequest.Job;
import pers.wxm.jobRequest.JobIn_thread;
import pers.wxm.memory.MMU;
import pers.wxm.memory.Memory;

import java.io.IOException;
import java.util.*;

public class ProcessScheduling_thread extends Thread {
    int JobInFirst = 0;
    private int OwnClock = 0;
    //是否可中断，用于1、2、3类指令
    boolean InstrucTime = false;
    //是否可中断，用于5、6类指令
    boolean InstrucTime5 = false;
    boolean InstrucTime6 = false;
    private int Times;  //时间片
    static public boolean PageInterruptFlag = false;    //缺页中断脉冲
    //static public PCB PageInterruptPCB; //发生缺页中断的进程
    static public int BlockIndex;   //拟替换的块号
    static int runIndex = 0;//当前获得执行权的进程在就绪队列的下标
    static public int runTurn = 1;//拥有相同最高优先级数量
    static int LAddress;
    static public boolean SchedulingEnd = false;
    static public boolean SchedulingEnd3 = false;


    public void run() {
        while (true) {
            synchronized (Clock_thread.class) {
                if (OwnClock != Clock_thread.COUNTTIME) {
                    if (Clock_thread.COUNTTIME % 10 == 0 && Clock_thread.COUNTTIME != 0) {
                        //每当时钟为10的倍数是，先等JobIn线程做完，进程调度再做
                        if (JobInFirst != JobIn_thread.JobInFirst) {
                            try {
                                ProcessScheduling();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            OwnClock = Clock_thread.COUNTTIME;
                            JobInFirst = JobIn_thread.JobInFirst;
                        }
                    } else {
                        try {
                            ProcessScheduling();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        OwnClock = Clock_thread.COUNTTIME;
                    }
                }
            }
        }

    }

    //进程调度算法
    public void ProcessScheduling() throws IOException {
        SchedulingEnd = false;
        SchedulingEnd3 = false;
        //每秒查看后备队列情况
        //System.out.println("内存就绪队列进程数量：" + Memory.ReadyJobPCB.size());
        //System.out.println("外存后备队列作业数量：" + Memory.Fallback.size());
//        for (int i = 0; i < 4; i++) {
//            System.out.print(Memory.BlockSet[i]+" ");
//        }
        //System.out.println("runTurn:"+runTurn);

        if (InstrucTime || InstrucTime5 || InstrucTime6) {
            //不可中断秒，指令1、5、6
            RunInstruction(Memory.ReadyJobPCB.get(runIndex));
            Memory.ReadyJobPCB.get(runIndex).PC++;
            //判断作业是否执行完
            //如果进程执行完
            if (Memory.ReadyJobPCB.get(runIndex).PC >= Memory.ReadyJobPCB.get(runIndex).InstrucNum) {
                System.out.println(Clock_thread.COUNTTIME+":["+Memory.ReadyJobPCB.get(runIndex).ProID
                +","+Memory.ReadyJobPCB.get(runIndex).InTimes+","+ (Clock_thread.COUNTTIME -
                        Memory.ReadyJobPCB.get(runIndex).InTimes)+"]");
                System.out.println(Clock_thread.COUNTTIME+":[终止进程"+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                //位置空出来
                Memory.BlockSet[Memory.ReadyJobPCB.get(runIndex).blockIndex] = false;
                //页表空出
                Memory.PageTable[Memory.ReadyJobPCB.get(runIndex).blockIndex] = -2;
                //就绪队列移出
                Memory.ReadyJobPCB.remove(runIndex);
                //runTurn 更新
                runTurn--;
                if (runTurn == 0) {
                    runTurn = 1;
                }
                //获得执行权下标更新
                if (runIndex == runTurn)
                    runIndex = 0;
            } else if (!CheckSchedulingTimes(2))
                //没有时间，轮询作业
                runIndex = (runIndex + 1) % runTurn;
        } else {
            //可中断秒
            //如果内存仍有空间，后备队列有作业，则放入就绪队列
            for (int i = 0; i < 4; i++) {
                if (Memory.Fallback.size() != 0 && !Memory.BlockSet[i]) {
                    //将后备队列队头作业取出并创建PCB
                    PCB pcb = new PCB();
                    Job temp = Memory.Fallback.poll();
                    pcb.ProID = temp.JobsID;
                    pcb.Priority = temp.Priority;
                    pcb.InTimes = Clock_thread.COUNTTIME;
                    pcb.InstrucNum = temp.InstrucNum;
                    //读取该进程的指令内容，并返回存放位置->InstrucSet的index
                    pcb.IR = JobIn_thread.ReadInstruc(String.valueOf(temp.JobsID));
                    //分配块簇
                    pcb.blockIndex = i;
                    Memory.BlockSet[i] = true;
                    //PCB-块分配
                    Memory.PageTable[i] = temp.JobsID;

                    //放入就绪队列
                    Memory.ReadyJobPCB.add(pcb);
                    //就绪队列排序
                    Collections.sort(Memory.ReadyJobPCB);
                    //记录需要几个进程进行轮询
                    runTurn = 1;
                    for (int j = 1; j < Memory.ReadyJobPCB.size(); j++) {
                        if (Objects.equals(Memory.ReadyJobPCB.get(j).Priority, Memory.ReadyJobPCB.get(0).Priority)) {
                            runTurn++;
                        } else
                            break;
                    }
                    //System.out.println("已完成排序");
                }
            }
            //如果就绪队列有进程
            if (Memory.ReadyJobPCB.size() != 0) {
                PCB current = Memory.ReadyJobPCB.get(runIndex);
                //System.out.println("ProID:" + current.ProID);

                //执行指令
                RunInstruction(current);

            } else
                System.out.println(Clock_thread.COUNTTIME+":[CPU空闲]");
        }
        SchedulingEnd = true;
        SchedulingEnd3 = true;
    }

    //校正时间片大小。当前时间片-指令执行所需的时间，若时间片小于等于0，则发生进程调度
    public boolean CheckSchedulingTimes(int needTime) {
        //System.out.println("正在update时间片");
        Times = Times - needTime;
        //如果时间片<=0，发生进程调度
        if (Times <= 0) {
            Times = 3;
            return false;
        }
        return true;
        //System.out.println("时间片更新完成");
    }

    //获取时间片大小信息
    public int GetTimes() {
        return Times;
    }

    //获取并执行指令，有多种情况
    public void RunInstruction(PCB current) throws IOException {
        //获取指令类型
        int state = Memory.InstrucSet.get(current.IR).get(current.PC).Instruc_State;
        if (InstrucTime) {
            //指令1的不可中断
            System.out.println(Clock_thread.COUNTTIME + ":[运行进程:" + current.ProID + ":" + Memory.InstrucSet.get(current.IR).get(current.PC).Instruc_ID + ","
                    + state + "," + Memory.InstrucSet.get(current.IR).get(current.PC).L_Address + "]");

            //CheckSchedulingTimes(2);
            InstrucTime = false;
        } else if (InstrucTime5) {
            //指令5的不可中断
            System.out.println(Clock_thread.COUNTTIME + ":[读文件]");
            //CheckSchedulingTimes(2);
            Memory.BlockingList4.remove(0);
            InstrucTime5 = false;
        } else if (InstrucTime6) {
            System.out.println(Clock_thread.COUNTTIME + ":[写文件]");
            //CheckSchedulingTimes(2);
            Memory.BlockingList5.remove(0);
            InstrucTime6 = false;
        } else {
            //可中断秒，指令类型0、2、3和1、5、6开头
            //获取逻辑地址
            LAddress = Memory.InstrucSet.get(current.IR).get(current.PC).L_Address;
            int i = 0;
            //遍历寻找页框中是否有对应块号
            for (; i < current.blockandPages.size(); i++) {
                if (current.blockandPages.get(i).pageNum == LAddress) {
                    break;
                }
            }
            //如果没有对应块号
            if (i == current.blockandPages.size()) {
                //没有需要的页，发生缺页中断
                //System.out.println("发生缺页中断");
                //页面替换线程脉冲
                PageInterruptFlag = true;
            } else {
                //有对应块号，更新block里的顺序，将最新的换到里blockIndex最远的地方，即blockIndex--的位置.LRU
                BlockandPage temp = Memory.ReadyJobPCB.get(runIndex).blockandPages.remove(i);
                Memory.ReadyJobPCB.get(runIndex).blockandPages.add(temp);
            }


            //先进行缺页中断，结束再执行指令
            while(PageInterruptFlag){
                System.out.print("");
            }

            if(state != 1 || Times > 1){
                System.out.println(Clock_thread.COUNTTIME + ":[运行进程:" + current.ProID + ":" + Memory.InstrucSet.get(current.IR).get(current.PC).Instruc_ID + ","
                        + state + "," + LAddress + "]");
            }

            switch (state) {
                case 0:
                    //需要更新就绪队列队首进程PCB的PC，不是current！
                    //System.out.println("执行指令0");
                    Memory.ReadyJobPCB.get(runIndex).PC++;
                    //判断是否执行完
                    //如果进程执行完
                    if (current.PC >= current.InstrucNum) {
                        System.out.println(Clock_thread.COUNTTIME+":["+Memory.ReadyJobPCB.get(runIndex).ProID
                                +","+Memory.ReadyJobPCB.get(runIndex).InTimes+","+ (Clock_thread.COUNTTIME -
                                Memory.ReadyJobPCB.get(runIndex).InTimes)+"]");
                        System.out.println(Clock_thread.COUNTTIME+":[终止进程"+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                        //位置空出来
                        Memory.BlockSet[Memory.ReadyJobPCB.get(runIndex).blockIndex] = false;
                        //页表空出
                        Memory.PageTable[Memory.ReadyJobPCB.get(runIndex).blockIndex] = -2;
                        //就绪队列移出
                        Memory.ReadyJobPCB.remove(runIndex);
                        //runTurn 更新
                        runTurn--;
                        if (runTurn == 0) {
                            runTurn = 1;
                        }
                        //获得执行权下标更新
                        if (runIndex == runTurn)
                            runIndex = 0;
                    } else if (!CheckSchedulingTimes(1))
                        //没有时间，轮询作业
                        runIndex = (runIndex + 1) % runTurn;
                    break;
                case 1:
                    if (CheckSchedulingTimes(2)) {
                        //够执行两秒
                        //System.out.println("执行指令1");
                        InstrucTime = true;
                        break;
                    } else {
                        //不够执行两秒，直接进行进程调度
                        CheckSchedulingTimes(3);
                        ProcessScheduling();
                        break;
                    }
                case 2:
                    //System.out.println("执行指令2");
                    //Memory.ReadyJobPCB.get(runIndex).PC++;
                    //PC++写到InputBlock_thread里，输出返回就绪队列后，判断进程是否执行完，执行完就不用回就绪队列了，直接做
                    System.out.println(Clock_thread.COUNTTIME+":[阻塞队列:"+"阻塞队列1"+","+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                    //进程执行完的修改动作
                    Memory.BlockingList1.add(Memory.ReadyJobPCB.remove(runIndex));
                    //runTurn 更新
                    //System.out.println("runturn:"+runTurn);
                    runTurn--;
                    if (runTurn == 0) {
                        runTurn = 1;
                    }
                    //获得执行权下标更新
                    if (runIndex == runTurn)
                        runIndex = 0;

                    Memory.Instruc2Back.add(Clock_thread.COUNTTIME + 1);
                    CheckSchedulingTimes(3);
                    //进行进程调度
                    ProcessScheduling();
                    break;
                case 3:
                    //System.out.println("执行指令3");
                    //Memory.ReadyJobPCB.get(runIndex).PC++;
                    //同case 2
                    System.out.println(Clock_thread.COUNTTIME+":[阻塞队列:"+"阻塞队列2"+","+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                    Memory.BlockingList2.add(Memory.ReadyJobPCB.remove(runIndex));
                    //runTurn 更新
                    runTurn--;
                    if (runTurn == 0) {
                        runTurn = 1;
                    }
                    //获得执行权下标更新
                    if (runIndex == runTurn)
                        runIndex = 0;

                    Memory.Instruc3Back.add(Clock_thread.COUNTTIME + 1);
                    CheckSchedulingTimes(3);
                    //进程调度
                    ProcessScheduling();
                    break;
                case 5:
                    //System.out.println("执行指令5");
                    System.out.println(Clock_thread.COUNTTIME+":[阻塞队列:"+"阻塞队列4"+","+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                    Memory.BlockingList4.add(Memory.ReadyJobPCB.get(runIndex));
                    //后备队列不用将作业放入就绪队列
                    InstrucTime5 = true;
                    break;
                case 6:
                    //System.out.println("执行指令6");
                    System.out.println(Clock_thread.COUNTTIME+":[阻塞队列:"+"阻塞队列5"+","+Memory.ReadyJobPCB.get(runIndex).ProID+"]");
                    Memory.BlockingList5.add(Memory.ReadyJobPCB.get(runIndex));
                    InstrucTime6 = true;
                    break;
            }
        }
    }

    //此处动作是为了防止实时作业的优先级高于就绪队列中的进程，需要进程调度。检查后备队列队首节点的优先级与就绪队列中的优先级哪个高，是否会发生进程调度。
//    public void CheckPriority() {
//        System.out.println("正在检查优先级");
//        //就绪队列的各个进程取下来，
//        List<PCB> ReadyQueueTemp = new ArrayList<>();
//        while (Memory.ReadyJobPCB.size() != 0) {
//            ReadyQueueTemp.add(Memory.ReadyJobPCB.poll());
//        }
//        //ReadyQueueFirstPriority
//        int RQFP = ReadyQueueTemp.get(0).Priority;
//        //ReadyQueueLastPriority
//        int RQLP = ReadyQueueTemp.get(ReadyQueueTemp.size() - 1).Priority;
//        //FallbackFirstPriority
//        assert Memory.Fallback.peek() != null;
//        int FFP = Memory.Fallback.peek().Priority;
//        if (FFP > RQLP) {
//            for (int i = 0; i < ReadyQueueTemp.size() - 1; i++) {
//                Memory.ReadyJobPCB.add(ReadyQueueTemp.get(i));
//            }
//            //簇号交换
//            Memory.Fallback.peek().blockSetNumber = ReadyQueueTemp.get(ReadyQueueTemp.size() - 1).blockSetNumber;
//            Memory.ReadyJobPCB.add(Memory.Fallback.poll());
//            //就绪队列最后一个调出
//            Memory.Fallback.add(ReadyQueueTemp.get(ReadyQueueTemp.size() - 1));
//            //System.out.println("First:"+Memory.ReadyJobPCB.peek().ProID);
//            if (FFP > RQFP) {
//                //实时作业优先级最高，时间片更新
//                Times = 3;
//            }
//            System.out.println("队列更新完成");
//        } else {
//            //不变
//            Memory.ReadyJobPCB.addAll(ReadyQueueTemp);
//        }
//        System.out.println("优先级检查完毕");
//    }
}
