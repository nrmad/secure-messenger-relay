package datasource;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtilities {

    private static DatabaseUtilities databaseUtilities;
    private static ReadPropertiesFile propertiesFile;
    private Connection conn;

    private static final String DB_NAME = "secure_messenger_relay";
    private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/?useSSL=false";

    private static final String CREATE_DB = "CREATE DATABASE IF NOT EXISTS " + DB_NAME;
    private static final String USE_DB = "USE " + DB_NAME;

    private static final String GET_LOCK = "SELECT GET_LOCK(?, 0)";
    private static final String RELEASE_LOCK = "SELECT RELEASE_LOCK(?)";

    private static final String CREATE_ACCOUNT_CONTACT = "CREATE TABLE IF NOT EXISTS accountContact(aid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(aid,cid), FOREIGN KEY(aid) REFERENCES accounts(aid) ON DELETE CASCADE, " +
            "FOREIGN KEY(cid) REFERENCES contacts(cid) ON DELETE CASCADE)";
    private static final String CREATE_ACCOUNTS = "CREATE TABLE IF NOT EXISTS accounts(aid INTEGER PRIMARY KEY, " +
            "username VARCHAR(255) UNIQUE NOT NULL, password TEXT, salt CHAR(84) NOT NULL, iterations INTEGER NOT NULL)";
    private static final String CREATE_CONTACTS = "CREATE TABLE IF NOT EXISTS contacts(cid INTEGER PRIMARY KEY, " +
            "alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_PORTS = "CREATE TABLE IF NOT EXISTS ports(pid INTEGER PRIMARY KEY, " +
            "port INTEGER UNIQUE NOT NULL)";
    private static final String CREATE_NETWORK_PORTS = "CREATE TABLE IF NOT EXISTS networkPorts(nid INTEGER, pid INTEGER, " +
            "PRIMARY KEY(nid, pid), FOREIGN KEY(nid) REFERENCES networks(nid) ON DELETE CASCADE, FOREIGN KEY(pid) " +
            "REFERENCES ports(pid))";
    private static final String CREATE_NETWORKS = "CREATE TABLE IF NOT EXISTS networks(nid INTEGER PRIMARY KEY, " +
            "network_alias VARCHAR(255) UNIQUE NOT NULL)";
    private static final String CREATE_CHATROOMS = "CREATE TABLE IF NOT EXISTS chatrooms(rid INTEGER PRIMARY KEY, " +
            "room_alias VARCHAR(255) NOT NULL)";
    private static final String CREATE_NETWORK_CONTACTS = "CREATE TABLE IF NOT EXISTS networkContacts(nid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(nid,cid), FOREIGN KEY(nid) REFERENCES networks(nid) ON DELETE CASCADE, " +
            "FOREIGN KEY(cid) REFERENCES contacts(cid) ON DELETE CASCADE)";
    private static final String CREATE_CHATROOM_CONTACTS = "CREATE TABLE IF NOT EXISTS chatroomContacts( rid INTEGER, " +
            "cid INTEGER, PRIMARY KEY(rid,cid), FOREIGN KEY(rid) REFERENCES chatrooms(rid) ON DELETE CASCADE, " +
            "FOREIGN KEY(cid) REFERENCES contacts(cid) ON DELETE CASCADE)";

    private static final String SELECT_ALL_NETWORKS = "SELECT n.nid, p.port, n.network_alias FROM networks n INNER JOIN " +
            "networkPorts np ON n.nid = np.nid INNER JOIN ports p ON np.pid = p.pid";
    private static final String SELECT_NETWORK = "SELECT * from networks n INNER JOIN networkContacts nc ON n.nid = " +
            "nc.nid INNER JOIN contacts c ON nc.cid = c.cid INNER JOIN accountContact ac ON c.cid = ac.cid INNER JOIN " +
            "accounts a ON ac.aid = a.aid WHERE a.username = ?";
    private static final String SELECT_CONTACTS = "SELECT * FROM contacts c INNER JOIN networkContacts nc ON nc.cid = c.cid " +
            "INNER JOIN networks n ON n.nid = nc.nid WHERE n.nid = ?";
    private static final String INSERT_ACCOUNT =  "INSERT INTO accounts(aid, username, password, salt, iterations) " +
            "VALUES (?,?,?,?,?)";
    private static final String INSERT_CONTACT = "INSERT INTO contacts(cid, alias) VALUES(?,?)";
    private static final String INSERT_ACCOUNT_CONTACT = "INSERT INTO accountContact(aid, cid) VALUES (?,?)";
    private static final String INSERT_NETWORK_CONTACTS = "INSERT INTO networkContacts(nid, cid) VALUES (?,?)";
    private static final String DELETE_ACCOUNT = "DELETE accounts FROM accounts INNER JOIN accountContact ac ON " +
            "accounts.aid = ac.aid WHERE ac.cid = ?";
    private static final String DELETE_CONTACT = "DELETE FROM contacts WHERE cid = ?";
    private static final String SELECT_ACCOUNT = "SELECT * FROM accounts WHERE username = ? ";
    private static final String SELECT_CONTACT = "SELECT cid FROM contacts c INNER JOIN accountContact ac ON c.cid = ac.cid " +
            "INNER JOIN accounts a ON ac.aid = a.aid WHERE a.username = ?";
    private static final String UPDATE_ACCOUNT_CREDENTIALS = "UPDATE accounts SET password = ?, salt = ?, iterations = ?" +
            " WHERE aid = ?";
    // ALSO UPDATE PASSWORD AND ENFORCE TWO RECORDS
    private static final String CHECK_READY = " SELECT IF(EXISTS(SELECT * FROM networks WHERE nid = ? AND network_alias " +
            "= ? AND (SELECT COUNT(nid) FROM networks) > 1), 1, 0)";

    private static final String RETRIEVE_MAX_NID = "SELECT COALESCE(MAX(nid), 0) FROM networks";
    private static final String RETRIEVE_MAX_CID = "SELECT COALESCE(MAX(cid), 0) FROM contacts";
    private static final String RETRIEVE_MAX_AID = "SELECT COALESCE(MAX(aid), 0) FROM accounts";

    // make all static OR NONE STATIC
    private PreparedStatement querySelectAllNetworks;
    private PreparedStatement querySelectNetwork;
    private PreparedStatement querySelectContacts;
    private PreparedStatement queryInsertAccount;
    private PreparedStatement queryInsertContact;
    private PreparedStatement queryInsertAccountContact;
    private PreparedStatement queryDeleteContact;
    private PreparedStatement queryInsertNetworkContacts;
    private PreparedStatement queryDeleteAccount;
    private PreparedStatement querySelectAccount;
    private PreparedStatement querySelectContact;
    private PreparedStatement queryUpdateAccountCredentials;
    private static PreparedStatement queryGetLock;
    private static PreparedStatement queryReleaseLock;
    private static PreparedStatement queryCheckReady;

    private PreparedStatement queryRetrieveMaxNid;
    private PreparedStatement queryRetrieveMaxCid;
    private PreparedStatement queryRetrieveMaxAid;

    private int networkCounter;
    private int contactCounter;
    private int accountCounter;

    private DatabaseUtilities(String username, String password) throws SQLException {
        openConnection(username, password);
        setupPreparedStatements();
        getLock();
        setupCounters();
        checkReady();
    }


    // NOT SURE WHETHER TO HANDLE EXCEPTION HERE OR NOT
    public static void setDatabaseUtilities(String username, String password) throws SQLException, IOException {
        if (databaseUtilities == null) {
            propertiesFile = ReadPropertiesFile.getInstance();
            databaseUtilities = new DatabaseUtilities(username, password);
        }
    }

    public static DatabaseUtilities getInstance() throws SQLException{
        if(databaseUtilities != null)
            return databaseUtilities;
        else
            throw new SQLException();
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
        querySelectNetwork = conn.prepareStatement(SELECT_NETWORK);
        querySelectContacts = conn.prepareStatement(SELECT_CONTACTS);
        queryInsertAccount = conn.prepareStatement(INSERT_ACCOUNT);
        queryInsertContact = conn.prepareStatement(INSERT_CONTACT);
        queryInsertAccountContact = conn.prepareStatement(INSERT_ACCOUNT_CONTACT);
        queryDeleteContact = conn.prepareStatement(DELETE_CONTACT);
        queryInsertNetworkContacts = conn.prepareStatement(INSERT_NETWORK_CONTACTS);
        queryDeleteAccount = conn.prepareStatement(DELETE_ACCOUNT);
        querySelectAccount = conn.prepareStatement(SELECT_ACCOUNT);
        querySelectContact = conn.prepareStatement(SELECT_CONTACT);
        queryUpdateAccountCredentials = conn.prepareStatement(UPDATE_ACCOUNT_CREDENTIALS);
        queryGetLock = conn.prepareStatement(GET_LOCK);
        queryReleaseLock = conn.prepareStatement(RELEASE_LOCK);
        queryCheckReady = conn.prepareStatement(CHECK_READY);

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

    private void getLock() throws SQLException{

        queryGetLock.clearParameters();
        queryGetLock.setString(1, propertiesFile.getDbLock());
        ResultSet resultSet = queryGetLock.executeQuery();
        resultSet.next();
        if(resultSet.getInt(1) == 0)
            throw  new SQLException();
    }

    private void releaseLock() throws SQLException{

        queryReleaseLock.clearParameters();
        queryReleaseLock.setString(1, propertiesFile.getDbLock());
        queryReleaseLock.execute();
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
        statement.addBatch(CREATE_PORTS);
        statement.addBatch(CREATE_NETWORK_PORTS);
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
            releaseLock();

            if (querySelectAllNetworks != null) {
                querySelectAllNetworks.close();
            }
            if(querySelectNetwork != null){
                querySelectNetwork.close();
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
            if(queryInsertNetworkContacts != null){
                queryInsertNetworkContacts.close();
            }
            if(queryDeleteAccount != null){
                queryDeleteAccount.close();
            }
            if(querySelectAccount != null){
                querySelectContact.close();
            }
            if(querySelectContact != null){
                querySelectContact.close();
            }
            if(queryUpdateAccountCredentials != null){
                queryUpdateAccountCredentials.close();
            }
            if(queryGetLock != null){
                queryGetLock.close();
            }
            if(queryReleaseLock != null){
                queryReleaseLock.close();
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
            if(queryCheckReady != null){
                queryCheckReady.close();
            }
            if (conn != null) {
                conn.close();
            }
            databaseUtilities = null;

        } catch (SQLException e) {
            System.out.println("Failed to close connection: " + e.getMessage());
        }
    }

    // MAYBE CHECK PORTS TOO
    private void checkReady() throws SQLException{
            queryCheckReady.clearParameters();
            queryCheckReady.setInt(1, propertiesFile.getRegDefaultNid());
            queryCheckReady.setString(2, propertiesFile.getRegDefaultAlias());
            ResultSet resultSet = queryCheckReady.executeQuery();
            resultSet.next();
            if (!(resultSet.getInt(1) == 1))
                throw new SQLException();
    }



    public List<Network> getAllNetworks() throws SQLException {

        List<Network> networks = new ArrayList<>();
        ResultSet resultSet = querySelectAllNetworks.executeQuery();
        if (resultSet.next()) {
            do {
                networks.add(new Network(resultSet.getInt(1), new Port(resultSet.getInt(2)),
                        resultSet.getString(3)));
            } while (resultSet.next());
        }
        return networks;
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

    public Account getAccount(Account account) throws SQLException{
        querySelectAccount.clearParameters();
        querySelectAccount.setString(1, account.getUsername());
        ResultSet resultSet = querySelectAccount.executeQuery();
        if(resultSet.next())
            return new Account(resultSet.getInt(1), resultSet.getString(2),
                    resultSet.getString(3), resultSet.getString(4), resultSet.getInt(5));
        else
            throw new SQLException("Account not found");
    }

    public Network getAccountNetwork(Account account) throws SQLException{
        querySelectNetwork.clearParameters();
        querySelectNetwork.setString(1, account.getUsername());
        ResultSet resultSet = querySelectNetwork.executeQuery();
        if(resultSet.next())
            return new Network(resultSet.getInt(1), resultSet.getString(2));
        else
            throw new SQLException();
    }

    public boolean updateAccountCredentials(Account account){
        try {
            queryUpdateAccountCredentials.clearParameters();
            queryUpdateAccountCredentials.setString(1, account.getPassword());
            queryUpdateAccountCredentials.setString(2,account.getSalt());
            queryUpdateAccountCredentials.setInt(3, account.getIterations());
            queryUpdateAccountCredentials.setInt(4,account.getAid());
            if(!(queryUpdateAccountCredentials.executeUpdate() == 0))
                return true;
        }catch (SQLException e){}
        return false;
    }

    public Contact getContact(Account account) throws SQLException{
        querySelectContact.clearParameters();
        querySelectContact.setString(1, account.getUsername());
        ResultSet resultSet = queryDeleteContact.executeQuery();
        if(resultSet.next())
            return new Contact(resultSet.getInt(1));
        else
            throw new SQLException("Contact not found");
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
        queryInsertNetworkContacts.setInt(1, network.getNid());
        queryInsertNetworkContacts.setInt(2, contact.getCid());
        if (queryInsertNetworkContacts.executeUpdate()== 0)
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

    private void deleteAccount(Contact contact) throws SQLException{
        queryDeleteAccount.setInt(1, contact.getCid());
        if(queryDeleteAccount.executeUpdate() == 0)
            throw new SQLException();
    }


    private void deleteContact(Contact contact) throws SQLException{
        queryDeleteContact.setInt(1, contact.getCid());
        if (queryDeleteContact.executeUpdate() == 0)
            throw new SQLException();
    }

}
