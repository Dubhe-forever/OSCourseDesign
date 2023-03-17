package pers.wxm.jobRequest;

import pers.wxm.clock.Clock_thread;
import pers.wxm.process.PCB;
import pers.wxm.memory.Memory;
import pers.wxm.jobRequest.Job;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JobIn_thread extends Thread {
    boolean flag = true;    //1s只执行一次
    public static int JobInFirst = 0;
    public List<Job> JobList = new ArrayList<>();   //存放用户作业的地方
    public int JobListIndex = 0;

    @Override
    public void run() {
        while (true) {
            //如果用到Clock必须要锁住再用！！
            synchronized (Clock_thread.class) {
                try {
                    CheckJob();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void CheckJob() throws IOException {
        if (Clock_thread.COUNTTIME % 10 == 0 && Clock_thread.COUNTTIME != 0) {
            if (flag) {
                //System.out.println("Job in!");
                //判断作业是否进入后备队列
                if (JobListIndex < JobList.size()) {//保证下标不越界，不超过作业数量，可以满足实时作业请求。实时作业进来时更新JobList即可
                    while (Clock_thread.COUNTTIME >= JobList.get(JobListIndex).Intimes) {
//                        PCB pcb = new PCB();    //一个进程一个PCB
//                        pcb.ProID = JobList.get(JobListIndex).JobsID;
//                        pcb.Priority = JobList.get(JobListIndex).Priority;
//                        pcb.InTimes = Clock_thread.COUNTTIME;//需要考虑一下！！
//                        pcb.InstrucNum = JobList.get(JobListIndex).InstrucNum;
//
//                        //如果有实时作业请求的话，生成PCB后，指令内容需要再写一个函数，已循环的方式随机生成指令并放到InstrucSet中
//                        pcb.IR = ReadInstruc(String.valueOf(JobList.get(JobListIndex).JobsID));
                        //将Job放入外存后备队列
                        Memory.Fallback.add(JobList.get(JobListIndex));

                        JobListIndex++;
                        if (JobListIndex == JobList.size())
                            break;
                    }
                }
                //System.out.println(pers.wxm.memory.Memory.ReadyJobPCB);
                flag = false;
                JobIn_thread.JobInFirst = (JobIn_thread.JobInFirst + 1) % 2;
            }
        } else flag = true;
    }

    public void ReadFile(String path) throws IOException {
        FileReader fileReader = new FileReader(path);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            String[] s = line.split(",");
            if (s.length == 4) {
                //创建作业信息，放入作业队列
                Job job = new Job(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()), Integer.parseInt(s[2].trim()), Integer.parseInt(s[3].trim()));
                JobList.add(job);
                //System.out.println(Clock_thread.COUNTTIME+":[新增作业:"+s[0]+","+s[2]+","+s[3].trim()+"]");
            }
        }
        //System.out.println(JobList);
    }

    static public int ReadInstruc(String JobNO) throws IOException {
        //某进程的指令集
        List<Instructions> InstrucListPiece = new ArrayList<>();
        //读文件里的指令
        String path = "../input1/" + JobNO + ".txt";
        FileReader fileReader = new FileReader(path);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            String[] s = line.split(",");
            if (s.length == 3) {
                Instructions instructions = new Instructions(); //某一条指令
                instructions.JobsID = Integer.parseInt(JobNO);
                instructions.Instruc_ID = Integer.parseInt(s[0].trim());
                instructions.Instruc_State = Integer.parseInt(s[1].trim());
                instructions.L_Address = Integer.parseInt(s[2].trim());
                //将这条指令加到该进程的指令集中
                InstrucListPiece.add(instructions);
            }
        }
        //将该进程的指令集加入到->内存中的全部指令集
        Memory.InstrucSet.add(InstrucListPiece);
        //每次查询指令信息时，先通过进程PCB中的ID查询到该进程的指令集InstrucListPiece，再结合PCB中IR下标查询到当前执行到哪条指令
        //System.out.println(Memory.InstrucSet);
        return Memory.InstrucSet.size() - 1;
    }
}
