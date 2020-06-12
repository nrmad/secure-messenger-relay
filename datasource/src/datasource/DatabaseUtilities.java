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

    private static final String CREATE_ACCOUNT_CONTACT = "CREATE TABLE IF NOT EXISTS accountContact(aid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(aid,cid), FOREIGN KEY(aid) REFERENCES accounts(aid), " +
            "FOREIGN KEY(cid) REFERENCES contacts(cid))";
    private static final String CREATE_ACCOUNTS = "CREATE TABLE IF NOT EXISTS accounts(aid INTEGER PRIMARY KEY, " +
            "username VARCHAR(255) UNIQUE NOT NULL, password TEXT, salt CHAR(84) NOT NULL, iterations INTEGER NOT NULL)";
    private static final String CREATE_CONTACTS = "CREATE TABLE IF NOT EXISTS contacts(cid INTEGER PRIMARY KEY, " +
            "alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORKS = "CREATE TABLE IF NOT EXISTS networks(nid INTEGER PRIMARY KEY, " +
            "fingerprint CHAR(88) UNIQUE NOT NULL, port INTEGER UNIQUE NOT NULL, " +
            "network_alias VARCHAR(255) UNIQUE NOT NULL)";
    private static final String CREATE_CHATROOMS = "CREATE TABLE IF NOT EXISTS chatrooms(rid INTEGER PRIMARY KEY, " +
            "room_alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORK_CONTACTS = "CREATE TABLE IF NOT EXISTS networkContacts(nid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(nid,cid), " +
            "FOREIGN KEY(nid) REFERENCES networks(nid), FOREIGN KEY(cid) REFERENCES contacts(cid))";
    private static final String CREATE_CHATROOM_CONTACTS = "CREATE TABLE IF NOT EXISTS chatroomContacts( rid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(rid,cid), FOREIGN KEY(rid) REFERENCES chatrooms(rid), " +
            "FOREIGN KEY(cid) REFERENCES contacts(cid))";

    // ??? MAYBE ALSO PRINT OUT FINGERPRINT
    private static final String SELECT_ALL_NETWORKS = "SELECT * FROM networks";
    private static final String SELECT_CONTACTS = "SELECT * FROM contacts c INNER JOIN networkContacts nc ON nc.cid = c.cid " +
            "INNER JOIN networks n ON n.nid = nc.nid WHERE n.nid = ?";
    private static final String INSERT_ACCOUNT =  "INSERT INTO accounts(aid, username, password, salt, iterations) " +
            "VALUES (?,?,?,?,?)";
    private static final String INSERT_CONTACT = "INSERT INTO contacts(cid, alias) VALUES(?,?)";
    private static final String INSERT_ACCOUNT_CONTACT = "INSERT INTO accountContact(aid, cid) VALUES (?,?)";
    private static final String INSERT_NETWORKCONTACTS_CID = "INSERT INTO networkContacts(nid, cid) VALUES (?,?)";
    private static final String DELETE_ACCOUNT = "DELETE FROM accounts a INNER JOIN accountContact ac a.aid = ac.aid " +
            "INNER JOIN contacts c ON ac.cid = c.cid WHERE cid = ?";
    private static final String DELETE_ACCOUNT_CONTACT = "DELETE FROM accountContact WHERE cid = ?";
    private static final String DELETE_CONTACT = "DELETE FROM contacts WHERE cid = ?";
    private static final String DELETE_NETWORKCONTACTS_CID = "DELETE FROM networkContacts WHERE cid = ?";

    private static final String RETRIEVE_MAX_NID = "SELECT COALESCE(MAX(nid), 0) FROM networks";
    private static final String RETRIEVE_MAX_CID = "SELECT COALESCE(MAX(cid), 0) FROM contacts";
    private static final String RETRIEVE_MAX_AID = "SELECT COALESCE(MAX(aid), 0) FROM accounts";

    private PreparedStatement querySelectAllNetworks;
    private PreparedStatement querySelectContacts;
    private PreparedStatement queryInsertAccount;
    private PreparedStatement queryInsertContact;
    private PreparedStatement queryInsertAccountContact;
    private PreparedStatement queryDeleteContact;
    private PreparedStatement queryInsertNetworkContactsCid;
    private PreparedStatement queryDeleteNetworkContactsCid;
    private PreparedStatement queryDeleteAccount;
    private PreparedStatement queryDeleteAccountContact;

    private PreparedStatement queryRetrieveMaxNid;
    private PreparedStatement queryRetrieveMaxCid;
    private PreparedStatement queryRetrieveMaxAid;

    private int networkCounter;
    private int contactCounter;
    private int accountCounter;

    private DatabaseUtilities(String username, String password) throws SQLException {
        openConnection(username, password);
        setupPreparedStatements();
        // !!! ADD LOCK
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

        querySelectAllNetworks = conn.prepareStatement(SELECT_ALL_NETWORKS);
        querySelectContacts = conn.prepareStatement(SELECT_CONTACTS);
        queryInsertAccount = conn.prepareStatement(INSERT_ACCOUNT);
        queryInsertContact = conn.prepareStatement(INSERT_CONTACT);
        queryInsertAccountContact = conn.prepareStatement(INSERT_ACCOUNT_CONTACT);
        queryDeleteContact = conn.prepareStatement(DELETE_CONTACT);
        queryInsertNetworkContactsCid = conn.prepareStatement(INSERT_NETWORKCONTACTS_CID);
        queryDeleteNetworkContactsCid = conn.prepareStatement(DELETE_NETWORKCONTACTS_CID);
        queryDeleteAccount = conn.prepareStatement(DELETE_ACCOUNT);
        queryDeleteAccountContact = conn.prepareStatement(DELETE_ACCOUNT_CONTACT);

        queryRetrieveMaxNid = conn.prepareStatement(RETRIEVE_MAX_NID);
        queryRetrieveMaxCid = conn.prepareStatement(RETRIEVE_MAX_CID);
        queryRetrieveMaxAid = conn.prepareStatement(RETRIEVE_MAX_AID);
    }

    /**
     * Setup the counter for the nid so we know the next available one on adding new networks
     */
    private void setupCounters() throws SQLException{

            ResultSet result;
            result = queryRetrieveMaxNid.executeQuery();
            if (result.next())
                networkCounter = result.getInt(1) + 1;
            result = queryRetrieveMaxCid.executeQuery();
            if(result.next())
                contactCounter = result.getInt(1) +1;
            result = queryRetrieveMaxAid.executeQuery();
            if(result.next())
                accountCounter = result.getInt(1) +1;
    }

    /**
     * sets up tables which do not already exist
     */
    private void setupDatabase() throws SQLException {

        Statement statement = conn.createStatement();

        statement.execute(CREATE_DB);
        statement.execute(USE_DB);

        statement.addBatch(CREATE_ACCOUNTS);
        statement.addBatch(CREATE_CONTACTS);
        statement.addBatch(CREATE_NETWORKS);
        statement.addBatch(CREATE_CHATROOMS);
        statement.addBatch(CREATE_ACCOUNT_CONTACT);
        statement.addBatch(CREATE_NETWORK_CONTACTS);
        statement.addBatch(CREATE_CHATROOM_CONTACTS);

        statement.executeBatch();
    }


    /**
     * Closes the connection when the application closes
     */
    public void closeConnection() {

        try {
            if (querySelectAllNetworks != null) {
                querySelectAllNetworks.close();
            }
            if(querySelectContacts != null){
                querySelectContacts.close();
            }
            if(queryInsertAccount != null){
                queryInsertAccount.close();
            }
            if(queryInsertContact != null){
                queryInsertContact.close();
            }
            if(queryInsertAccountContact != null){
                queryInsertAccountContact.close();
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
            if(queryDeleteAccount != null){
                queryDeleteAccount.close();
            }
            if(queryDeleteAccountContact != null){
                queryDeleteAccountContact.close();
            }
            if(queryRetrieveMaxNid != null){
                queryRetrieveMaxNid.close();
            }
            if(queryRetrieveMaxCid != null){
                queryRetrieveMaxCid.close();
            }
            if(queryRetrieveMaxAid != null){
                queryRetrieveMaxAid.close();
            }
            if (conn != null) {
                conn.close();
            }
            databaseUtilities = null;

        } catch (SQLException e) {
            System.out.println("Failed to close connection: " + e.getMessage());
        }
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
                contacts.add(new Contact(resultSet.getInt(1), resultSet.getString(2)));
            } while (resultSet.next());
        }
            return contacts;
    }


    /**
     * Add a new contact to the database for the specified network along with the account details
     * @param contact the contact to add
     * @param network the network to add it to
     * @return true for success and false for failure
     */
    public boolean addUser(Contact contact, Network network, Account account){

        try {
            try {
                conn.setAutoCommit(false);
                contact.setCid(contactCounter++);
                account.setAid(accountCounter++);
                addContact(contact);
                addAccount(account);
                addAccountContact(account, contact);
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

    private void addAccountContact(Account account, Contact contact) throws SQLException{
        queryInsertAccountContact.setInt(1, account.getAid());
        queryInsertAccountContact.setInt(2, contact.getCid());
        if(queryInsertAccountContact.executeUpdate() == 0)
            throw new SQLException();
    }

    private void addAccount(Account account) throws SQLException{
        queryInsertAccount.setInt(1, account.getAid());
        queryInsertAccount.setString(2, account.getUsername());
        queryInsertAccount.setString(3, account.getPassword());
        queryInsertAccount.setString(4,account.getSalt());
        queryInsertAccount.setInt(5,account.getIterations());
        if(queryInsertAccount.executeUpdate() == 0)
            throw new SQLException();

    }

    private void addContact(Contact contact) throws SQLException{
        queryInsertContact.setInt(1, contact.getCid());
        queryInsertContact.setString(2,contact.getAlias());
        if (queryInsertContact.executeUpdate() == 0)
            throw new SQLException();
    }

    /**
     * adds the associative entity record connecting the new contact and the network it pertains to
     * @param contact the contact to connect
     * @param network the network to connect it to
     * @throws SQLException
     */
    private void addNetworkContact(Contact contact, Network network) throws SQLException{
        queryInsertNetworkContactsCid.setInt(1, network.getNid());
        queryInsertNetworkContactsCid.setInt(2, contact.getCid());
        if (queryInsertNetworkContactsCid.executeUpdate()== 0)
            throw new SQLException();
    }

    /**
     * delete a contact from the database for the specified network
     * @param contact the contact to be deleted
     * @return true for success and false for failure
     */
    public boolean deleteUser(Contact contact){

        try {
            try {
                conn.setAutoCommit(false);
                deleteNetworkContact(contact);
                deleteAccountContact(contact);
                deleteAccount(contact);
                deleteContact(contact);
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
     * deletes the associative entity record connecting the specified contact do its pertaining network
     * @param contact the contact to disconnect
     * @throws SQLException
     */
    private void deleteNetworkContact(Contact contact) throws SQLException{
        queryDeleteNetworkContactsCid.setInt(1, contact.getCid());
        if(queryDeleteNetworkContactsCid.executeUpdate() == 0)
            throw new SQLException();
    }

    private void deleteAccount(Contact contact) throws SQLException{
        queryDeleteAccount.setInt(1, contact.getCid());
        if(queryDeleteAccount.executeUpdate() == 0)
            throw new SQLException();
    }

    private void deleteAccountContact(Contact contact) throws SQLException{
        queryDeleteAccountContact.setInt(1,contact.getCid());
        if(queryDeleteAccountContact.executeUpdate() == 0)
            throw new SQLException();
    }


    private void deleteContact(Contact contact) throws SQLException{
        queryDeleteContact.setInt(1, contact.getCid());
        if (queryDeleteContact.executeUpdate() == 0)
            throw new SQLException();
    }



    //  ------------------------- TEMPORARY METHODs FOR TESTING PURPOSES ----------------------------------------------
    public boolean tempMethod() {

        try {
            Statement statement = conn.createStatement();
            statement.execute("DELETE FROM networkContacts");
            statement.execute("DELETE FROM chatroomContacts");
            statement.execute("DELETE FROM networks");
            statement.execute("DELETE FROM accountContact");
            statement.execute("DELETE FROM accounts");
            statement.execute("DELETE FROM contacts");
            statement.execute("DELETE FROM chatrooms");

            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean addNetworks(List<Network> networks) {

        try {
            try {
                PreparedStatement queryInsertNetworks = conn.prepareStatement("INSERT INTO networks(nid, fingerprint, port," +
                        " network_alias) VALUES(?,?,?,?)");

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

         PreparedStatement querySelectNetworks = conn.prepareStatement("SELECT fingerprint, port, network_alias FROM " +
                 "networks WHERE nid = ?");

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


}
