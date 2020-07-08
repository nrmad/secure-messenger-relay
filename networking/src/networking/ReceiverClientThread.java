package networking;

import datasource.Account;
import datasource.Contact;
import datasource.DatabaseUtilities;
import packets.AuthFailedPacket;
import packets.AuthSuccessPacket;
import packets.AuthenticationPacket;
import packets.Packet;
import security.SecurityUtilities;

import javax.net.ssl.SSLSocket;
import javax.security.auth.login.AccountNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Phaser;

import static java.lang.Thread.interrupted;


public class ReceiverClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap ;
    private final BlockingQueue<Packet> channel;
    private final int authIterations;
    private final Set<String> usernames;
    private final Phaser conPhaser;
    private DatabaseUtilities databaseUtilities;

    public ReceiverClientThread(SSLSocket sslSocket, HashMap<Integer, ConcurrentMap<Integer,
            Optional<BlockingQueue<Packet>>>> networkMap
            , BlockingQueue<Packet> channel, int authIterations, Set<String> usernames, Phaser conPhaser)
            throws SQLException {
        this.sslSocket = sslSocket;
        this.networkMap = networkMap;
        this.channel = channel;
        this.authIterations = authIterations;
        this.usernames = usernames;
        this.conPhaser = conPhaser;
        databaseUtilities = DatabaseUtilities.getInstance();
//        conPhaser.register();
    }

    public void run() {
        boolean quit = false;
        Optional<BlockingQueue<Packet>> destChannel;
        ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;
        int cid, nid, aid;
        Packet packet;

        try {
            conPhaser.arriveAndAwaitAdvance();
            conPhaser.arriveAndDeregister();
            try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(sslSocket.getInputStream()))) {

                AuthenticationPacket authenticationPacket = (AuthenticationPacket) input.readObject();
                Account account = authenticate(authenticationPacket.getUsername(), authenticationPacket.getPassword());
                Contact contact = databaseUtilities.getContact(account);
                cid = contact.getCid();
                nid = databaseUtilities.getAccountNetwork(account).getNid();
                aid = account.getAid();
                channelMap = networkMap.get(nid);
                channel.put(new AuthSuccessPacket(cid, nid, aid));

                while (!quit && !interrupted()) {
                    try {
                        packet = (Packet) input.readObject();
                        if (!(packet.getSource() == cid))
                            continue;
                        switch (packet.getType()) {
                            case MESSAGE:
                            case ACCEPT_JOIN:
                                if (channelMap.containsKey(packet.getDestination()) && (destChannel = channelMap
                                        .get(packet.getDestination())).isPresent())
                                    destChannel.get().offer(packet);
                                break;
                            case END_SESSION:
                                quit = true;
                                channel.put(packet);
                                break;
                            case DELETE_ACCOUNT:
                                    channelMap.replace(cid, Optional.empty());
                                    databaseUtilities.deleteUser(contact);
                                    usernames.remove(account.getUsername());
                                    quit = true;
                                    channel.put(packet);
                                break;

                        }
                    } catch (ClassNotFoundException | IOException e) {
                    }
                }
            } catch (IOException | SQLException | GeneralSecurityException | ClassNotFoundException e) {
                // LOG FAILED LOGIN
                channel.put(new AuthFailedPacket());
            }
        }catch (InterruptedException e){}
    }

    private Account authenticate(String username, String password)
            throws SQLException, GeneralSecurityException {
        Account account = new Account(username);
        account = databaseUtilities.getAccount(account);
        String hashPassword = SecurityUtilities.getAuthenticationHash(password, account.getSalt(),
                account.getIterations());
        if (account.getPassword().equals(hashPassword)) {
            if (account.getIterations() != authIterations) {
                List<String> updatedCredentials = SecurityUtilities.getAuthenticationHash(password, authIterations);
                account = new Account(account.getAid(), account.getUsername(), updatedCredentials.get(0),
                        updatedCredentials.get(1),
                        authIterations);
                databaseUtilities.updateAccountCredentials(account);
            }
            return account;
        } else
            throw new AccountNotFoundException();
        }
}
