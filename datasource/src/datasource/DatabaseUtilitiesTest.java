package datasource;


import org.junit.After;
import org.junit.BeforeClass;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class DatabaseUtilitiesTest {

    private static DatabaseUtilities databaseUtilities;

    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/?useSSL=false";
    private static Connection conn;

    private static final String RETRIEVE_MAX_NID = "SELECT COALESCE(MAX(nid), 0) FROM networks";
    private static final String INSERT_NETWORKS = "INSERT INTO networks(nid, network_alias) VALUES(?,?)";
    private static final String INSERT_NETWORK_PORTS = "INSERT INTO networkPorts(nid, pid) VALUES(?,?)";

    private static PreparedStatement queryRetrieveMaxNid;
    private static PreparedStatement queryInsertNetworks;
    private static PreparedStatement queryInsertNetworkPorts;
    private static int networkCounter;
    private static Pattern aliasPattern = Pattern.compile("\\w{1,255}");



    @BeforeClass
    public static void setUp() {
        try {
            String username = "relay-app", password = "relaypass";
            conn = DriverManager.getConnection(CONNECTION_STRING, username, password);
            Statement statement = conn.createStatement();
            statement.execute("USE secure_messenger_relay");

            queryRetrieveMaxNid = conn.prepareStatement(RETRIEVE_MAX_NID);
            queryInsertNetworkPorts = conn.prepareStatement(INSERT_NETWORK_PORTS);
            queryInsertNetworks = conn.prepareStatement(INSERT_NETWORKS);
            ResultSet result;
            result = queryRetrieveMaxNid.executeQuery();
            if (result.next())
                networkCounter = result.getInt(1) + 1;

            seedDatabase();
            DatabaseUtilities.setDatabaseUtilities(username, password);
            databaseUtilities = DatabaseUtilities.getInstance();
            tempMethod();
        }catch(SQLException | IOException e){
            System.out.println(e.getMessage());
        }
    }

    @After
    public void tearDown() {
try {
    tempMethod();
}catch (SQLException e){}
}

    @org.junit.Test
    public void setDatabaseUtilities() {
        assertNotNull(databaseUtilities);
    }

//    @org.junit.Test
//    public void getInstance() {
//        try {
//            assertNotNull(DatabaseUtilities.getInstance());
//        }catch (SQLException e){}
//    }


    @org.junit.Test
    public void getAllNetworks(){

        List<Network> networks = new ArrayList<>(), networks1;
        // add a bunch of networks then call get and assert true that it returns
        try {
            networks.add(new Network(1, new Port(2049),"tom"));
            networks.add(new Network(2, new Port(2049), "dick"));
            networks.add(new Network(3, new Port(2049), "harry"));
            addNetworks(networks);


                networks = databaseUtilities.getAllNetworks();

            networks1 = databaseUtilities.getAllNetworks();
            for(int i = 0; i< networks.size(); i++){
                assertEquals(networks.get(i).getNid(), networks1.get(i).getNid());
                assertEquals(networks.get(i).getPort().getTLSPort(), networks1.get(i).getPort().getTLSPort());
                assertEquals(networks.get(i).getNetworkAlias(), networks1.get(i).getNetworkAlias());
            }
        }catch (SQLException e){
            fail("failed it");
        }
    }


    @org.junit.Test
    public void getNetworkContacts(){
        List<Network> networks = new ArrayList<>();
        List<Contact> contacts;
        networks.add(new Network(1,  "tom"));
        Contact contact1 = new Contact(1, "tomboi");
        Contact contact2 = new Contact(2, "dickboi");
        Contact contact3 = new Contact(3, "harryboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);
        Account account3 = new Account(3, "anon", "pass3", "salt3",12000);

        addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        databaseUtilities.addUser(contact2, networks.get(0), account2);
        databaseUtilities.addUser(contact3, networks.get(0), account3);

        try {
            contacts = databaseUtilities.getNetworkContacts(networks.get(0));
            assertTrue(contacts.contains(contact1));
            assertTrue(contacts.contains(contact2));
            assertTrue(contacts.contains(contact3));

            // TEST A FAIL CASE

            contacts = databaseUtilities.getNetworkContacts(new Network(5, "james"));
            assertTrue(contacts.isEmpty());

        }catch (SQLException e){
            fail();
        }
    }

    @org.junit.Test
    public void getAccount(){
        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1,  "tom"));
        Contact contact1 = new Contact(1, "tomboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);


        addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);

        try{
            assertEquals(account1,databaseUtilities.getAccount(account1));
            databaseUtilities.getAccount(account2);
            fail();
        }catch (SQLException e){}

    }

    @org.junit.Test
    public void updateAccountCredentials(){

        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, "tom"));
        Contact contact1 = new Contact(1, "tomboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);

        addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        account1 = new Account(account1.getAid(), account1.getUsername(), "newpass", "newsalt", 14000);
        assertTrue(databaseUtilities.updateAccountCredentials(account1));
        assertFalse(databaseUtilities.updateAccountCredentials(account2));
    }

    @org.junit.Test
    public void getContact(){
        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, "tom"));
        Contact contact1 = new Contact(1, "tomboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);

        addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        assertEquals(contact1, databaseUtilities.getContact(account1));
        databaseUtilities.getContact(account2);
        fail();
        }catch (SQLException e){}

    }

    @org.junit.Test
    public void addUser(){
        List<Network> networks = new ArrayList<>();
        List<Contact> contacts = new ArrayList<>();
        Contact contact1 = new Contact(1, "tomboi");
        Contact contact2 = new Contact(2, "dickboi");
        Contact contact3 = new Contact(3, "harryboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);
        Account account3 = new Account(3, "anon", "pass3", "salt3",12000);
        networks.add(new Network(1, "tom"));
        contacts.add(contact1);
        contacts.add(contact2);
        contacts.add(contact3);

        addNetworks(networks);
        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        databaseUtilities.addUser(contact2, networks.get(0), account2);
        databaseUtilities.addUser(contact3, networks.get(0), account3);

        try {
            assertEquals(contacts, databaseUtilities.getNetworkContacts(networks.get(0)));

            assertFalse(databaseUtilities.addUser(new Contact(4, "jimboi"), new Network(5,"drdontexist"),
                    new Account(1,"james", "pass", "salt",12000)));

        }catch (SQLException e) {
            fail();
        }

    }

    @org.junit.Test
    public void deleteUser(){

        List<Network> networks = new ArrayList<>();
//        List<Contact> contacts = new ArrayList<>();
        Contact contact1 = new Contact(1, "tomboi");
        Contact contact2 = new Contact(2, "dickboi");
        Contact contact3 = new Contact(3, "harryboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);
        Account account3 = new Account(3, "anon", "pass3", "salt3",12000);
        networks.add(new Network(1,  "tom"));

        addNetworks(networks);
        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        databaseUtilities.addUser(contact2, networks.get(0), account2);
        databaseUtilities.addUser(contact3, networks.get(0), account3);

       assertTrue(databaseUtilities.deleteUser(contact1));
       assertTrue(databaseUtilities.deleteUser(contact2));
       assertTrue(databaseUtilities.deleteUser(contact3));
       assertFalse(databaseUtilities.deleteUser(new Contact(66, "notAnAlias")));
        assertFalse(databaseUtilities.deleteUser(new Contact(99, "notAnAlias")));

        try {
            assertTrue(databaseUtilities.getNetworkContacts(networks.get(0)).isEmpty());
        }catch (SQLException e) {
            fail();
        }

    }

    @org.junit.Test
    public void getAccountNetwork() {

        List<Network> networks = new ArrayList<>();
        Network network1 = new Network(1,  "tom");
        Contact contact1 = new Contact(1, "tomboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        networks.add(network1);

        addNetworks(networks);
        try{
            networks = databaseUtilities.getAllNetworks();

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        Network network2 = databaseUtilities.getAccountNetwork(account1);
        assertEquals(network1, network2);
        }catch (SQLException e){fail();}

    }


    // ------------------------------------------
    public static void tempMethod() throws SQLException {

            Statement statement = conn.createStatement();
            statement.execute("DELETE FROM networkPorts");
//            statement.execute("DELETE FROM ports");
            statement.execute("DELETE FROM accountContact");
            statement.execute("DELETE FROM accounts");
            statement.execute("DELETE FROM networkContacts");
            statement.execute("DELETE FROM chatroomContacts");
            statement.execute("DELETE FROM networks");
            statement.execute("DELETE FROM contacts");
            statement.execute("DELETE FROM chatrooms");
    }

    public static boolean addNetworks(List<Network> networks) {

        try {
            int currentNetCount = networkCounter, tempNetCount;
            try {
                conn.setAutoCommit(false);
                queryInsertNetworks.clearBatch();
                queryInsertNetworkPorts.clearBatch();

                for (Network network : networks) {
                    if (aliasPattern.matcher(network.getNetworkAlias()).matches()) {
                        tempNetCount = networkCounter++;
                        queryInsertNetworks.setInt(1, tempNetCount);
                        queryInsertNetworks.setString(2, network.getNetworkAlias());
                        queryInsertNetworks.addBatch();

                        queryInsertNetworkPorts.setInt(1, tempNetCount);
                        queryInsertNetworkPorts.setInt(2, 2);
                        queryInsertNetworkPorts.addBatch();
                    } else {
                        throw new SQLException("Format incorrect");
                    }
                }
                if(Arrays.stream(queryInsertNetworks.executeBatch()).anyMatch(x -> x == 0)) {
                    throw new SQLException("Format incorrect");
                }
                queryInsertNetworkPorts.executeBatch();
                conn.commit();
                return true;

            } catch (SQLException e) {
                networkCounter = currentNetCount;
                System.out.println("failed to add networks" + e.getMessage());
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }catch (SQLException e){}
        return false;
    }


    private static void seedDatabase() throws SQLException{

        Statement statement = conn.createStatement();
        statement.execute("INSERT IGNORE INTO networks(nid, network_alias) VALUES(1, 'REGISTRATION')," +
                "(2, 'james')");
        statement.execute("INSERT IGNORE INTO ports(pid, port) VALUES(1, 2048)," +
                " (2, 2049)");
        statement.execute("INSERT IGNORE INTO networkPorts(nid, pid) VALUES(1,1)," +
                " (2,2)");

    }

}