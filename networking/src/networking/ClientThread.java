package networking;

import packets.Packet;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Phaser;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final Set<String> usernames;
    private final HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap ;
    private final int authIterations;

    public ClientThread(SSLSocket sslSocket, Set<String> usernames, HashMap<Integer,ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, int authIterations){
    this.sslSocket = sslSocket;
    this.usernames = usernames;
    this.networkMap = networkMap;
    this.authIterations = authIterations;
    }

    public void run() {
        try {
            BlockingQueue<Packet> channel = new ArrayBlockingQueue<>(100);
            Phaser conPhaser = new Phaser(2);
             Thread receiver = new Thread(new ReceiverClientThread(sslSocket, networkMap, channel,
                     authIterations, usernames, conPhaser));
             Thread sender = new Thread(new SenderClientThread(sslSocket, networkMap, channel, conPhaser));

              receiver.start();
              sender.start();
                 try {
                     sender.join();
                 } catch (InterruptedException e) {
                     receiver.interrupt();
                     sender.interrupt();
                     sslSocket.close();
                 }


        } catch ( SQLException | IOException e) {
            // LOGGING HERE ON PRODUCTION
            e.printStackTrace();
        }
    }
}
