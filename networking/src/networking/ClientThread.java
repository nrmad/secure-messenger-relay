package networking;

import javax.net.ssl.SSLSocket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap ;
    private final int authIterations;

    public ClientThread(SSLSocket sslSocket, HashMap<Integer,ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, int authIterations){
    this.sslSocket = sslSocket;
    this.networkMap = networkMap;
    this.authIterations = authIterations;
    }

    public void run() {
        try {
            // ??? MAYBE HAVE THE QUEUE CONFIGURABLE
            BlockingQueue<Packet> channel = new ArrayBlockingQueue<>(100);

             Thread receiver = new Thread(new ReceiverClientThread(sslSocket, networkMap, channel, authIterations));
             Thread sender = new Thread(new SenderClientThread(sslSocket, networkMap, channel));

              receiver.start();
              sender.start();
                 try {
                     sender.join();
                 } catch (InterruptedException e) {
                     receiver.interrupt();
                     sender.interrupt();
                 }


        } catch ( SQLException e) {
            // LOGGING HERE ON PRODUCTION
            e.printStackTrace();
        }
    }
}
