package datasource;

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
}

