package networking;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkConfiguration {

    private AtomicBoolean networkUp;
    private final AtomicInteger PACKET_CAPACITY;
    private static NetworkConfiguration networkConfiguration = null;

    private NetworkConfiguration(){
        networkUp = new AtomicBoolean(true);
        PACKET_CAPACITY = new AtomicInteger(10000);
    }

    static NetworkConfiguration getNetworkConfiguration(){
        if(networkConfiguration == null)
            networkConfiguration = new NetworkConfiguration();
        return networkConfiguration;
    }

    static void clearNetworkConfiguration(){
        networkConfiguration = null;
    }

    public boolean getNetworkUp() {
        return networkUp.get();
    }

    public void setNetworkUp(boolean networkUp) {
        this.networkUp.set(networkUp);
    }

    public int getPacketCapacity() {
        return PACKET_CAPACITY.get();
    }




}
