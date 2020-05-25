package datasource;


import org.junit.After;
import org.junit.BeforeClass;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class DatabaseUtilitiesTest {

    private static DatabaseUtilities databaseUtilities;

    @BeforeClass
    public static void setUp() {
        try {
            DatabaseUtilities.setDatabaseUtilities("relay-app", "relaypass");
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
    public void updateNetworkPorts(){
        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, 2000, ""));
        assertFalse(databaseUtilities.updateNetworks(networks));

        // CODE TO ADD NETWORKS SO UPDATES CAN BE SUCCESSFUL

        networks.clear();
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2, "2", 3000, "dick" ));
        networks.add(new Network(3, "3",3005, "harry"));

        databaseUtilities.addNetworks(networks);
        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        for(Network network: networks){
            network.setPort(network.getPort()+1);
        }

        assertTrue(databaseUtilities.updateNetworkPorts(networks));
        System.out.println("here");

    }

    @org.junit.Test
    public void updateNetworkAliases(){
        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, -1, "james"));
        assertFalse(databaseUtilities.updateNetworkAliases(networks));

        // CODE TO ADD NETWORKS SO UPDATES CAN BE SUCCESSFUL

        networks.clear();
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2, "2", 3000, "dick" ));
        networks.add(new Network(3, "3",3005, "harry"));
        databaseUtilities.addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        for(Network network: networks){
            network.setNetwork_alias(network.getNetwork_alias()+"boi");
        }

        assertTrue(databaseUtilities.updateNetworkAliases(networks));
        System.out.println("here");
    }

    @org.junit.Test
    public void updateNetworks(){

        // !!  BOUNDARIES UNTESTED
        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, 2000, "james"));
        assertFalse(databaseUtilities.updateNetworks(networks));

        // CODE TO ADD NETWORKS SO UPDATES CAN BE SUCCESSFUL

        networks.clear();
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2, "2", 3000, "dick" ));
        networks.add(new Network(3, "3",3005, "harry"));
        databaseUtilities.addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        for(Network network: networks){
            network.setPort(network.getPort()+1);
            network.setNetwork_alias(network.getNetwork_alias()+"boi");
        }

        assertTrue(databaseUtilities.updateNetworks(networks));
        System.out.println("here");
    }

    @org.junit.Test
    public void getAllNetworks(){

        List<Network> networks = new ArrayList<>(), networks1;
        // call get with no networks and assert error
        try{
            databaseUtilities.getAllNetworks();
            fail("shouldn't succeed with no networks");
        } catch(SQLException e){assertTrue(true);}
        // add a bunch of networks then call get and assert true that it returns
        try {
            networks.add(new Network(1, "1",2050, "tom"));
            networks.add(new Network(2,"2", 3000, "dick"));
            networks.add(new Network(3, "3",3005, "harry"));
            databaseUtilities.addNetworks(networks);

            try{
                networks = databaseUtilities.getAllNetworks();
            }catch (SQLException e){}

            networks1 = databaseUtilities.getAllNetworks();
            for(int i = 0; i< networks.size(); i++){
                assertEquals(networks.get(i).getNid(), networks1.get(i).getNid());
                assertEquals(networks.get(i).getPort(), networks1.get(i).getPort());
                assertEquals(networks.get(i).getNetwork_alias(), networks1.get(i).getNetwork_alias());
            }
        }catch (SQLException e){
            fail("fucked it");
        }
    }

    @org.junit.Test
    public void addNetworks(){

        List<Network> networks = new ArrayList<>();
        // ACTUAL USAGE DOES NOT INCLUDE MANUAL NID BUT DOESN'T MATTER
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2,"2", 3000, "dick"));
        networks.add(new Network(3, "3",3005, "harry"));
        assertTrue(databaseUtilities.addNetworks(networks));
        networks.clear();

        networks.add(new Network(4, "4",70000, "jose"));
        assertFalse(databaseUtilities.addNetworks(networks));
        networks.clear();

        networks.add(new Network(4, "4",1023, "jose"));
        assertFalse(databaseUtilities.addNetworks(networks));
        networks.clear();

        networks.add(new Network(4, "4",3010, "~~~~"));
        assertFalse(databaseUtilities.addNetworks(networks));
        networks.clear();

    }

    @org.junit.Test
    public void getNetworks(){

        List<Network> networks = new ArrayList<>(), networks1;
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2,"2", 3000, "dick"));
        networks.add(new Network(3, "3",3005, "harry"));
        databaseUtilities.addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        try {
            networks1 = databaseUtilities.getNetworks(networks.subList(0, 1));
            for (int i = 0; i < networks1.size(); i++) {
                assertEquals(networks.get(i).getNid(), networks1.get(i).getNid());
                assertEquals(networks.get(i).getPort(), networks1.get(i).getPort());
                assertEquals(networks.get(i).getNetwork_alias(), networks1.get(i).getNetwork_alias());
            }
        }catch (SQLException e) {
        fail("should hace succeeded");
        }

        try{
            databaseUtilities.getNetworks(new ArrayList<>(Arrays.asList(new Network(4,"4", 5000,"jimmy"))));
            fail("shouldn't reach");
        }catch (SQLException e){
         assertTrue(true);
        }
    }

    @org.junit.Test
    public void deleteNetworks(){

        List<Network> networks = new ArrayList<>(), networks1 = new ArrayList<>();
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2,"2", 3000, "dick"));
        networks.add(new Network(3, "3",3005, "harry"));
        databaseUtilities.addNetworks(networks);

        try{
            networks = databaseUtilities.getAllNetworks();
        }catch (SQLException e){}

        assertTrue(databaseUtilities.deleteNetworks(networks));

        networks.clear();
        networks.add(new Network(1, "1",2050, "tom"));
        networks.add(new Network(2,"2", 3000, "dick"));
        databaseUtilities.addNetworks(networks);

        networks1.add(new Network(3, "3",3005, "harry"));

        assertFalse(databaseUtilities.deleteNetworks(networks1));
    }

    @org.junit.Test
    public void getNetworkContacts(){

    }




}