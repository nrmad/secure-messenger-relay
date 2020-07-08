package datasource;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Port implements Externalizable {

    public static final long serialVersionUID = 10103L;
    private int pid;
    private int port;

    public Port(){}

    public Port(int pid, int port) {
        this.pid = pid;
        this.port = port;
    }

    public Port(int port) {
        this.port = port;
    }

    public int getPid() {
        return pid;
    }

    public int getTLSPort() {
        return port;
    }

    @Override
    public void writeExternal(ObjectOutput out)throws IOException {
        out.writeInt(pid);
        out.writeInt(port);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {

    }

}
