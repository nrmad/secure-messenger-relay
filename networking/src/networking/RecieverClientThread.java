package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class RecieverClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap;
    private final BlockingQueue<Packet> channel;
    private final int cid;

    public RecieverClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>  channelMap, BlockingQueue<Packet> channel, int cid){
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.channel = channel;
        this.cid = cid;
    }

    public void run(){

//        ArrayBlockingQueue<Packet> channel;
        boolean quit = false;
        Optional<BlockingQueue<Packet>> destChannel;

        try (ObjectInputStream input = new ObjectInputStream( new BufferedInputStream(sslSocket.getInputStream()))) {
            while (!quit) {
                try {
                    Packet packet = (Packet) input.readObject();
                    if(!(packet.getSource() == cid))
                        continue;
                    switch(packet.getType()){
                        case MESSAGE:
                        case ACCEPT_USER:
                            if(channelMap.containsKey(packet.getDestination()) && (destChannel = channelMap.get(packet.getDestination())).isPresent())
                                destChannel.get().put(packet);
                            break;
                        case END_SESSION:
                            quit = true;
                            channel.put(packet);
                            break;

                    }
                }catch (ClassNotFoundException e){}
            }
        }catch (IOException | InterruptedException e){}
//        finally {
//            countDownLatch.countDown();
//        }


        // SHOULD PROBABLY DEC COUNTDOWN HERE
    }
}
