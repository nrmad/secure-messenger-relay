package packets;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class EndSessionPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10007L;

    public EndSessionPacket(){
        super(Type.END_SESSION);
    }

    public EndSessionPacket(int destination, int source) {
        super(destination, source, Type.END_SESSION);
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
