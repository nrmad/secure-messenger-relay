package networking;

import packets.Packet;
import packets.ShutdownPacket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static java.lang.Thread.interrupted;

public class NetworkThread implements Runnable {

    private final SSLServerSocket sslServerSocket;
    private final HashMap<Integer,ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap;
    private final Set<String> usernames;
    private final int authIterations;


    public NetworkThread( SSLServerSocket sslServerSocket, Set<String> usernames, HashMap<Integer,ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, int authIterations) {
        this.sslServerSocket = sslServerSocket;
        this.usernames = usernames;
        this.networkMap = networkMap;
        this.authIterations = authIterations;
    }

    public void run() {

        ExecutorService clientThreads = Executors.newCachedThreadPool();

        try {
            // BEGIN RECEIVING NEW CONNECTION INSTANCES ON THAT PORT AND LOOP

            while (!interrupted()) {

                try {
                    SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                    clientThreads.execute(new ClientThread(sslSocket, usernames, networkMap, authIterations));

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

        } catch ( InterruptedException e) {
            clientThreads.shutdownNow();
        }
    }
}
