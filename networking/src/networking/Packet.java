package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Packet implements Externalizable {

    private int destination;
    private int source;
    private Type type;
    private String info;
    private String data;

    public Packet(){}

    public Packet(int destination, int source, Type type, String info){
        this(destination, source, type, info, "");
    }

    public Packet(int destination, int source, Type type, String info, String data) {
        this.destination = destination;
        this.source = source;
        this.type = type;
        this.info = info;
        this.data = data;
    }

    public int getDestination() {
        return destination;
    }

    public int getSource() {
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
        out.writeInt(destination);
        out.writeInt(source);
        out.writeInt(type.getCode());
        out.writeUTF(info);
        out.writeUTF(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        destination =in.readInt();
        source = in.readInt();
        type = Type.valueOf(in.readInt());
        info = in.readUTF();
        data = in.readUTF();

    }

    public static Packet getShutdownPacket(int destination, int source){
        return new Packet(destination, source, Type.RELAY_SHUTDOWN, "", "");
    }

    public static Packet getAuthSuccessPacket(int cid){
        return new Packet(-1, cid, Type.AUTH_SUCCESS, "", "" );
    }

    public static Packet getAuthFailedPacket(){
        return new Packet(-1,-1, Type.AUTH_FAILED, "", "");
    }
}
