package orchestrator;

import datasource.*;
import networking.NetworkThread;
import packets.Packet;
import networking.RequestThread;
import networking.SecureSocketManager;
import security.SecurityUtilities;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        List<Network> networks;
        DatabaseUtilities databaseUtilities;

        try{
                String[] credentials = getCredentials();
                ReadPropertiesFile propertiesFile = ReadPropertiesFile.getInstance();
                DatabaseUtilities.setDatabaseUtilities(credentials[0], credentials[1]);
                databaseUtilities = DatabaseUtilities.getInstance();

                // INIT NETWORK THREADS

                KeyStore keystore = SecurityUtilities.loadKeystore(credentials[1]);
                networks = databaseUtilities.getAllNetworks();
                Network registration = networks.remove(0);

                HashMap<Integer,ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap = new HashMap<>();
                ExecutorService threadManager = Executors.newFixedThreadPool(2);

                Set<String> usernames = databaseUtilities.getUsernames();
                SecureSocketManager secureSocketManager = new SecureSocketManager(keystore, credentials[1]);

                for(Network network : networks) {

                    // LOAD THE CHANNELMAP WITH EVERY CONTACT AND AN EMPTY OPTIONAL

                    List<Contact> contacts = databaseUtilities.getNetworkContacts(network);
                    ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap = contacts.stream()
                    .collect(Collectors.toConcurrentMap(Contact::getCid, s -> Optional.empty()));

                    // SAVE THE CHANNELMAP TO NETWORK MAP WITH ITS NID
                    networkMap.put(network.getNid(), channelMap);

                }

            SSLServerSocket sslServerSocket = secureSocketManager.getSslServerSocket(networks.get(0).getPort().getTLSPort());
            threadManager.execute(new NetworkThread(sslServerSocket, usernames, networkMap,propertiesFile.getAuthIterations()));

            // INIT REQUEST THREAD

            SSLServerSocket sslRegServerSocket = secureSocketManager.getSslServerSocket(registration.getPort().getTLSPort());
            threadManager.execute(new RequestThread(sslRegServerSocket, networkMap, usernames, propertiesFile.getAuthIterations()));


            // ADD SIGTERM HOOK

                Runtime.getRuntime().addShutdownHook(new ShutdownHook(threadManager, sslServerSocket, sslRegServerSocket));

//            databaseUtilities.closeConnection();

        } catch (SQLException | GeneralSecurityException | IOException e) {
            // WILL HANDLE FAILED DELETE
            System.out.println("stuff");
        }

    }

    private static String[] getCredentials() {

        //Console con = System.console();
        Scanner scanner = new Scanner(System.in);
        String[] credentials = new String[2];

        System.out.print("Enter username: ");
        credentials[0] = scanner.nextLine();
        System.out.print("Enter password: ");
        credentials[1] = scanner.nextLine();
//        password = new String(con.readPassword("Enter password: "));
        return credentials;
    }
}
