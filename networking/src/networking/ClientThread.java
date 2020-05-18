package networking;

import security.SecurityUtilities;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<String, Pipe.SinkChannel> channelMap;

    public ClientThread(SSLSocket sslSocket, ConcurrentHashMap<String, Pipe.SinkChannel> channelMap){
    this.sslSocket = sslSocket;
    this.channelMap = channelMap;
    }

    public void run() {
        try {
            Pipe pipe = Pipe.open();
            // ??? DOES THIS CALCULATE THE CORRECT CID
            Certificate[] serverCerts = sslSocket.getSession().getPeerCertificates();
            String cid = SecurityUtilities.calculateFingerprint(serverCerts[0].getEncoded());
            CountDownLatch countDownLatch = new CountDownLatch(1);

             Thread reciever = new Thread(new RecieverClientThread(sslSocket, channelMap, pipe.sink(), cid, countDownLatch));
             Thread sender = new Thread(new SenderClientThread(sslSocket, pipe.source(), countDownLatch));

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
