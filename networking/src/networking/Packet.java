package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Packet implements Externalizable {

    private String destination;
    private String source;
    private Type type;
    private String info;
    private String data;

    public Packet(){}

    public Packet(String destination, String source, Type type, String info){
        this(destination, source, type, info, "");
    }

    public Packet(String destination, String source, Type type, String info, String data) {
        this.destination = destination;
        this.source = source;
        this.type = type;
        this.info = info;
        this.data = data;
    }

    public String getDestination() {
        return destination;
    }

    public String getSource() {
        return source;
    }

    public Type getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }

    public String getData() {
        return data;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(destination);
        out.writeUTF(source);
        out.writeInt(type.getCode());
        out.writeUTF(info);
        out.writeUTF(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        destination =in.readUTF();
        source = in.readUTF();
        type = Type.valueOf(in.readInt());
        info = in.readUTF();
        data = in.readUTF();

    }
}
