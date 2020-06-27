package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AckPacket extends Packet implements Externalizable {


    public static final long serialVersionUID = 10006L;
    private long mid;

    public AckPacket(){}

    public AckPacket(int destination, int source, int mid) {
        super(destination, source, Type.ACK);
        this.mid = mid;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeLong(mid);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        mid = in.readLong();
    }
}
