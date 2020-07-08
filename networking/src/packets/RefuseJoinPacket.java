package packets;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RefuseJoinPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10009L;
    // MAYBE IMPLEMENT ENUM FAIL REASON

    public RefuseJoinPacket(){
        super(Type.REFUSE_JOIN);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in)throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }

}
