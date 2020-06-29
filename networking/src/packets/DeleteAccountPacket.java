package packets;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DeleteAccountPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10010L;

    public DeleteAccountPacket(){
        super(Type.DELETE_ACCOUNT);
    }

    @Override
    public void writeExternal(ObjectOutput out) {
    }

    @Override
    public void readExternal(ObjectInput in) {
    }

}
