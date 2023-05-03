package bgu.spl.net.srv;

public class User {
    private String username;
    private String password;
    private boolean activeConnection;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        activeConnection = false;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public boolean getConnection(){
        return activeConnection;
    }

    public void setConnection(boolean activeConnection){
        this.activeConnection = activeConnection;
    }
}
