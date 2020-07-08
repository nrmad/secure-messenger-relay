package networking;

import packets.*;
import packets.Packet.Type;
import datasource.Account;
import datasource.Contact;
import datasource.DatabaseUtilities;
import datasource.Network;
import security.SecurityUtilities;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class RequestHandlerThread implements Runnable {

    private SSLSocket sslSocket;
    private HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap;
    private Set<String> usernames;
    private int authIterations;
    private DatabaseUtilities databaseUtilities;
    private Pattern namePattern = Pattern.compile("\\w{1,255}");
    private Pattern passPattern = Pattern.compile("\\w");
    private static int nextRequestId = 1000000;

    public RequestHandlerThread(SSLSocket sslSocket, HashMap<Integer, ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap, Set<String> usernames, int authIterations) throws SQLException {
        this.sslSocket = sslSocket;
        this.networkMap = networkMap;
        this.usernames = usernames;
        this.authIterations = authIterations;
        databaseUtilities = DatabaseUtilities.getInstance();
    }

    public void run() {


        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(sslSocket.getOutputStream()));
             ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(sslSocket.getInputStream()))) {


            RequestJoinPacket requestJoinPacket;
            // RECEIVE DATA FROM THE CLIENT
            requestJoinPacket = (RequestJoinPacket) input.readObject();
            Optional<ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> channelMap = Optional.empty();
            int requestId = getNextRequestId(), nid;


            try {
                // VALIDATE THE DATA ELSE RESPOND FAIL

                BlockingQueue<Packet> sendChannel, receiveChannel;
                Packet result;
                nid = databaseUtilities.getNid(requestJoinPacket.getCid());

                // GET NETWORK FROM CID

                if (namePattern.matcher(requestJoinPacket.getAlias()).matches() &&
                        namePattern.matcher(requestJoinPacket.getUsername()).matches() &&
                        passPattern.matcher(requestJoinPacket.getPassword()).matches() &&
                        (channelMap = Optional.of(networkMap.get(nid))).get().containsKey(requestJoinPacket.getCid()) &&
                        channelMap.get().get(requestJoinPacket.getCid()).isPresent() &&
                        usernames.add(requestJoinPacket.getUsername())) {

                    sendChannel = channelMap.get().get(requestJoinPacket.getCid()).get();
                    // !!! MAKE configurable
                    receiveChannel = new ArrayBlockingQueue<>(100);


                } else {
                    throw new FailedJoinException("nid / cid invalid or user inactive");
                }

                // ADD THIS THREAD TO THE REQUESTED NETWORKCHANNEL AND MAKE REQUEST OF USER

                requestJoinPacket = new RequestJoinPacket(requestJoinPacket.getCid(), requestId, requestJoinPacket.getAlias());
                channelMap.get().put(requestId, Optional.of(receiveChannel));
                if (!sendChannel.offer(requestJoinPacket)) {
                    throw new FailedJoinException("user inactive or busy");
                }
                // IF USER DID NOT EXIST RESPOND FAIL / IF THEY DECLINE RESPOND FAIL
                // NEED TO FIND A TIMEOUT FOR THESE THREADS OR THEY'LL ACCUMULATE

                Callable<Packet> waitForResponse = receiveChannel::take;

                Callable<Packet> waitForCancel = () -> {
                    return (CancelOperationPacket) input.readObject();
                };

                List<Callable<Packet>> waitList = new ArrayList<>();
                waitList.add(waitForResponse);
                waitList.add(waitForCancel);
                ExecutorService executor = Executors.newFixedThreadPool(2);

                result = executor.invokeAny(waitList, 5, TimeUnit.MINUTES);
                executor.shutdownNow();

                // ON ACCEPT THEN ADD THEN CREATE HASH FOR PASSWORD AND ADD TO DATABASE

                if (result.getType() == Type.CANCEL_OPERATION || result.getType() == Type.REFUSE_JOIN) {
                    throw new FailedJoinException("join refused by chosen user or cancelled");
                } else if (result.getType() == Type.ACCEPT_JOIN) {
                    List<String> auth = SecurityUtilities.getAuthenticationHash(requestJoinPacket.getPassword(), authIterations);
                    Account account = new Account(requestJoinPacket.getUsername(), auth.get(0), auth.get(1),authIterations);
                    Contact contact = new Contact(requestJoinPacket.getAlias());
                    Network network = new Network(nid);
                    List<Contact> contacts = databaseUtilities.getNetworkContacts(network);
                    if(databaseUtilities.addUser(contact, network, account)){
                        account = databaseUtilities.getAccount(account);
                        contact = databaseUtilities.getContact(account);
                        channelMap.get().put(contact.getCid(), Optional.empty());
                        // SHOULD SEND AN UPDATE TO EVERYONE ON THE NETWORK THIS CONTACT IS ADDED
                        output.writeObject(new AcceptJoinPacket(contact, network, account, contacts));
                    }
                } else{
                    throw new FailedJoinException("unspecified failure");
                }

            } catch (FailedJoinException | IllegalStateException |
                    InterruptedException | ExecutionException |
                    GeneralSecurityException | TimeoutException | SQLException e) {
                channelMap.ifPresent(c -> c.remove(requestId));
                usernames.remove(requestJoinPacket.getUsername());
                output.writeObject(new RefuseJoinPacket());
            }
        } catch (IOException | ClassNotFoundException e) {
        }

    }

    private static synchronized int getNextRequestId() {
        return nextRequestId++;
    }
}

