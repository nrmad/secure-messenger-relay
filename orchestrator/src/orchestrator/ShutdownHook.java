package orchestrator;

import java.util.concurrent.ExecutorService;

public class ShutdownHook extends Thread {

    private ExecutorService threadManager;

    public ShutdownHook(ExecutorService threadManager){
        this.threadManager = threadManager;
    }

    public void run(){
        threadManager.shutdownNow();
    }


}
