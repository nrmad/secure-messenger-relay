package orchestrator;

import java.util.List;

public class ShutdownHook extends Thread {

    private List<Thread> threads;

    public ShutdownHook(List<Thread> threads){
        this.threads = threads;
    }

    public void run(){
        for (Thread thread: threads) {
            thread.interrupt();
        }
    }


}
