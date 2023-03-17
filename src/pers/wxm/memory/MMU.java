package pers.wxm.memory;

public class MMU {
    //前4块是放进程PCB，后12块随机绑定页号块号


    public MMU() {
        CreatePageTable();
    }

    public void CreatePageTable() {
        for (int i = 0; i < 16; i++) {
            Memory.PageTable[i] = -2;
        }
    }
}
