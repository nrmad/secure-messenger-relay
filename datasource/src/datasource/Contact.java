package datasource;

import java.util.Objects;

public class Contact {

private int cid;
private String alias;

public Contact(int cid, String alias){
    this.cid = cid;
    this.alias = alias;
}

public Contact(int cid){
    this.cid = cid;
    this.alias = "";
}

public Contact(String alias){
    this.alias = alias;
}

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getCid() {
        return cid;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return cid == contact.cid &&
                Objects.equals(alias, contact.alias);
    }
}
