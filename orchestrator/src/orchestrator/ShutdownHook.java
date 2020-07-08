package orchestrator;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class ShutdownHook extends Thread {

    private ExecutorService threadManager;
    private SSLServerSocket sslServerSocket, sslRegServerSocket;

    public ShutdownHook(ExecutorService threadManager, SSLServerSocket sslServerSocket, SSLServerSocket sslRegServerSocket){
        this.threadManager = threadManager;
        this.sslServerSocket = sslServerSocket;
        this.sslRegServerSocket = sslRegServerSocket;
    }

    public void run(){
        threadManager.shutdownNow();
        try {
            sslRegServerSocket.close();
            sslServerSocket.close();
        }catch (IOException e){}
    }


}
