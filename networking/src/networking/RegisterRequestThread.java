package networking;

import java.net.Socket;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterRequestThread implements Runnable {

    private static int currentId = 0;
    private Socket socket;
    private  HashMap<Integer,ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>> networkMap;

    public RegisterRequestThread(Socket socket,  HashMap<Integer, ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>> networkMap){
        this.socket = socket;
        this.networkMap = networkMap;
    }

    public void run(){
        int id = getRequestId();


    }

    private static synchronized int getRequestId(){
        return currentId++;
    }
}
