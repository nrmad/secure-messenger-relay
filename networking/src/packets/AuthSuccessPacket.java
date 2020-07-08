package packets;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AuthSuccessPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10003L;
    int cid;
    int nid;
    int aid;

    public AuthSuccessPacket(){
        super(Type.AUTH_SUCCESS);
    }

    public AuthSuccessPacket(int cid, int nid, int aid){
        super(Type.AUTH_SUCCESS);
        this.cid = cid;
        this.nid = nid;
        this.aid = aid;
    }


    public int getCid() {
        return cid;
    }

    public int getNid() {
        return nid;
    }

    @Override
    public void writeExternal(ObjectOutput out)throws IOException {
        out.writeInt(aid);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    }
}
