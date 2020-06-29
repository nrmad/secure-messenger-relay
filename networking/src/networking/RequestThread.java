package networking;

import packets.Packet;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.interrupted;

public class RequestThread implements Runnable {

    private SSLServerSocket sslServerSocket;
    private final HashMap<Integer,ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap;
    private final Set<String> usernames;
    private int authIterations;

    public RequestThread(SSLServerSocket sslServerSocket, HashMap<Integer, ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, Set<String> usernames, int authIterations){
        this.sslServerSocket = sslServerSocket;
        this.networkMap = networkMap;
        this.usernames = usernames;
        this.authIterations = authIterations;
    }

    public void run(){

        // PERHAPS CONFIGURABLE IN NETWORK CONFIGURATION
        ExecutorService registerRequestThreads = Executors.newFixedThreadPool(100);

            while (!interrupted()) {
                try {
                    SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                    registerRequestThreads.submit(new RequestHandlerThread(sslSocket, networkMap, usernames, authIterations));
                }catch (IOException | SQLException e){}
            }
            // SHUTDOWN CODE HERE
    }

}
