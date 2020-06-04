package networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.interrupted;

public class RequestThread implements Runnable {

    private final int port;
    private final HashMap<Integer,ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>> networkMap;

    public RequestThread(int port,  HashMap<Integer,ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>> networkMap){
        this.port = port;
        this.networkMap = networkMap;
    }

    public void run(){

        // PERHAPS CONFIGURABLE IN NETWORK CONFIGURATION
        ExecutorService registerRequestThreads = Executors.newFixedThreadPool(100);
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (!interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    registerRequestThreads.submit(new RegisterRequestThread(socket, networkMap));
                }catch (IOException e){}
            }

        }catch (IOException e){}
    }

}
