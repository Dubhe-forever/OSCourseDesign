package pers.wxm.main;

import pers.wxm.clock.Clock_thread;
import pers.wxm.instrucThread.InputBlock_thread;
import pers.wxm.instrucThread.OutputBlock_thread;
import pers.wxm.jobRequest.JobIn_thread;
import pers.wxm.memory.MMU;
import pers.wxm.memory.Memory;
import pers.wxm.process.PageInterruption_thread;
import pers.wxm.process.ProcessScheduling_thread;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        //初始化配置
        for (int i = 0; i < 4; i++) {
            Memory.BlockSet[i] = false;
        }
        MMU mmu = new MMU();

        Clock_thread clock_thread = new Clock_thread();
        clock_thread.start();
        JobIn_thread jobIn_thread = new JobIn_thread();
        jobIn_thread.ReadFile("../input1/jobs-input.txt");
        jobIn_thread.start();
        ProcessScheduling_thread processScheduling_thread = new ProcessScheduling_thread();
        processScheduling_thread.start();
        PageInterruption_thread pageInterruption_thread = new PageInterruption_thread();
        pageInterruption_thread.start();
        InputBlock_thread inputBlock_thread = new InputBlock_thread();
        inputBlock_thread.start();
        OutputBlock_thread outputBlock_thread = new OutputBlock_thread();
        outputBlock_thread.start();
    }
}