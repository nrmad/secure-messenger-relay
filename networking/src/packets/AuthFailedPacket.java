package packets;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AuthFailedPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10004L;

    public AuthFailedPacket(){
        super(Type.AUTH_FAILED);
    }


    @Override
    public void writeExternal(ObjectOutput out) {
    }

    @Override
    public void readExternal(ObjectInput in)  {
    }
}
