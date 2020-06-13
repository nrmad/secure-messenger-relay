package datasource;


import org.junit.After;
import org.junit.BeforeClass;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class DatabaseUtilitiesTest {

    private static DatabaseUtilities databaseUtilities;


    @BeforeClass
    public static void setUp() {
        try {
            DatabaseUtilities.setDatabaseUtilities( "relay-app", "relaypass");
            databaseUtilities = DatabaseUtilities.getInstance();

        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @After
    public void tearDown() {

        databaseUtilities.tempMethod();
    }

    @org.junit.Test
    public void setDatabaseUtilities() {
            assertNotNull(databaseUtilities);
    }

    @org.junit.Test
    public void getInstance() {
        assertNotNull(DatabaseUtilities.getInstance());
    }




    @org.junit.Test
    public void getAllNetworks(){

        List<Network> networks = new ArrayList<>(), networks1;
        // add a bunch of networks then call get and assert true that it returns
        try {
            networks.add(new Network(1, "1",2050, "tom"));
            networks.add(new Network(2,"2", 3000, "dick"));
            networks.add(new Network(3, "3",3005, "harry"));
            databaseUtilities.addNetworks(networks);


                networks = databaseUtilities.getAllNetworks();

            networks1 = databaseUtilities.getAllNetworks();
            for(int i = 0; i< networks.size(); i++){
                assertEquals(networks.get(i).getNid(), networks1.get(i).getNid());
                assertEquals(networks.get(i).getPort(), networks1.get(i).getPort());
                assertEquals(networks.get(i).getNetwork_alias(), networks1.get(i).getNetwork_alias());
            }
        }catch (SQLException e){
            fail("failed it");
        }
    }


    @org.junit.Test
    public void getNetworkContacts(){
        List<Network> networks = new ArrayList<>();
        List<Contact> contacts;
        networks.add(new Network(1, "1",2050, "tom"));
        Contact contact1 = new Contact(1, "tomboi");
        Contact contact2 = new Contact(2, "dickboi");
        Contact contact3 = new Contact(3, "harryboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);
        Account account3 = new Account(3, "anon", "pass3", "salt3",12000);

        databaseUtilities.addNetworks(networks);

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
    public void addUser(){
        List<Network> networks = new ArrayList<>();
        List<Contact> contacts = new ArrayList<>();
        Contact contact1 = new Contact(1, "tomboi");
        Contact contact2 = new Contact(2, "dickboi");
        Contact contact3 = new Contact(3, "harryboi");
        Account account1 = new Account(1,"nrmad", "pass1", "salt1", 12000);
        Account account2 = new Account(2,"azt4er", "pass2", "salt2", 12000);
        Account account3 = new Account(3, "anon", "pass3", "salt3",12000);
        networks.add(new Network(1, "1",2050, "tom"));
        contacts.add(contact1);
        contacts.add(contact2);
        contacts.add(contact3);

        databaseUtilities.addNetworks(networks);
        try{
            networks = databaseUtilities.getNetworks(networks);
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
        networks.add(new Network(1, "1",2050, "tom"));
//        contacts.add(contact1);
//        contacts.add(contact2);
//        contacts.add(contact3);

        databaseUtilities.addNetworks(networks);
        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        databaseUtilities.addUser(contact1, networks.get(0), account1);
        databaseUtilities.addUser(contact2, networks.get(0), account2);
        databaseUtilities.addUser(contact3, networks.get(0), account3);

       assertTrue(databaseUtilities.deleteUser(contact1, account1));
       assertTrue(databaseUtilities.deleteUser(contact2, account2));
       assertTrue(databaseUtilities.deleteUser(contact3, account3));
       assertFalse(databaseUtilities.deleteUser(new Contact(66, "notAnAlias"), new Account(66,"james", "barnoby","salty",12000)));
        assertFalse(databaseUtilities.deleteUser(new Contact(99, "notAnAlias"), new Account(99,"jack", "johnson","salty",12000)));

        try {
            assertTrue(databaseUtilities.getNetworkContacts(networks.get(0)).isEmpty());
        }catch (SQLException e) {
            fail();
        }

    }




}