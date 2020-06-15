package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class SenderClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>   channelMap;
    private final BlockingQueue<Packet> channel;


    public SenderClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap , BlockingQueue<Packet> channel) {
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.channel = channel;
    }

    public void run(){

        boolean quit = false;
        channelMap.replace(cid, Optional.of(channel));

        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {
            // RECEIVE CID OR FAIL FROM RECEIVER AND UPDATE CHANNELMAP
            while (!quit) {
                    Packet packet = channel.take();
                    switch(packet.getType()){
                        case ACK:
                        case MESSAGE:
                        case REQUEST_USER:
                        case RELAY_SHUTDOWN:
                            output.writeObject(packet);
                            break;
                        case END_SESSION:
                            if(packet.getSource() == cid) {
                                output.writeObject(packet);
                                quit = true;
                            }
                            break;
                    }
            }
        }catch (IOException | InterruptedException e){}
        finally {
            channelMap.replace(cid, Optional.empty());
//            countDownLatch.countDown();
        }


    }
}
