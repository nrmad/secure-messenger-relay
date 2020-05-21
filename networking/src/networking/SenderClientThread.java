package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.interrupted;

public class SenderClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>   channelMap;
    private final BlockingQueue<Packet> channel;
    private final String cid;
    private final CountDownLatch countDownLatch;


    public SenderClientThread(SSLSocket sslSocket, ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>  channelMap , BlockingQueue<Packet> channel, String cid, CountDownLatch countDownLatch) {
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.channel = channel;
        this.cid = cid;
        this.countDownLatch = countDownLatch;
    }

    public void run(){

        boolean quit = false;
        channelMap.replace(cid, Optional.of(channel));

        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {
            while (!interrupted() && !quit) {
                try {
                    Packet packet = channel.take();
                    switch(packet.getType()){
                        case MESSAGE:
                        case SEND_FAILED:
                        case REQUEST_USER:
                            output.writeObject(packet);
                            break;
                        case END_SESSION:
                            if(packet.getSource().equals(cid)) {
                                output.writeObject(packet);
                                quit = true;
                            }
                    }
                }catch (InterruptedException e){}

            }
        }catch (IOException e){}
        finally {
            channelMap.replace(cid, Optional.empty());
            countDownLatch.countDown();
        }


    }
}
