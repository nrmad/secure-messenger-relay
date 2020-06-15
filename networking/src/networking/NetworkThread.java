package networking;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;

import static java.lang.Thread.interrupted;

public class NetworkThread implements Runnable {

    private SecureSocketManager secureSocketManager;
    private final int nid;
    private final int tlsPort;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;


    public NetworkThread(SecureSocketManager secureSocketManager, int nid, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>> channelMap, int tlsPort) {
        this.secureSocketManager = secureSocketManager;
        this.nid = nid;
        this.tlsPort = tlsPort;
        this.channelMap = channelMap;
    }

    public void run() {

        ExecutorService clientThreads = Executors.newCachedThreadPool();
        // SETUP A SECURESOCKETMANAGER INSTANCE AND ESTABLISH A SERVER SOCKET ON DESIRED PORT

        try {
            SSLServerSocket sslServerSocket = secureSocketManager.getSslServerSocket(tlsPort);

            // BEGIN RECEIVING NEW CONNECTION INSTANCES ON THAT PORT AND LOOP

            while (!interrupted()) {

                try {
                    SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                    clientThreads.execute(new ClientThread(sslSocket, channelMap, nid));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        clientThreads.shutdown();

            // There should be no second interrupt so having this unchecked lambda wrapper is acceptable
           channelMap.entrySet()
                   .stream()
                   .filter(e -> e.getValue().isPresent())
                   .forEach(ThrowingConsumer.unchecked(e -> e.getValue().get().put(Packet.getShutdownPacket(e.getKey(), nid))));
           if(!clientThreads.awaitTermination(3, TimeUnit.MINUTES))
               clientThreads.shutdownNow();

        } catch (IOException | InterruptedException e) {
            clientThreads.shutdownNow();
        }


    }

}
