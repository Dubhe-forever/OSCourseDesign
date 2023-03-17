package pers.wxm.process;

import pers.wxm.clock.Clock_thread;
import pers.wxm.memory.MMU;
import pers.wxm.memory.Memory;

public class PageInterruption_thread extends Thread {
    @Override
    public void run() {
        while (true) {
            synchronized (ProcessScheduling_thread.class) {
                if (ProcessScheduling_thread.PageInterruptFlag) {
                    PageOut();
                    PageIn();
                    //System.out.println("完成页面置换");
                    //缺页中断停止
                    ProcessScheduling_thread.PageInterruptFlag = false;
                }
            }
        }
    }

    public void PageIn() {
        //用LRU算法
        //物理地址
        int PAddress = -1;
        int i = 4;
        //首先判断PageTable里有没有已经存在的页号
        for (; i < Memory.PageTable.length; i++) {
            //如果有
            if (Memory.PageTable[i] == ProcessScheduling_thread.LAddress) {
                PAddress = i;
                break;
            }
        }
        //如果PageTable里没有存在的页号
        if (i == Memory.PageTable.length) {
            for (int j = 3; j < Memory.PageTable.length; j++) {
                //如果有空位置
                if (Memory.PageTable[j] == -2) {
                    PAddress = j;
                    break;
                }
            }
        }
        //如果当前执行的进程3页空间未满，说明页表之前也没有满，PAddress一定不为-1
        //其实不一定
        //System.out.println("当前进程已绑定的页块号多少："+Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.size());
        if (Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.size() <= 3) {
            System.out.println(Clock_thread.COUNTTIME + ":[缺页中断:" + Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).ProID + ":"
                    + Memory.PageTable[PAddress] + "," + ProcessScheduling_thread.LAddress+"]");
            BlockandPage temp = new BlockandPage();
            temp.pageNum = ProcessScheduling_thread.LAddress;
            temp.blockNum = PAddress;
            Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.add(temp);

        } else {
            //进程3块空间满
            if (PAddress == -1) {
                //页表没找到而且满了
                //拟换出的块号
                int blockIndex = Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.get(0).blockNum;
                //拟换出的页号
                int pageIndex = Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.get(0).pageNum;
                //输出缺页中断
                System.out.println(Clock_thread.COUNTTIME + ":[缺页中断:" + Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).ProID + ":"
                        + pageIndex + "," + ProcessScheduling_thread.LAddress+"]");
                Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.remove(0);
                BlockandPage temp = new BlockandPage();
                temp.pageNum = ProcessScheduling_thread.LAddress;
                temp.blockNum = blockIndex;
                Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.add(temp);
            } else {
                //页表找到了。先移除，后放入
                //拟换出的块号
                //System.out.println("页表找到了");
                int pageIndex = Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.get(0).pageNum;
                //输出缺页中断
                System.out.println(Clock_thread.COUNTTIME + ":[缺页中断:" + Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).ProID + ":"
                        + pageIndex + "," + ProcessScheduling_thread.LAddress+"]");
                Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.remove(0);
                BlockandPage temp = new BlockandPage();
                temp.pageNum = ProcessScheduling_thread.LAddress;
                temp.blockNum = PAddress;
                Memory.ReadyJobPCB.get(ProcessScheduling_thread.runIndex).blockandPages.add(temp);
            }
        }
    }

    public void PageOut() {

    }
}
