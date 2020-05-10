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
            "port INTEGER UNIQUE NOT NULL, network_alias VARCHAR(255) UNIQUE NOT NULL)";
    private static final String CREATE_CHATROOMS = "CREATE TABLE IF NOT EXISTS chatrooms(rid INTEGER PRIMARY KEY, room_alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORK_CONTACTS = "CREATE TABLE IF NOT EXISTS networkContacts( nid INTEGER, cid CHAR(88), PRIMARY KEY(nid,cid), " +
            "FOREIGN KEY(nid) REFERENCES networks(nid), FOREIGN KEY(cid) REFERENCES contacts(cid))";
    private static final String CREATE_CHATROOM_CONTACTS = "CREATE TABLE IF NOT EXISTS chatroomContacts( rid INTEGER, cid CHAR(88), PRIMARY KEY(rid,cid), " +
            "FOREIGN KEY(rid) REFERENCES chatrooms(rid), FOREIGN KEY(cid) REFERENCES contacts(cid))";

    private static final String UPDATE_NETWORKS = "UPDATE networks SET port = ?, network_alias = ? WHERE nid = ?";
    // ??? MAYBE ALSO PRINT OUT FINGERPRINT
    private static final String SELECT_ALL_NETWORKS = "SELECT nid, port, network_alias FROM networks";
    private static final String INSERT_NETWORKS = "INSERT INTO networks(nid, fingerprint, port, network_alias) VALUES(?,?,?,?)";
    private static final String SELECT_NETWORKS = "SELECT fingerprint, port, network_alias FROM networks WHERE nid = ?";
    private static final String DELETE_NETWORKS = "DELETE FROM networks WHERE nid = ?";
    private static final String DELETE_NETWORK_CONTACTS = "DELETE FROM networkContacts WHERE nid = ?";

    private static final String RETRIEVE_MAX_NID = "SELECT COALESCE(MAX(nid), 0) FROM networks";

    private PreparedStatement queryUpdateNetworks;
    private PreparedStatement querySelectAllNetworks;
    private PreparedStatement queryInsertNetworks;
    private PreparedStatement querySelectNetworks;
    private PreparedStatement queryDeleteNetworks;
    private PreparedStatement queryDeleteNetworkContacts;

    private PreparedStatement queryRetrieveMaxNid;

    private int networkCounter;

    private DatabaseUtilities(String username, String password) throws SQLException {
        openConnection(username, password);
        setupPreparedStatements();
        setupCounters();
    }


    // NOT SURE WHETHER TO HANDLE EXCEPTION HERE OR NOT
    public static void setDatabaseUtilities(String username, String password) throws SQLException {
        if (databaseUtilities == null)
            databaseUtilities = new DatabaseUtilities(username, password);
    }

    public static DatabaseUtilities getInstance() {
        return databaseUtilities;
    }

    private void openConnection(String username, String password) throws SQLException {

        conn = DriverManager.getConnection(CONNECTION_STRING, username, password);
        if (conn != null) {
            setupDatabase();
        }

    }

    /**
     * Setup query prepared statements
     */
    private void setupPreparedStatements() throws SQLException {

        queryUpdateNetworks = conn.prepareStatement(UPDATE_NETWORKS);
        querySelectAllNetworks = conn.prepareStatement(SELECT_ALL_NETWORKS);
        queryInsertNetworks = conn.prepareStatement(INSERT_NETWORKS);
        querySelectNetworks = conn.prepareStatement(SELECT_NETWORKS);
        queryDeleteNetworks = conn.prepareStatement(DELETE_NETWORKS);
        queryDeleteNetworkContacts = conn.prepareStatement(DELETE_NETWORK_CONTACTS);

        queryRetrieveMaxNid = conn.prepareStatement(RETRIEVE_MAX_NID);
    }

    /**
     * Setup the counter for the nid so we know the next available one on adding new networks
     */
    private void setupCounters() {

        try {
            ResultSet result;
            result = queryRetrieveMaxNid.executeQuery();
            if (result.next())
                networkCounter = result.getInt(1) + 1;
        } catch (SQLException e) {
            System.out.println("Failed to setup counters: " + e.getMessage());
        }
    }

    /**
     * sets up tables which do not already exist
     */
    private void setupDatabase() throws SQLException {

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
    private void closeConnection() {

        try {
            if (queryUpdateNetworks != null) {
                queryUpdateNetworks.close();
            }
            if (querySelectAllNetworks != null) {
                querySelectAllNetworks.close();
            }
            if (queryInsertNetworks != null) {
                queryInsertNetworks.close();
            }
            if(querySelectNetworks != null){
                querySelectNetworks.close();
            }
            if(queryDeleteNetworks != null){
                queryDeleteNetworks.close();
            }
            if(queryDeleteNetworkContacts != null){
                queryDeleteNetworkContacts.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Failed to close connection: " + e.getMessage());
        }
    }


    public boolean updateNetworks(List<Network> networks) {

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

                if (Arrays.stream(queryUpdateNetworks.executeBatch()).anyMatch(x -> x == 0))
                    throw new SQLException("update failed");
                conn.commit();
                return true;


            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
        }

        return false;
    }

    public List<Network> getAllNetworks() throws SQLException {

        List<Network> networks = new ArrayList<>();
        ResultSet resultSet = querySelectAllNetworks.executeQuery();
        if (resultSet.next()) {
            do {
                networks.add(new Network(resultSet.getInt(1), resultSet.getInt(2),
                        resultSet.getString(3)));
            } while(resultSet.next());
            return networks;
        } else {
            throw new SQLException("no networks exist");
        }
    }

    public boolean addNetworks(List<Network> networks) {

        try {
            try {
                conn.setAutoCommit(false);
                queryInsertNetworks.clearBatch();

                for (Network network : networks) {
                    if (network.getPort() >= 1024 && network.getPort() <= 65535 && aliasPattern.matcher(network.getNetwork_alias()).matches()) {
                        queryInsertNetworks.setInt(1, networkCounter++);
                        queryInsertNetworks.setString(2, network.getFingerprint());
                        queryInsertNetworks.setInt(3, network.getPort());
                        queryInsertNetworks.setString(4, network.getNetwork_alias());
                        queryInsertNetworks.addBatch();

                    } else {
                        throw new SQLException("Format incorrect");
                    }
                }
                if(Arrays.stream(queryInsertNetworks.executeBatch()).anyMatch(x -> x == 0))
                    throw new SQLException("Format incorrect");

                conn.commit();
                return true;

            } catch (SQLException e) {
                System.out.println("failed to add networks" + e.getMessage());
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }catch (SQLException e){}
        return false;
    }

    public List<Network> getNetworks(List<Network> networks) throws SQLException{

        for(Network network : networks){
            querySelectNetworks.clearParameters();
            querySelectNetworks.setInt(1,network.getNid());
            ResultSet resultSet = querySelectNetworks.executeQuery();
            if(resultSet.next()){
                network.setFingerprint(resultSet.getString(1));
                network.setPort(resultSet.getInt(2));
                network.setNetwork_alias(resultSet.getString(3));
            } else {
                throw new SQLException();
            }
        }
        return networks;
    }

    public boolean deleteNetworks(List<Network> networks){

        try {
            try {
                conn.setAutoCommit(false);
                queryDeleteNetworks.clearBatch();

                deleteNetworkContacts(networks);

                for (Network network : networks) {
                    queryDeleteNetworks.setInt(1, network.getNid());
                    queryDeleteNetworks.addBatch();
                }

                if (Arrays.stream(queryDeleteNetworks.executeBatch()).anyMatch(x -> x == 0))
                    throw new SQLException("update failed");
                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
        }
        return false;

    }

    private void deleteNetworkContacts(List<Network> networks) throws SQLException{

        queryDeleteNetworkContacts.clearBatch();
        for(Network network: networks){
            queryDeleteNetworkContacts.setInt(1,network.getNid());
            queryDeleteNetworkContacts.addBatch();
        }

        queryDeleteNetworkContacts.executeBatch();
    }

    // A TEMPORARY METHOD FOR TESTING PURPOSES
    public boolean tempMethod() {

        try {
            Statement statement = conn.createStatement();
            statement.execute("DELETE FROM networkContacts");
            statement.execute("DELETE FROM chatroomContacts");
            statement.execute("DELETE FROM networks");
            statement.execute("DELETE FROM contacts");
            statement.execute("DELETE FROM chatrooms");

            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
