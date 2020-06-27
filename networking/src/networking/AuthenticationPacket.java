package networking;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AuthenticationPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10001L;
    private String username;
    private String password;

    public AuthenticationPacket(){}

    public AuthenticationPacket( String username, String password) {
        super(Type.AUTHENTICATE);
        this.username = username;
        this.password = password;

    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
       out.writeUTF(username);
       out.writeUTF(password);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
      username = in.readUTF();
      password = in.readUTF();
    }


}
