package packets;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ShutdownPacket extends Packet implements Externalizable {


    public static final long serialVersionUID = 10005L;

    public ShutdownPacket(){}

    public ShutdownPacket(int destination, int source) {
        super(destination, source, Type.RELAY_SHUTDOWN);

    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }
}
