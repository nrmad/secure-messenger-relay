package orchestrator;

import datasource.Contact;
import datasource.DatabaseUtilities;
import datasource.Network;
import networking.NetworkThread;
import networking.Packet;
import networking.SecureSocketManager;
import security.SecurityUtilities;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        boolean success = false;
        List<Network> networks = new ArrayList<>();
        DatabaseUtilities databaseUtilities;

        try{
                String[] credentials = getCredentials();
                DatabaseUtilities.setDatabaseUtilities(credentials[0], credentials[1]);
                databaseUtilities = DatabaseUtilities.getInstance();

//                List<Thread> threads = new ArrayList<>();
                // INIT NETWORK THREADS

                KeyStore keystore = SecurityUtilities.loadKeystore(credentials[1]);
                KeyStore truststore = SecurityUtilities.loadTruststore(credentials[1]);

                networks = databaseUtilities.getAllNetworks();

                // CHECK THE NETWORK LIST IS AT LEAST TWO IN LENGTH OR END

                HashMap<Integer,ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>>> networkMap = new HashMap<>();
                ExecutorService threadManager = Executors.newFixedThreadPool(networks.size());

                for (Network network: networks) {

                    KeyStore singleKeystore = SecurityUtilities.loadSingleKeystore(keystore, credentials[1], network.getFingerprint());
                    KeyStore singleTruststore = SecurityUtilities.loadSingleTruststore(truststore, network.getFingerprint());
                    SecureSocketManager secureSocketManager = new SecureSocketManager(singleKeystore, singleTruststore, credentials[1]);
                    ConcurrentHashMap<String, Optional<BlockingQueue<Packet>>> channelMap = new ConcurrentHashMap<>();

                    // LOAD THE CHANNELMAP WITH EVERY CONTACT AND AN EMPTY OPTIONAL

                    List<Contact> contacts = databaseUtilities.getNetworkContacts(network);
                    contacts.forEach(c -> channelMap.put(c.getCid(), Optional.empty()));

                    // SAVE THE CHANNELMAP TO NETWORK MAP WITH ITS NID

                    networkMap.put(network.getNid(), channelMap);

                    // START NEW NETWORKTHREAD ??? NEED TO KNOW ABOUT LINUX SERVICES SHOULD THIS OBJECT BE RETAINED

                    threadManager.execute(new NetworkThread(secureSocketManager, network.getFingerprint(), channelMap, network.getPort()));

                }


                // INIT REQUEST THREAD


                // ADD SIGTERM HOOK

                Runtime.getRuntime().addShutdownHook(new ShutdownHook(threadManager));

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
