package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Packet implements Externalizable {

    public static final long serialVersionUID = 10000;
    private Type type;
    private int destination;
    private int source;


    public Packet(){}

    public Packet(Type type){
        this.type = type;
    }

    public Packet(int destination, int source, Type type){
        this.destination = destination;
        this.source = source;
        this.type = type;
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(destination);
        out.writeInt(source);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        destination =in.readInt();
        source = in.readInt();
    }

     enum Type {
        MESSAGE,
        ACK,
        END_SESSION,
        RELAY_SHUTDOWN,
        ACCEPT_USER,
        REQUEST_USER,
        AUTHENTICATE,
        AUTH_FAILED,
        AUTH_SUCCESS;
    }
}
