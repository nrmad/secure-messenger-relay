package datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtilities {

    private static DatabaseUtilities databaseUtilities;
    private Connection conn;

    private static final String DB_NAME = "secure-messenger-relay";
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/"+DB_NAME+"?useSSL=false";

    private DatabaseUtilities(String username, String password){
        openConnection(username, password);
    }

    public static void setDatabaseUtilities(String username, String password){
        databaseUtilities = new DatabaseUtilities(username, password);
    }

    private void openConnection(String username, String password){
        try{
            conn = DriverManager.getConnection(CONNECTION_STRING, username, password);
            if(conn != null){
                setupDatabase();
            }
        } catch (SQLException e){}
    }

    /**
     * sets up tables which do not already exist
     */
    private void setupDatabase() {

        try {
            Statement statement = conn.createStatement();

//            statement.addBatch(CREATE_CONTACTS_TABLE);
//            statement.addBatch(CREATE_ACCOUNTS_TABLE);
//            statement.addBatch(CREATE_ACCOUNTCONTACT_TABLE);
//            statement.addBatch(CREATE_CHAT_TABLE);
//            statement.addBatch(CREATE_CHATMESSAGES_TABLE);
//            statement.addBatch(CREATE_MESSAGES_TABLE);
//            statement.addBatch(CREATE_SYNCHRONIZE_TABLE);

            statement.executeBatch();

        } catch (SQLException e) {
            System.out.println("Failed to setup database: " + e.getMessage());
        }
    }
}
