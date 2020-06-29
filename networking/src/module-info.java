module networking {
    requires datasource;
    requires org.bouncycastle.pkix;
    requires security;
    requires java.sql;
    exports networking;
    exports packets;
}