package datasource;


import org.junit.Before;
import org.junit.BeforeClass;

import java.sql.SQLException;
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

    @org.junit.Test
    public void setDatabaseUtilities() {
            assertNotNull(databaseUtilities);
    }

    @org.junit.Test
    public void getInstance() {
        assertNotNull(DatabaseUtilities.getInstance());
    }
}