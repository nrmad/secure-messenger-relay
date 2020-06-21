package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.interrupted;

public class SenderClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>   channelMap;
    private final BlockingQueue<Packet> channel;


    public SenderClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>
            channelMap, BlockingQueue<Packet> channel) {
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.channel = channel;
    }

    public void run(){

        boolean quit = false;
        Packet packet;
        int cid = 0;

        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {
          packet = channel.take();
          if(packet.getType() == Type.AUTH_SUCCESS){
              cid = packet.getSource();
              channelMap.replace(cid, Optional.of(channel));
              output.writeObject(packet);
          } else{
              output.writeObject(packet);
              return;
          }

            while (!quit && !interrupted()) {
                    packet = channel.take();
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
                                channelMap.replace(cid, Optional.empty());
                                quit = true;
                            }
                            break;
                    }
            }
        }catch (IOException | InterruptedException e){
            channelMap.replace(cid, Optional.empty());
        }
    }
}
