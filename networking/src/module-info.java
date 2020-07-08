module networking {
    requires datasource;
    requires org.bouncycastle.pkix;
    requires security;
    requires java.sql;
    requires junit;
    exports networking;
    exports packets;
}