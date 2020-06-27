package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.interrupted;
import static networking.Packet.Type.AUTH_SUCCESS;

public class SenderClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap;
    private final BlockingQueue<Packet> channel;


    public SenderClientThread(SSLSocket sslSocket, HashMap<Integer, ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap
            , BlockingQueue<Packet> channel) {
        this.sslSocket = sslSocket;
        this.networkMap = networkMap;
        this.channel = channel;
    }

    public void run() {

        boolean quit = false;
        Packet packet;
        int cid, nid;
        ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;

        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {

                packet = channel.take();
                if (packet.getType() == AUTH_SUCCESS) {
                    cid = ((AuthSuccessPacket) packet).getCid();
                    nid = ((AuthSuccessPacket) packet).getNid();
                    channelMap = networkMap.get(nid);
                    channelMap.replace(cid, Optional.of(channel));
                    output.writeObject(packet);
                } else {
                    output.writeObject(packet);
                    return;
                }

            while (!quit && !interrupted()) {
                try {
                    packet = channel.take();
                    switch (packet.getType()) {
                        case ACK:
                        case MESSAGE:
                        case REQUEST_USER:
                        case RELAY_SHUTDOWN:
                            output.writeObject(packet);
                            break;
                        case END_SESSION:
                            if (packet.getSource() == cid) {
                                output.writeObject(packet);
                                channelMap.replace(cid, Optional.empty());
                                quit = true;
                            }
                            break;
                    }
                } catch (InterruptedException e) {
                    channelMap.replace(cid, Optional.empty());
                    return;
                } catch (IOException e) {
                }
            }
        } catch (IOException | InterruptedException e) {
        }
    }
}
