package orchestrator;

import datasource.Contact;
import datasource.DatabaseUtilities;
import datasource.Network;
import datasource.ReadPropertiesFile;
import networking.NetworkThread;
import networking.Packet;
import networking.SecureSocketManager;
import security.SecurityUtilities;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
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

                SecureSocketManager secureSocketManager = new SecureSocketManager(keystore, credentials[1]);
//                ConcurrentHashMap<Integer, Optional<BlockingQueue<Packet>>> channelMap;

                for(Network network : networks) {

                    // LOAD THE CHANNELMAP WITH EVERY CONTACT AND AN EMPTY OPTIONAL

                    List<Contact> contacts = databaseUtilities.getNetworkContacts(network);
                    ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap = contacts.stream()
                    .collect(Collectors.toConcurrentMap(Contact::getCid, s -> Optional.empty()));

                    // SAVE THE CHANNELMAP TO NETWORK MAP WITH ITS NID
                    networkMap.put(network.getNid(), channelMap);

                }

            // START NEW NETWORKTHREAD ??? NEED TO KNOW ABOUT LINUX SERVICES SHOULD THIS OBJECT BE RETAINED

            threadManager.execute(new NetworkThread(secureSocketManager, networkMap,networks.get(0).getPort().getTLSPort(),
                    propertiesFile.getAuthIterations()));


            // INIT REQUEST THREAD



            // ADD SIGTERM HOOK

                Runtime.getRuntime().addShutdownHook(new ShutdownHook(threadManager));

            databaseUtilities.closeConnection();

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
