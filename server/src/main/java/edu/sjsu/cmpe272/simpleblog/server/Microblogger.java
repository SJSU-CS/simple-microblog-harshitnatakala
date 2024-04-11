package edu.sjsu.cmpe272.simpleblog.server;

public class Microblogger {
    private String publicKey;
    private String user;

    public Microblogger() {}

    public Microblogger(String user, String publicKey) {
        this.publicKey = publicKey;
        this.user = user;
    }

    // Getters

    public String getPublicKey() {
        return publicKey;
    }
    public String getUser() {
        return user;
    }


    // Setters

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setUser(String user) {
        this.user = user;
    }

}

