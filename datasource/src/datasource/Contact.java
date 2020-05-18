package datasource;

public class Contact {

private String cid;
private String alias;

public Contact(String cid, String alias){
    this.cid = cid;
    this.alias = alias;
}

    public String getCid() {
        return cid;
    }

    public String getAlias() {
        return alias;
    }
}
