package datasource;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DatabaseUtilities {

    private static DatabaseUtilities databaseUtilities;
    private Connection conn;

    private Pattern aliasPattern = Pattern.compile("\\w{1,255}");

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

    private static final String UPDATE_NETWORKS = "UPDATE networks SET port = ?, network_alias = ? WHERE nid = ?";
    private static final String GET_NETWORKS = "SELECT nid, port, network_alias FROM networks";

    private PreparedStatement queryUpdateNetworks;
    private PreparedStatement queryGetNetworks;

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
            if(queryUpdateNetworks != null){
                queryUpdateNetworks.close();
            }
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

        queryUpdateNetworks = conn.prepareStatement(UPDATE_NETWORKS);
        queryGetNetworks = conn.prepareStatement(GET_NETWORKS);

    }

    public boolean updateNetworks(List<Network> networks){

        try {
            try {

                conn.setAutoCommit(false);
                queryUpdateNetworks.clearBatch();

                for (Network network : networks) {
                    if (network.getPort() >= 1024 && network.getPort() <= 65535 && aliasPattern.matcher(network.getNetwork_alias()).matches()) {

                        queryUpdateNetworks.setInt(1, network.getPort());
                        queryUpdateNetworks.setString(2, network.getNetwork_alias());
                        queryUpdateNetworks.setInt(3, network.getNid());

                        queryUpdateNetworks.addBatch();

                    } else {
                        throw new SQLException("Format incorrect");
                    }
                }

                if(Arrays.stream(queryUpdateNetworks.executeBatch()).anyMatch(x -> x == 0))
                        throw new SQLException("update failed");
                conn.commit();
                return true;


            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }catch (SQLException e){}

        return false;
    }

    public List<Network> getNetworks() throws SQLException{

        List<Network> networks = new ArrayList<>();
        ResultSet resultSet = queryGetNetworks.executeQuery();
           if(resultSet.next()) {
            while(resultSet.next())
                networks.add(new Network(resultSet.getInt(1), resultSet.getInt(2),
                resultSet.getString(3)));
            return networks;
           }else {
               throw new SQLException("no networks exist");
           }
    }
}
