package networking;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkThread implements Runnable {

    private SecureSocketManager secureSocketManager;
    private final int tlsPort;
    private final ConcurrentHashMap<String, Pipe.SinkChannel> channelMap;


    public NetworkThread(SecureSocketManager secureSocketManager, ConcurrentHashMap<String, Pipe.SinkChannel> channelMap, int tlsPort){
        this.secureSocketManager = secureSocketManager;
        this.tlsPort = tlsPort;
        this.channelMap = channelMap;
//        contacts.forEach(c-> channelMap.put(c.getCid(), null));
    }

    public void run(){

        ExecutorService clientThreads = Executors.newCachedThreadPool();
        // SETUP A SECURESOCKETMANAGER INSTANCE AND ESTABLISH A SERVER SOCKET ON DESIRED PORT

        try {
            SSLServerSocket sslServerSocket = secureSocketManager.getSslServerSocket(tlsPort);
            Pipe pipe;

            // BEGIN RECEIVING NEW CONNECTION INSTANCES ON THAT PORT AND LOOP

            // !!! WILL BE BASED ON CENTRAL TBOARD
            while(true) {

            try{
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                pipe = Pipe.open();
                clientThreads.submit(new RecieverClientThread(sslSocket, channelMap, pipe));
                clientThreads.submit(new SenderClientThread(sslSocket, pipe));

            }catch(IOException e){
                e.printStackTrace();
            }

            }

        }catch (IOException e){
                e.printStackTrace();
        }


    }

}
