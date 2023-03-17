package pers.wxm.jobRequest;

public class Job {
    public int JobsID;
    public int Priority;
    public int Intimes;
    public int InstrucNum;

    //默认构造函数
    Job(){
        this.JobsID = 0;
        this.Priority = 0;
        this.Intimes = -1;
        this.InstrucNum = 0;
    }

    Job(int JobsID,int Priority,int Intimes, int InstrucNum){
        this.JobsID = JobsID;
        this.Priority = Priority;
        this.Intimes = Intimes;
        this.InstrucNum = InstrucNum;
    }

    public int getPriority(){
        return Priority;
    }
}
