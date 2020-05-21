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
import java.util.concurrent.CountDownLatch;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>  channelMap;

    public ClientThread(SSLSocket sslSocket, ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>  channelMap){
    this.sslSocket = sslSocket;
    this.channelMap = channelMap;
    }

    public void run() {
        try {
            // ??? MAYBE HAVE THE QUEUE CONFIGURABLE
            BlockingQueue<Packet> channel = new ArrayBlockingQueue<Packet>(100);
            // ??? DOES THIS CALCULATE THE CORRECT CID
            Certificate[] serverCerts = sslSocket.getSession().getPeerCertificates();
            String cid = SecurityUtilities.calculateFingerprint(serverCerts[0].getEncoded());
            // SHOULD PROBABLY ASSURE THIS IS ESTABLISHED BEFORE LISTING IT
//            channelMap.put(cid, pipe.sink());

            // !!! SHOULD PROBABLY USE JUST A LOOP ON BEING INTERRUPTED IF THREADS CAN'T INDIVIDUALLY FAIL
            CountDownLatch countDownLatch = new CountDownLatch(1);

             Thread reciever = new Thread(new RecieverClientThread(sslSocket, channelMap, channel, cid, countDownLatch));
             Thread sender = new Thread(new SenderClientThread(sslSocket, channelMap, channel , cid, countDownLatch));

             try {
                 while (countDownLatch.getCount() > 0)
                     countDownLatch.await();
             }catch (InterruptedException e){}

            reciever.interrupt();
            sender.interrupt();

        } catch (GeneralSecurityException | IOException e) {
            // LOGGING HERE ON PRODUCTION
            e.printStackTrace();
        }

    }

}
