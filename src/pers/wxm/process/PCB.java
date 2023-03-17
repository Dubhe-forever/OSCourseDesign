package pers.wxm.process;

import java.util.ArrayList;
import java.util.List;

public class PCB implements Comparable<PCB>{
    public int ProID;   //进程编号
    public Integer Priority;    //进程优先级
    public int InTimes; //创建时间
    public int EndTimes;    //结束时间
    public String PSW;  //状态
    public int RunTimes;    //运行时间
    public int TurnTimes;   //周转时间
    public int InstrucNum;  //包含的指令数目
    public int PC;   //程序计数器，记录执行到第几条指令了
    public int IR;   //指令计数器，InstrucSet中存放的位置
    public int RqNum;   //就绪队列or阻塞队列位置编号
    public int RqTimes; //进入就绪队列or阻塞队列时间
    public boolean Interruption;    //2s指令中是否只执行了1s
    public List<BlockandPage> blockandPages;
    public int blockSetNumber;
    public int blockIndex;

    public PCB(){
        this.IR = 0;
        this.RunTimes = 0;
        this.PC = 0;
        this.Interruption = false;
        this.blockIndex = 0;
        this.blockandPages = new ArrayList<>(3);
    }

    public int getPriority(){
        return this.Priority;
    }


    @Override
    public int compareTo(PCB o) {
        return this.Priority.compareTo(o.Priority);
    }
}
