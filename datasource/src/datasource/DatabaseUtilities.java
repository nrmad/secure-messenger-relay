package datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtilities {

    private static DatabaseUtilities databaseUtilities;
    private Connection conn;

    private static final String DB_NAME = "secure_messenger_relay";
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/?useSSL=false";

    private static final String CREATE_DB = "CREATE DATABASE IF NOT EXISTS " + DB_NAME;
    private static final String USE_DB = "USE " + DB_NAME;

    private static final String CREATE_CONTACTS = "CREATE TABLE IF NOT EXISTS contacts(cid CHAR(88) PRIMARY KEY, alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORKS = "CREATE TABLE IF NOT EXISTS networks(nid INTEGER PRIMARY KEY, fingerprint CHAR(88) UNIQUE NOT NULL," +
                                                  "port INTEGER UNIQUE NOT NULL, network_alias VARCHAR(255))";
    private static final String CREATE_CHATROOMS = "CREATE TABLE IF NOT EXISTS chatrooms(rid INTEGER PRIMARY KEY, room_alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORK_CONTACTS  = "CREATE TABLE IF NOT EXISTS networkContacts( nid INTEGER, cid CHAR(88), PRIMARY KEY(nid,cid), " +
                                                           "FOREIGN KEY(nid) REFERENCES networks(nid), FOREIGN KEY(cid) REFERENCES contacts(cid))";
    private static final String CREATE_CHATROOM_CONTACTS = "CREATE TABLE IF NOT EXISTS chatroomContacts( rid INTEGER, cid CHAR(88), PRIMARY KEY(rid,cid), " +
                                                           "FOREIGN KEY(rid) REFERENCES chatrooms(rid), FOREIGN KEY(cid) REFERENCES contacts(cid))";

    private DatabaseUtilities(String username, String password) throws SQLException{
        openConnection(username, password);
        setupPreparedStatements();
    }


    // NOT SURE WHETHER TO HANDLE EXCEPTION HERE OR NOT
    public static void setDatabaseUtilities(String username, String password) throws SQLException{
        if(databaseUtilities == null)
                databaseUtilities = new DatabaseUtilities(username, password);
    }

    public static DatabaseUtilities getInstance(){
            return databaseUtilities;
    }

    private void openConnection(String username, String password) throws SQLException {

            conn = DriverManager.getConnection(CONNECTION_STRING, username, password);
            if(conn != null){
                setupDatabase();
            }

    }

    /**
     * sets up tables which do not already exist
     */
    private void setupDatabase() throws SQLException{

            Statement statement = conn.createStatement();

            statement.execute(CREATE_DB);
            statement.execute(USE_DB);

            statement.addBatch(CREATE_CONTACTS);
            statement.addBatch(CREATE_NETWORKS);
            statement.addBatch(CREATE_CHATROOMS);
            statement.addBatch(CREATE_NETWORK_CONTACTS);
            statement.addBatch(CREATE_CHATROOM_CONTACTS);

            statement.executeBatch();

    }

    /**
     * Closes the connection when the application closes
     */
    private void closeConnection(){

        try {
//            if (queryInsertContact != null) {
//                queryInsertContact.close();
//            }
//            if (queryInsertAccount != null) {
//                queryInsertAccount.close();
//            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Failed to close connection: " + e.getMessage());
        }
    }

    /**
     * Setup query prepared statements
     */
    private void setupPreparedStatements() throws SQLException{

//            queryInsertContact = conn.prepareStatement(INSERT_CONTACT);
//            queryInsertAccount = conn.prepareStatement(INSERT_ACCOUNT);



    }
}
