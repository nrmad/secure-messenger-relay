package packets;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CancelOperationPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10012L;

    public CancelOperationPacket(){
        super(Type.CANCEL_OPERATION);
    }

    @Override
    public void writeExternal(ObjectOutput out) {
    }

    @Override
    public void readExternal(ObjectInput in) {
    }
}
