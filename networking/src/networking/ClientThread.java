package networking;

import datasource.ReadPropertiesFile;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap;
    private final int nid;
    private final int authIterations;

    public ClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap,
                        int nid, int authIterations){
    this.sslSocket = sslSocket;
    this.channelMap = channelMap;
    this.nid = nid;
    this.authIterations = authIterations;
    }

    public void run() {
        try {
            // ??? MAYBE HAVE THE QUEUE CONFIGURABLE
            BlockingQueue<Packet> channel = new ArrayBlockingQueue<>(100);
            // ??? DOES THIS CALCULATE THE CORRECT CID
//            Certificate[] serverCerts = sslSocket.getSession().getPeerCertificates();
//            String cid = SecurityUtilities.calculateFingerprint(serverCerts[0].getEncoded());

             Thread receiver = new Thread(new ReceiverClientThread(sslSocket, channelMap, channel, nid, authIterations));
             Thread sender = new Thread(new SenderClientThread(sslSocket, channelMap, channel));

             if(!channelMap.get(cid).isPresent()) {
              receiver.start();
              sender.start();
                 try {
                     sender.join();
                 } catch (InterruptedException e) {
                     receiver.interrupt();
                     sender.interrupt();
                 }
             }

        } catch (GeneralSecurityException | IOException | SQLException e) {
            // LOGGING HERE ON PRODUCTION
            e.printStackTrace();
        }
    }
}
