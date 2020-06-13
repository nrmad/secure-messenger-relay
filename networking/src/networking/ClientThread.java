package networking;

import security.SecurityUtilities;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap;

    public ClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap){
    this.sslSocket = sslSocket;
    this.channelMap = channelMap;
    }

    public void run() {
        try {
            // ??? MAYBE HAVE THE QUEUE CONFIGURABLE
            BlockingQueue<Packet> channel = new ArrayBlockingQueue<>(100);
            // ??? DOES THIS CALCULATE THE CORRECT CID
//            Certificate[] serverCerts = sslSocket.getSession().getPeerCertificates();
//            String cid = SecurityUtilities.calculateFingerprint(serverCerts[0].getEncoded());
            // --------------------------- NEW AUTHENTICATION --------------------------------



            // -------------------------------------------------------------------------------
             Thread reciever = new Thread(new RecieverClientThread(sslSocket, channelMap, channel, cid));
             Thread sender = new Thread(new SenderClientThread(sslSocket, channelMap, channel , cid));

             if(!channelMap.get(cid).isPresent()) {
              reciever.start();
              sender.start();
                 try {
                     sender.join();
                 } catch (InterruptedException e) {
                     reciever.interrupt();
                     sender.interrupt();
                 }
             }

        } catch (GeneralSecurityException | IOException e) {
            // LOGGING HERE ON PRODUCTION
            e.printStackTrace();
        }

    }

}
