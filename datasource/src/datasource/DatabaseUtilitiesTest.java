package datasource;


import org.junit.BeforeClass;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @org.junit.Test
    public void setDatabaseUtilities() {
            assertNotNull(databaseUtilities);
    }

    @org.junit.Test
    public void getInstance() {
        assertNotNull(DatabaseUtilities.getInstance());
    }

    @org.junit.Test
    public void updateNetworks(){

        List<Network> networks = new ArrayList<>();
        networks.add(new Network(1, 50, "james"));
        assertFalse(databaseUtilities.updateNetworks(networks));

        // CODE TO ADD NETWORKS SO UPDATES CAN BE SUCCESSFUL
    }

    @org.junit.Test
    public void getNetworks(){

        // call get with no networks and assert error
        try{
            databaseUtilities.getNetworks();
            fail("shouldn't succeed with no networks");
        } catch(SQLException e){assertTrue(true);}
        // add a bunch of networks then call get and assert true that it returns


    }

}