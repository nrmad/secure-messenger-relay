package networking;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.*;

import static java.lang.Thread.interrupted;

public class NetworkThread implements Runnable {

    private SecureSocketManager secureSocketManager;
    private final int tlsPort;
    private final HashMap<Integer,ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap ;
    private final int authIterations;


    public NetworkThread(SecureSocketManager secureSocketManager, HashMap<Integer,ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, int tlsPort, int authIterations) {
        this.secureSocketManager = secureSocketManager;
        this.tlsPort = tlsPort;
        this.networkMap = networkMap;
        this.authIterations = authIterations;
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
                    clientThreads.execute(new ClientThread(sslSocket, networkMap, authIterations));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        clientThreads.shutdown();

            // There should be no second interrupt so having this unchecked lambda wrapper is acceptable
           networkMap.values()
                   .stream()
                   .map(ConcurrentMap::values)
                   .flatMap(Collection::stream)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .forEach(q -> { try {
                       q.put(new ShutdownPacket());
                   }catch (InterruptedException e){}
                   });

           if(!clientThreads.awaitTermination(3, TimeUnit.MINUTES))
               clientThreads.shutdownNow();

        } catch (IOException | InterruptedException e) {
            clientThreads.shutdownNow();
        }
    }
}
