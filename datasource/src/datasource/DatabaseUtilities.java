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

    private static final String UPDATE_NETWORK_PORTS = "UPDATE networks SET port = ? WHERE nid = ?";
    private static final String UPDATE_NETWORK_ALIASES = "UPDATE networks SET network_alias = ? WHERE nid = ?";
    private static final String UPDATE_NETWORKS = "UPDATE networks SET port = ?, network_alias = ? WHERE nid = ?";
    // ??? MAYBE ALSO PRINT OUT FINGERPRINT
    private static final String SELECT_ALL_NETWORKS = "SELECT * FROM networks";
    private static final String INSERT_NETWORKS = "INSERT INTO networks(nid, fingerprint, port, network_alias) VALUES(?,?,?,?)";
    private static final String SELECT_NETWORKS = "SELECT fingerprint, port, network_alias FROM networks WHERE nid = ?";
    private static final String DELETE_NETWORKS = "DELETE FROM networks WHERE nid = ?";
    private static final String DELETE_NETWORKCONTACTS_NID = "DELETE FROM networkContacts WHERE nid = ?";
    private static final String DELETE_CONTACTS = "DELETE FROM contacts c INNER JOIN networkContacts nc ON nc.cid = c.cid INNER JOIN networks n ON n.nid = nc.nid WHERE n.nid = ?";
    private static final String SELECT_CONTACTS = "SELECT * FROM contacts c INNER JOIN networkContacts nc ON nc.cid = c.cid INNER JOIN networks n ON n.nid = nc.nid WHERE n.nid = ?";
    private static final String INSERT_CONTACT = "INSERT INTO contacts(cid, alias) VALUES(?,?)";
    private static final String DELETE_CONTACT = "DELETE FROM contacts WHERE cid = ?";
    private static final String INSERT_NETWORKCONTACTS_CID = "INSERT INTO networkContacts(nid, cid) VALUES (?,?)";
    private static final String DELETE_NETWORKCONTACTS_CID = "DELETE FROM networkContacts WHERE nid = ? AND cid = ?";
