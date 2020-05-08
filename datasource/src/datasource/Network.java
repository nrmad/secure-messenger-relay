package datasource;

public class Network {

    private int nid;
    private String fingerprint;
    private int port;
    private String network_alias;

    /**
     * Network constructor used for Networks containing no alias or fingerprint
     * @param nid network id
     * @param port tls port
     */
    public Network(int nid, int port){
        this(nid, "", port, "");
    }

    /**
     * Network constructor used for Networks containing no fingerprint
     * @param nid network id
     * @param port tls port
     * @param network_alias network alias
     */
    public Network(int nid, int port, String network_alias){
        this(nid, "", port, network_alias);
    }

    /**
     * Network constructor used for Networks containing only a port (creating new ones)
     * @param port
     */
    public Network(int port){
        this(-1, "", port);
    }

    /**
     * Network constructor used for Networks containing only port and alias (creating new ones)
     * @param port tls port
     * @param network_alias network alias
     */
    public Network(int port, String network_alias){
        this(-1, "", port, network_alias);
    }

    /**
     * Network constructor used for Networks containing no alias
     * @param nid netwerk id
     * @param fingerprint public key fingerprint
     * @param port tls port
     */
    public Network(int nid, String fingerprint, int port){
        this(nid, fingerprint, port, "");
    }

    /**
     * Network contructor used for fully defined networks
     * @param nid network id
     * @param fingerprint public key fingerprint
     * @param port tls port
     * @param network_alias network alias
     */
    public Network(int nid, String fingerprint, int port, String network_alias){
        this.nid = nid;
        this.fingerprint = fingerprint;
        this.port = port;
        this.network_alias = network_alias;
    }

    public int getNid() {
        return nid;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public int getPort() {
        return port;
    }

    public String getNetwork_alias() {
        return network_alias;
    }
}
