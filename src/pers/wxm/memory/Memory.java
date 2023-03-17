package pers.wxm.memory;

import pers.wxm.jobRequest.Job;
import pers.wxm.process.PCB;
import pers.wxm.jobRequest.Instructions;

import java.util.*;

public class Memory {
    //内存区
    static public Integer[] PageTable = new Integer[16];
    //后备队列
    static public Queue<Job> Fallback = new PriorityQueue<>(
            Comparator.comparingInt(Job::getPriority)    //降序
    );
    //就绪队列中进程排序     PS：List增长规则：（（n*3）/2）+1
    static public List<PCB> ReadyJobPCB = new ArrayList<>(4);
    static public List<List<Instructions>> InstrucSet = new ArrayList<>();
    //内存块，BlockSet是限制只有四个进程进入；
    static public Boolean[] BlockSet = new Boolean[4];
    //阻塞队列
    static public List<PCB> BlockingList1 = new LinkedList<>();
    static public List<PCB> BlockingList2 = new LinkedList<>();
    static public List<PCB> BlockingList4 = new LinkedList<>();
    static public List<PCB> BlockingList5 = new LinkedList<>();
    static public List<Integer> Instruc2Back = new LinkedList<>();
    static public List<Integer> Instruc3Back = new LinkedList<>();
}
