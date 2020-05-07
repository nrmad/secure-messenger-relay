package datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtilities {

    private static DatabaseUtilities databaseUtilities;
    private Connection conn;

    private static final String DB_NAME = "secure-messenger-relay";
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/?useSSL=false";

    private static final String CREATE_DB = "CREATE DATABASE IF NOT EXISTS" + DB_NAME;
    private static final String USE_DB = "USE " + DB_NAME;
    private static final String CREATE_CONTACTS = "CREATE TABLE IF NOT EXISTS contacts(cid CHAR(88) PRIMARY KEY, alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORKS = "CREATE TABLE IF NOT EXISTS networks(nid INTEGER PRIMARY KEY, fingerprint CHAR(88) UNIQUE NOT NULL," +
                                                  "port INTEGER UNIQUE NOT NULL, network_alias VARCHAR(255))";
    private static final String CREATE_CHATROOMS = "CREATE TABLE IF NOT EXISTS chatrooms(rid INTEGER PRIMARY KEY, room_alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORK_CONTACTS  = "CREATE TABLE IF NOT EXISTS networkContacts( nid INTEGER, cid CHAR(88), PRIMARY KEY(nid,cid), " +
                                                           "FOREIGN KEY(nid) REFERENCES networks(nid), FOREIGN KEY(cid) REFERENCES contacts(cid)";
    private static final String CREATE_CHATROOM_CONTACTS = "CREATE TABLE IF NOT EXISTS chatroomContacts( rid INTEGER, cid CHAR(88), PRIMARY KEY(rid,cid), " +
                                                           "FOREIGN KEY(rid) REFERENCES chatrooms(rid), FOREIGN KEY(cid) REFERENCES contacts(cid)";

    private DatabaseUtilities(String username, String password){
        openConnection(username, password);
    }

    public static void setDatabaseUtilities(String username, String password){
        if(databaseUtilities == null)
        databaseUtilities = new DatabaseUtilities(username, password);
    }

    public static DatabaseUtilities getDatabaseUtilities(){
            return databaseUtilities;
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

            statement.execute(CREATE_DB);
            statement.execute(USE_DB);

            statement.addBatch(CREATE_CONTACTS);
            statement.addBatch(CREATE_NETWORKS);
            statement.addBatch(CREATE_CHATROOMS);
            statement.addBatch(CREATE_NETWORK_CONTACTS);
            statement.addBatch(CREATE_CHATROOM_CONTACTS);

            statement.executeBatch();

        } catch (SQLException e) {
            System.out.println("Failed to setup database: " + e.getMessage());
        }
    }

    /**
     * Closes the connection when the application closes
     */
    private void closeConnection() {

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
}
