package datasource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ReadPropertiesFile {


    private static ReadPropertiesFile readPropertiesFile;
    private static Properties properties = new Properties();
    private static final String db_lock_key = "lock-string";
    private static final String reg_default_nid_key = "reg-default-nid";
    private static final String reg_default_alias_key = "reg-default-alias";
    private static final String auth_iterations_key = "auth-iterations";
    private final String db_lock;
    private final int reg_default_nid;
    private final String reg_default_alias;
    private final int auth_iterations;

    private ReadPropertiesFile() throws IOException {

        FileInputStream ip = new FileInputStream("/etc/secure-messenger-relay/relay.properties");
        properties.load(ip);
        if(properties.containsKey(db_lock_key) &&
                properties.containsKey(reg_default_nid_key) &&
                properties.containsKey(reg_default_alias_key) &&
                properties.contains(auth_iterations_key)){
            db_lock = properties.getProperty(db_lock_key);
            reg_default_nid = Integer.parseInt(properties.getProperty(reg_default_nid_key));
            reg_default_alias = properties.getProperty(reg_default_alias_key);
            auth_iterations = Integer.parseInt(properties.getProperty(auth_iterations_key));
        } else{
            throw new IllegalArgumentException();
        }
    }

    public static ReadPropertiesFile getInstance() throws IOException {
        if(readPropertiesFile == null)
            readPropertiesFile = new ReadPropertiesFile();
        return readPropertiesFile;
    }

    public String getDbLock() {
        return db_lock;
    }

    public int getRegDefaultNid() {
        return reg_default_nid;
    }

    public String getRegDefaultAlias() {
        return reg_default_alias;
    }

    public int getAuthIterations() {
        return auth_iterations;
    }
}