//    private static final String SEED_REGISTRATION_RECORD =  "INSERT INTO networks("



    private static final String RETRIEVE_MAX_NID = "SELECT COALESCE(MAX(nid), 0) FROM networks";

    private PreparedStatement queryUpdateNetworkPorts;
    private PreparedStatement queryUpdateNetworkAliases;
    private PreparedStatement queryUpdateNetworks;
    private PreparedStatement querySelectAllNetworks;
    private PreparedStatement queryInsertNetworks;
    private PreparedStatement querySelectNetworks;
    private PreparedStatement queryDeleteNetworks;
    private PreparedStatement queryDeleteNetworkContactsNid;
    private PreparedStatement queryDeleteContacts;
    private PreparedStatement querySelectContacts;
    private PreparedStatement queryInsertContact;
    private PreparedStatement queryDeleteContact;
    private PreparedStatement queryInsertNetworkContactsCid;
    private PreparedStatement queryDeleteNetworkContactsCid;


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

        queryUpdateNetworkPorts = conn.prepareStatement(UPDATE_NETWORK_PORTS);
        queryUpdateNetworkAliases = conn.prepareStatement(UPDATE_NETWORK_ALIASES);
        queryUpdateNetworks = conn.prepareStatement(UPDATE_NETWORKS);
        querySelectAllNetworks = conn.prepareStatement(SELECT_ALL_NETWORKS);
        queryInsertNetworks = conn.prepareStatement(INSERT_NETWORKS);
        querySelectNetworks = conn.prepareStatement(SELECT_NETWORKS);
        queryDeleteNetworks = conn.prepareStatement(DELETE_NETWORKS);
        queryDeleteNetworkContactsNid = conn.prepareStatement(DELETE_NETWORKCONTACTS_NID);
        queryDeleteContacts = conn.prepareStatement(DELETE_CONTACTS);
        querySelectContacts = conn.prepareStatement(SELECT_CONTACTS);
        queryInsertContact = conn.prepareStatement(INSERT_CONTACT);
        queryDeleteContact = conn.prepareStatement(DELETE_CONTACT);
        queryInsertNetworkContactsCid = conn.prepareStatement(INSERT_NETWORKCONTACTS_CID);
        queryDeleteNetworkContactsCid = conn.prepareStatement(DELETE_NETWORKCONTACTS_CID);

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
    public void closeConnection() {

        try {
            if(queryUpdateNetworkPorts != null){
                queryUpdateNetworkPorts.close();
            }
            if(queryUpdateNetworkAliases != null){
                queryUpdateNetworkAliases.close();
            }
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
            if(queryDeleteNetworkContactsNid != null){
                queryDeleteNetworkContactsNid.close();
            }
            if(querySelectContacts != null){
                querySelectContacts.close();
            }
            if(queryInsertContact != null){
                queryInsertContact.close();
            }
            if(queryDeleteContact != null){
                queryDeleteContact.close();
            }
            if(queryInsertNetworkContactsCid != null){
                queryInsertNetworkContactsCid.close();
            }
            if(queryDeleteNetworkContactsCid != null){
                queryDeleteNetworkContactsCid.close();
            }
            if (conn != null) {
                conn.close();
            }
            databaseUtilities = null;

        } catch (SQLException e) {
            System.out.println("Failed to close connection: " + e.getMessage());
        }
    }

    /**
     * Update the specified network ports
     * @param networks networks to update
     * @return whether the method succeeded or not
     */
    public boolean updateNetworkPorts(List<Network> networks){
        try {
            try {
                conn.setAutoCommit(false);
                queryUpdateNetworkPorts.clearBatch();

                for (Network network : networks) {
                    if (network.getPort() >= 1024 && network.getPort() <= 65535) {

                        queryUpdateNetworkPorts.setInt(1, network.getPort());
                        queryUpdateNetworkPorts.setInt(2, network.getNid());
                        queryUpdateNetworkPorts.addBatch();

                    } else {
                        throw new SQLException("Format incorrect");
                    }
                }

                if (Arrays.stream(queryUpdateNetworkPorts.executeBatch()).anyMatch(x -> x == 0))
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

    /**
     * Update the specified network aliases
     * @param networks the networks to update
     * @return whether the method succeeded or not
     */
    public boolean updateNetworkAliases(List<Network> networks){
        try {
            try {
                conn.setAutoCommit(false);
                queryUpdateNetworkAliases.clearBatch();


                for (Network network : networks) {
                    if (aliasPattern.matcher(network.getNetwork_alias()).matches()) {

                        queryUpdateNetworkAliases.setString(1, network.getNetwork_alias());
                        queryUpdateNetworkAliases.setInt(2, network.getNid());
                        queryUpdateNetworkAliases.addBatch();

                    } else {
                        throw new SQLException("Format incorrect");
                    }
                }

                if (Arrays.stream(queryUpdateNetworkAliases.executeBatch()).anyMatch(x -> x == 0))
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


    // MUST BE UPDATED TO ALLOW SELECTION OF JUST PORTS OR ALIASES
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
                networks.add(new Network(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3),
                        resultSet.getString(4)));
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
                // ??? WOULD AN EMPTY ARRAY NOT BE BETTER !!! DON'T THINK SO BECAUSE WE WANT DO DELETE ALL OR NONE
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

                deleteContacts(networks);
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

    /**
     * A private helper to deleteNetworks this method deletes network contacts
     * @param networks the networks to delete contacts for
     * @throws SQLException
     */
    private void deleteNetworkContacts(List<Network> networks) throws SQLException{

        queryDeleteNetworkContactsNid.clearBatch();
        for(Network network: networks){
            queryDeleteNetworkContactsNid.setInt(1,network.getNid());
            queryDeleteNetworkContactsNid.addBatch();
        }

        queryDeleteNetworkContactsNid.executeBatch();
    }

    private void deleteContacts(List<Network> networks) throws SQLException{

        queryDeleteContacts.clearBatch();
        for(Network network: networks){
            queryDeleteContacts.setInt(1, network.getNid());
            queryDeleteContacts.addBatch();
        }
        queryDeleteContacts.executeBatch();
    }

    /**
     * gets all the contacts for the specified network
     * @param network the network to retrieve contacts for
     * @return the contact list
     * @throws SQLException
     */
    public List<Contact> getNetworkContacts(Network network) throws SQLException{

        List<Contact> contacts = new ArrayList<>();
        querySelectContacts.setInt(1, network.getNid());
        ResultSet resultSet = querySelectContacts.executeQuery();

        if (resultSet.next()) {
            do {
                contacts.add(new Contact(resultSet.getString(1), resultSet.getString(2)));
            } while (resultSet.next());
        }
            return contacts;
    }


    /**
     * Add a new contact to the database for the specified network
     * @param contact the contact to add
     * @param network the network to add it to
     * @return true for success and false for failure
     */
    public boolean addContact(Contact contact, Network network){

        try {
            try {
                conn.setAutoCommit(false);
                queryInsertContact.setString(1, contact.getCid());
                queryInsertContact.setString(2,contact.getAlias());
                if (queryInsertContact.executeUpdate() == 0)
                    throw new SQLException();
                addNetworkContact(contact, network);
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

    /**
     * delete a contact from the database for the specified network
     * @param contact the contact to be deleted
     * @param network the network its being deleted from
     * @return true for success and false for failure
     */
    public boolean deleteContact(Contact contact, Network network){

        try {
            try {
                conn.setAutoCommit(false);
                deleteNetworkContact( contact, network);
                queryDeleteContact.setString(1, contact.getCid());
                if (queryDeleteContact.executeUpdate() == 0)
                    throw new SQLException();
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

    /**
     * adds the associative entity record connecting the new contact and the network it pertains to
     * @param contact the contact to connect
     * @param network the network to connect it to
     * @throws SQLException
     */
    private void addNetworkContact(Contact contact, Network network) throws SQLException{
        queryInsertNetworkContactsCid.setInt(1, network.getNid());
        queryInsertNetworkContactsCid.setString(2, contact.getCid());
        if (queryInsertNetworkContactsCid.executeUpdate()== 0)
            throw new SQLException();



    }

    /**
     * deletes the associative entity record connecting the specified contact do its pertaining network
     * @param contact the contact to disconnect
     * @param network the network it is to be disconnected from
     * @throws SQLException
     */
    private void deleteNetworkContact(Contact contact, Network network) throws SQLException{
        queryDeleteNetworkContactsCid.setInt(1, network.getNid());
        queryDeleteNetworkContactsCid.setString(2, contact.getCid());
        if(queryDeleteNetworkContactsCid.executeUpdate() == 0)
            throw new SQLException();
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
