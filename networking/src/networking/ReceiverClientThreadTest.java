package networking;

import datasource.Account;
import datasource.Contact;
import datasource.DatabaseUtilities;
import datasource.Network;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import packets.EndSessionPacket;
import packets.Packet;
import security.SecurityUtilities;

import javax.net.ssl.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ReceiverClientThreadTest {

    // ADD REG AND A SINGLE NETWORK
    // ESTABLISH A TLS CONNECTION BETWEEN TWO POINTS WITH
    private final static String KEY_MANAGER = "SunX509";
    private final static String TLS_VERSION = "TLSv1.2";
    private final static String RNG_ALGORITHM = "DEFAULT";
    private final static String RNG_PROVIDER = "BC";
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/?useSSL=false";
    private static Connection conn;
    private static final String PROVIDER = "BC";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static KeyStore keyStore1, keyStore2, trustStore2;
    private static DatabaseUtilities databaseUtilities;
    private static SecureSocketManager secureSocketManager;
    private SSLSocket serverSocket;
    private SSLSocket clientSocket;

    @BeforeClass
    public static void setUp() throws SQLException, GeneralSecurityException, OperatorCreationException, IOException {

        String username = "relay-app", password = "relaypass";
        conn = DriverManager.getConnection(CONNECTION_STRING, "relay-app", "relaypass");
        Statement statement = conn.createStatement();
        statement.execute("USE secure_messenger_relay");
        statement.execute("INSERT IGNORE INTO networks(nid, network_alias) VALUES(1, 'REGISTRATION')," +
                "(2, 'james')");
        statement.execute("INSERT IGNORE INTO ports(pid, port) VALUES(1, 2048)," +
                " (2, 2049)");
        statement.execute("INSERT IGNORE INTO networkPorts(nid, pid) VALUES(1,1)," +
                " (2,2)");

        DatabaseUtilities.setDatabaseUtilities(username, password);
        databaseUtilities = DatabaseUtilities.getInstance();


        String name1 = "localhost", name2 = "client";
        KeyPair kp1 = SecurityUtilities.generateKeyPair();
        KeyPair kp2 = SecurityUtilities.generateKeyPair();
        X509Certificate cert1 = SecurityUtilities.makeV1Certificate(kp1.getPrivate(), kp1.getPublic(), name1);
        X509Certificate cert2 = SecurityUtilities.makeV1Certificate(kp2.getPrivate(), kp2.getPublic(), name2);

        keyStore1 = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        keyStore2 = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        trustStore2 = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);


        keyStore1.load(null, null);
        keyStore1.setKeyEntry(name1, kp1.getPrivate(), null, new X509Certificate[]{cert1});

        keyStore2.load(null, null);
        keyStore2.setKeyEntry(name2, kp2.getPrivate(), null, new X509Certificate[]{cert2});

        trustStore2.load(null, null);
        trustStore2.setCertificateEntry(name2, cert1);

//        secureSocketManager = new SecureSocketManager(keyStore1, password);
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        Statement statement = conn.createStatement();
        statement.execute("DELETE FROM networks");
        statement.execute("DELETE FROM ports");
    }


    @Before
    public void init() throws IOException, GeneralSecurityException, InterruptedException, ExecutionException {
        SSLServerSocket sslServerSocket = getSSLServerSocket();
        SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<SSLSocket> c1 = () -> {
            return (SSLSocket) sslServerSocket.accept();
        };

        Callable<SSLSocket> c2 = () -> {
            return (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName("localhost"), 2048);
        };

        Future<SSLSocket> server = pool.submit(c1);
        Thread.sleep(1000);
        Future<SSLSocket> client = pool.submit(c2);
        Thread.sleep(1000);
        serverSocket = server.get();
        clientSocket = client.get();
    }

    @After
    public void tearDown(){
        serverSocket = null;
        clientSocket = null;
    }

    @org.junit.Test
    public void endSession(){

        try {
            HashMap<Integer, ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>>> networkMap = new HashMap<>();
            ConcurrentMap<Integer, Optional<BlockingQueue<Packet>>> channelMap = new ConcurrentHashMap<>();
            int authIterations = 12000;
            Network network = new Network(2);
            List<String> auth = SecurityUtilities.getAuthenticationHash("pass", authIterations);
            Account account = new Account("nrmad", auth.get(0), auth.get(1),  authIterations);
            Contact contact = new Contact("benny");
            databaseUtilities.addUser(contact, network, account);
            contact = databaseUtilities.getContact(account);
//            List<Contact> contacts = databaseUtilities.getNetworkContacts(network);
//            for (Contact contact1: contacts) {
//
//                channelMap.put(contact1.getCid(), Optional.of(new ArrayBlockingQueue<>(100)));
//            }
            networkMap.put(2, channelMap);
            BlockingQueue<Packet> rec = new ArrayBlockingQueue<>(100);

            Thread test = new Thread(new ReceiverClientThread(serverSocket, networkMap,rec, authIterations,
                    ConcurrentHashMap.newKeySet()));

            test.start();
            try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()))) {
                Packet packet = new EndSessionPacket(contact.getCid(),contact.getCid());
                assertTrue(test.isAlive());
                output.writeObject(packet);
                assertEquals(rec.take(), packet);
                Thread.sleep(1000);
                assertFalse(test.isAlive());
            }catch (IOException e){
                fail();
            }
            databaseUtilities.deleteUser(contact);

        }catch (GeneralSecurityException | SQLException | InterruptedException e){
            fail();
        }
    }


    private SSLServerSocket getSSLServerSocket() throws GeneralSecurityException, IOException {
        char[] entryPassword = "relaypass".toCharArray();
        // COULD ADD PROVIDER IN THESE FOR CONSISTENCY
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER);
        keyManagerFactory.init(keyStore1, entryPassword);

        // specify TLS version e.g. TLSv1.3
        SSLContext sslContext = SSLContext.getInstance(TLS_VERSION);
        sslContext.init(keyManagerFactory.getKeyManagers(),null, SecureRandom.getInstance(RNG_ALGORITHM, RNG_PROVIDER));

        SSLServerSocketFactory fact = sslContext.getServerSocketFactory();
        return (SSLServerSocket) fact.createServerSocket(2048 );
    }

    private SSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException{

        char[] entryPassword = "relaypass".toCharArray();
        // COULD ADD PROVIDER IN THESE FOR CONSISTENCY
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER);
        keyManagerFactory.init(keyStore1, entryPassword);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KEY_MANAGER);
        trustManagerFactory.init(trustStore2);
        // specify TLS version e.g. TLSv1.3
        SSLContext sslContext = SSLContext.getInstance(TLS_VERSION);
        sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),
                SecureRandom.getInstance(RNG_ALGORITHM, RNG_PROVIDER));
       return  sslContext.getSocketFactory();
    }


}