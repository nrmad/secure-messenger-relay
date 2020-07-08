package datasource;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class Contact implements Externalizable {
    public static final long serialVersionUID = 10101L;
    private int cid;
    private String alias;

    public Contact() {
    }

    public Contact(int cid, String alias) {
        this.cid = cid;
        this.alias = alias;
    }

    public Contact(int cid) {
        this.cid = cid;
        this.alias = "";
    }

    public Contact(String alias) {
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
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(cid);
        out.writeUTF(alias);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {

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
