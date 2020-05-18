package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.interrupted;

public class RecieverClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<String, Pipe.SinkChannel> channelMap;
    private final String cid;
    private CountDownLatch countDownLatch;
    private NetworkConfiguration networkConfiguration;
    private final ByteBuffer buffer;
    // MAYBE FOR SOME CONFIG FILE

    public RecieverClientThread(SSLSocket sslSocket, ConcurrentHashMap<String, Pipe.SinkChannel> channelMap, Pipe.SinkChannel sinkChannel, String cid, CountDownLatch countDownLatch){
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.cid = cid;
        this.countDownLatch = countDownLatch;
        // SEE IF FURTHER USE OF NETWORK CONFIG ELSE MAKE SINGLE USE
        this.networkConfiguration = NetworkConfiguration.getNetworkConfiguration();
        this.buffer = ByteBuffer.allocateDirect(networkConfiguration.getPacketCapacity());
        channelMap.put(cid, sinkChannel);

//        buffer.put(cid.getBytes(StandardCharsets.UTF_8));
//        sink.write(buffer);

    }

    public void run(){

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(sslSocket.getInputStream()))) {
            while (!interrupted()) {


            }
        }catch (IOException e){}
        finally {
            countDownLatch.countDown();
        }


        // SHOULD PROBABLY DEC COUNTDOWN HERE
    }
}
