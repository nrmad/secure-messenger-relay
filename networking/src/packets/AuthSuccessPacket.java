package packets;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AuthSuccessPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10003L;
    private int cid;
    private int nid;

    public AuthSuccessPacket(int cid, int nid){
        super(Type.AUTH_SUCCESS);
        this.cid = cid;
        this.nid = nid;
    }

    public AuthSuccessPacket() {
        super(Type.AUTH_SUCCESS);
    }

    public int getCid() {
        return cid;
    }

    public int getNid() {
        return nid;
    }


    @Override
    public void writeExternal(ObjectOutput out) {
    }

    @Override
    public void readExternal(ObjectInput in)  {
    }
}
