package packets;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RequestJoinPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10008L;
    private String username;
    private String password;
    private String alias;
    private int cid;

    public RequestJoinPacket(){
        super(Type.REQUEST_JOIN);
    }

    public RequestJoinPacket(int destination, int source, String alias) {
        super(destination, source, Type.REQUEST_JOIN);
        this.alias = alias;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAlias() {
        return alias;
    }


    public int getCid() {
        return cid;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(alias);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        username = in.readUTF();
        password = in.readUTF();
        alias = in.readUTF();
        cid = in.readInt();
    }
}
