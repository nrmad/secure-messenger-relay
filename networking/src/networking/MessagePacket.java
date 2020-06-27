package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MessagePacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10002L;
    private long sentTimeMillis;
    private String data;

    public MessagePacket(){}

    public MessagePacket(int destination, int source, long sentTimeMillis, String data) {
        super(destination, source, Type.MESSAGE);
        this.sentTimeMillis = sentTimeMillis;
        this.data = data;
    }

    public long getSentTimeMillis() {
        return sentTimeMillis;
    }

    public String getData() {
        return data;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeLong(sentTimeMillis);
        out.writeUTF(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        sentTimeMillis = in.readLong();
        data = in.readUTF();

    }
}
