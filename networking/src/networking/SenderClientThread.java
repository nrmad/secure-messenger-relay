package networking;

import packets.AuthSuccessPacket;
import packets.Packet;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Phaser;

import static java.lang.Thread.interrupted;
import static packets.Packet.Type.AUTH_SUCCESS;

public class SenderClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap;
    private final BlockingQueue<Packet> channel;
    private final Phaser conPhaser;

    public SenderClientThread(SSLSocket sslSocket, HashMap<Integer, ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap
            , BlockingQueue<Packet> channel, Phaser conPhaser) {
        this.sslSocket = sslSocket;
        this.networkMap = networkMap;
        this.channel = channel;
        this.conPhaser = conPhaser;
//        conPhaser.register();
    }

    public void run() {

        boolean quit = false;
        Packet packet;
        int cid, nid;
        ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;

        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {
                conPhaser.arriveAndDeregister();
                packet = channel.take();
                if (packet.getType() == AUTH_SUCCESS) {
                    cid = ((AuthSuccessPacket) packet).getCid();
                    nid = ((AuthSuccessPacket) packet).getNid();
                    channelMap = networkMap.get(nid);
                    channelMap.replace(cid, Optional.of(channel));
                    // AUTH SUCCESS PACKET SHOULD CONTAIN CONTACT LIST
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
                        case REQUEST_JOIN:
                        case RELAY_SHUTDOWN:
                            output.writeObject(packet);
                            break;
                        case DELETE_ACCOUNT:
                        case END_SESSION:
                                quit = true;
                                output.writeObject(packet);
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
