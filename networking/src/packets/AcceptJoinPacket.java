package packets;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AcceptJoinPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10011L;
    // MAYBE IMPLEMENT ENUM FAIL REASON

    public AcceptJoinPacket(){
        super(Type.ACCEPT_JOIN);
    }

    @Override
    public void writeExternal(ObjectOutput out) {
    }

    @Override
    public void readExternal(ObjectInput in) {
    }
}
