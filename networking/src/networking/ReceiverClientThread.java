package networking;

import datasource.Account;
import datasource.Contact;
import datasource.DatabaseUtilities;
import datasource.Network;
import security.SecurityUtilities;

import javax.net.ssl.SSLSocket;
import javax.security.auth.login.AccountNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.interrupted;


public class ReceiverClientThread implements Runnable {

    private final SSLSocket sslSocket;
    private final ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;
    private final BlockingQueue<Packet> channel;
    private final int nid;
    private final int authIterations;
    private DatabaseUtilities databaseUtilities;

    public ReceiverClientThread(SSLSocket sslSocket, ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>>
            channelMap, BlockingQueue<Packet> channel, int nid, int authIterations) throws SQLException {
        this.sslSocket = sslSocket;
        this.channelMap = channelMap;
        this.channel = channel;
        this.nid = nid;
        this.authIterations = authIterations;
        databaseUtilities = DatabaseUtilities.getInstance();
    }

    public void run() {
        boolean quit = false;
        Optional<BlockingQueue<Packet>> destChannel;
        int cid;
        Packet packet;
        try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(sslSocket.getInputStream()))) {
            try {
                packet = (Packet) input.readObject();
                if (!(packet.getType() == Type.AUTHENTICATE))
                    throw new AccountNotFoundException();
                String[] credentials = packet.getData().split(":");
                cid = authenticate(credentials[0], credentials[1]).getCid();
                channel.put(Packet.getAuthSuccessPacket(cid));
            } catch (SQLException | GeneralSecurityException | ClassNotFoundException e) {
                // LOG FAILED LOGIN
                channel.put(Packet.getAuthFailedPacket());
                return;
            }

            while (!quit && !interrupted()) {
                try {
                    packet = (Packet) input.readObject();
                    if (!(packet.getSource() == cid))
                        continue;
                    switch (packet.getType()) {
                        case MESSAGE:
                        case ACCEPT_USER:
                            if (channelMap.containsKey(packet.getDestination()) && (destChannel = channelMap
                                    .get(packet.getDestination())).isPresent())
                                destChannel.get().put(packet);
                            break;
                        case END_SESSION:
                            quit = true;
                            channel.put(packet);
                            break;
                    }
                } catch (ClassNotFoundException e) {}
            }
        } catch (IOException | InterruptedException e) {}
    }

    private Contact authenticate(String username, String password)
            throws SQLException, GeneralSecurityException {
        Account account = new Account(username);
        Network network = new Network(nid);
        account = databaseUtilities.getAccount(account, network);
        String hashPassword = SecurityUtilities.getAuthenticationHash(password, account.getSalt(), account.getIterations());
        if (account.getPassword().equals(hashPassword)) {
            if (account.getIterations() != authIterations) {
                List<String> updatedCredentials = SecurityUtilities.getAuthenticationHash(password, authIterations);
                account = new Account(account.getAid(), account.getUsername(), updatedCredentials.get(0),
                        updatedCredentials.get(1),
                        authIterations);
                databaseUtilities.updateAccountCredentials(account);
            }
            return databaseUtilities.getContact(account);
        } else
            throw new AccountNotFoundException();
    }
}
