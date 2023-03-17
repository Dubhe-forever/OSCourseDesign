package pers.wxm.instrucThread;

import pers.wxm.clock.Clock_thread;
import pers.wxm.memory.Memory;
import pers.wxm.process.ProcessScheduling_thread;

import java.util.Collections;
import java.util.Objects;

public class OutputBlock_thread extends Thread {
    public void run() {
        while (true) {
            synchronized (ProcessScheduling_thread.class) {

                for (int i = 0; i < Memory.Instruc3Back.size(); i++) {
                    while (Memory.Instruc3Back.size() != 0 && Memory.Instruc3Back.get(0) >= Clock_thread.COUNTTIME && ProcessScheduling_thread.SchedulingEnd3) {
                        System.out.println("返回队列");
                        //PC更新
                        Memory.BlockingList2.get(0).PC++;
                        //清空时间
                        Memory.Instruc3Back.remove(0);
                        //指令2、3恰好为最后一条指令
                        if (Memory.BlockingList2.get(0).PC >= Memory.BlockingList2.get(0).InstrucNum) {
                            System.out.println(Memory.BlockingList2.get(0).ProID + "作业执行完");
                            //位置空出来
                            Memory.BlockSet[Memory.BlockingList2.get(0).blockIndex] = false;
                            //页表空出
                            Memory.PageTable[Memory.BlockingList2.get(0).blockIndex] = -2;
                            //移出阻塞队列
                            Memory.BlockingList2.remove(0);
                        } else {
                            //放回就绪队列
                            Memory.ReadyJobPCB.add(Memory.BlockingList2.remove(0));
                            //排序
                            Collections.sort(Memory.ReadyJobPCB);
                            ProcessScheduling_thread.runTurn = 1;
                            for (int j = 1; j < Memory.ReadyJobPCB.size(); j++) {
                                if (Objects.equals(Memory.ReadyJobPCB.get(j).Priority, Memory.ReadyJobPCB.get(0).Priority)) {
                                    ProcessScheduling_thread.runTurn++;
                                } else
                                    break;
                            }
                            System.out.println("已完成排序");
                        }

                    }
                }
                ProcessScheduling_thread.SchedulingEnd3 = false;
            }

        }

    }
}
