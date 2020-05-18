package networking;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.interrupted;

public class SenderClientThread implements Runnable{

    private final SSLSocket sslSocket;
    private final Pipe.SourceChannel source;
    private final ByteBuffer buffer;
    private NetworkConfiguration networkConfiguration;
    private CountDownLatch countDownLatch;


    public SenderClientThread(SSLSocket sslSocket, Pipe.SourceChannel source, CountDownLatch countDownLatch) {
        this.sslSocket = sslSocket;
        this.source = source;
        this.countDownLatch = countDownLatch;
        // SEE IF FURTHER USE OF NETWORK CONFIG ELSE MAKE SINGLE USE
        this.networkConfiguration = NetworkConfiguration.getNetworkConfiguration();
        buffer = ByteBuffer.allocateDirect(networkConfiguration.getPacketCapacity());
    }

    public void run(){

        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()))) {
            while (!interrupted()) {


            }
        }catch (IOException e){}
        finally {
            countDownLatch.countDown();
        }


    }
}
