package networking;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SecureSocketManager {


    // !!! CENTRAL CONFIG PANNEL FOR THIS CLASS AND SECURITY UTILITIES
    private final static String KEY_MANAGER = "SunX509";
    private final static String TLS_VERSION = "TLS";
    private final static String RNG_ALGORITHM = "DEFAULT";
    private final static String RNG_PROVIDER = "BC";
    private final static int CON_TIMEOUT = 15000;

    private static SSLContext sslContext = null;

    public SecureSocketManager(KeyStore keystore, String password) throws GeneralSecurityException
    {
        initContext(keystore, password);
    }

    // IF THIS FAILS SO SHOULD LOGIN BECAUSE NO CONNECTIONS WILL BE ATTAINABLE WITHOUT THE SSLCONTEXT

    private static void initContext(KeyStore keystore, String password)
            throws GeneralSecurityException
    {
        char[] entryPassword = password.toCharArray();
        // COULD ADD PROVIDER IN THESE FOR CONSISTENCY
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER);
        keyManagerFactory.init(keystore, entryPassword);

        // specify TLS version e.g. TLSv1.3
        SSLContext sslContext = SSLContext.getInstance(TLS_VERSION);
        sslContext.init(keyManagerFactory.getKeyManagers(),null, SecureRandom.getInstance(RNG_ALGORITHM, RNG_PROVIDER));


        SecureSocketManager.sslContext = sslContext;
    }

    /**
     * takes a tlsPort which will be derived from the TBoard session and produces a
     * SSLServerSocket on it
     * @param tlsPort the port to listen on
     * @return SSLServerSocket
     * @throws IOException
     */
    public SSLServerSocket getSslServerSocket(int tlsPort)throws IOException{
        SSLServerSocketFactory fact = sslContext.getServerSocketFactory();
        return (SSLServerSocket) fact.createServerSocket(tlsPort);
    }

}
