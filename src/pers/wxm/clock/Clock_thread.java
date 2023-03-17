package pers.wxm.clock;

public class Clock_thread extends Thread{
    static public int COUNTTIME;

    @Override
    public void run() {
        while (true){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //锁住
            synchronized (Clock_thread.class){
                COUNTTIME++;
                //System.out.println(COUNTTIME);
            }
        }
    }
}
