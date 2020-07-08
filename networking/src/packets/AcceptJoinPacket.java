package packets;

import datasource.Account;
import datasource.Contact;
import datasource.Network;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class AcceptJoinPacket extends Packet implements Externalizable {

    public static final long serialVersionUID = 10011L;
    Contact contact;
    Network network;
    Account account;
    List<Contact> contacts;

    public AcceptJoinPacket(){
        super(Type.ACCEPT_JOIN);
    }

    public AcceptJoinPacket(Contact contact, Network network, Account account, List<Contact> contacts) {
        super(Type.AUTH_SUCCESS);
        this.contact = contact;
        this.network = network;
        this.account = account;
        this.contacts = contacts;
    }




    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(contact);
            out.writeObject(network);
            out.writeObject(account);
            out.writeInt(contacts.size());
            for (Contact cntct : contacts) {
                out.writeObject(cntct);
            }
    }

    @Override
    public void readExternal(ObjectInput in)throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }
}
