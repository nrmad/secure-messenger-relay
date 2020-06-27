package datasource;

import java.util.Objects;

public class Account {

    private int aid;
    private String username;
    private String password;
    private String salt;
    private int iterations;


    public Account(int aid, String username, String password, String salt, int iterations) {
        this.aid = aid;
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.iterations = iterations;
    }

    public Account(String username, String password, String salt, int iterations) {
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.iterations = iterations;
    }

    /**
     * Account constructor containing just the username
     * @param username the username used to access the account record
     */
    public Account(String username){
        this.aid = -1;
        this.username = username;
        this.password = "";
        this.salt = "";
        this.iterations = -1;
    }
    public void setAid(int aid) {
        this.aid = aid;
    }

    public int getAid() {
        return aid;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSalt() {
        return salt;
    }

    public int getIterations() {
        return iterations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return aid == account.aid &&
                Objects.equals(username, account.username) &&
                Objects.equals(password, account.password) &&
                Objects.equals(salt, account.salt) &&
                iterations == account.iterations;
    }
}

