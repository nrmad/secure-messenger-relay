package networking;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkThread implements Runnable {

    private SecureSocketManager secureSocketManager;
    private final int tlsPort;
    private final ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>> channelMap;
    private NetworkConfiguration networkConfiguration;



    public NetworkThread(SecureSocketManager secureSocketManager, ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>  channelMap, int tlsPort){
        this.secureSocketManager = secureSocketManager;
        this.tlsPort = tlsPort;
        this.channelMap = channelMap;
        networkConfiguration = NetworkConfiguration.getNetworkConfiguration();
//        contacts.forEach(c-> channelMap.put(c.getCid(), null));
    }

    public void run(){

        ExecutorService clientThreads = Executors.newCachedThreadPool();
        // SETUP A SECURESOCKETMANAGER INSTANCE AND ESTABLISH A SERVER SOCKET ON DESIRED PORT

        try {
            SSLServerSocket sslServerSocket = secureSocketManager.getSslServerSocket(tlsPort);

            // BEGIN RECEIVING NEW CONNECTION INSTANCES ON THAT PORT AND LOOP

            while(networkConfiguration.getNetworkUp()) {

            try{
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                clientThreads.submit(new ClientThread(sslSocket, channelMap));

            }catch(IOException e){
                e.printStackTrace();
            }

            }

        }catch (IOException e){
                e.printStackTrace();
        }


    }

}
