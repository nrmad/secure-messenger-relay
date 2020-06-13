package datasource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ReadPropertiesFile {

    private static ReadPropertiesFile readPropertiesFile;
    private static Properties properties = new Properties();
    private static FileInputStream ip;
    private static final String db_lock_key = "lock-string";
    private final String db_lock;


    private ReadPropertiesFile() throws IOException {

        ip = new FileInputStream("/etc/secure-messenger-relay/relay.properties");
        properties.load(ip);
        if(properties.containsKey(db_lock_key))
            db_lock = properties.getProperty(db_lock_key);
        else
            throw new IllegalArgumentException();
    }

    public static ReadPropertiesFile getInstance() throws IOException {
        if(readPropertiesFile == null)
            readPropertiesFile = new ReadPropertiesFile();
        return readPropertiesFile;
    }

    public String getDb_lock() {
        return db_lock;
    }
}