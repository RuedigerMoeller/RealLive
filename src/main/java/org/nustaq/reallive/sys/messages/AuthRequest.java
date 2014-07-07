package org.nustaq.reallive.sys.messages;

import java.io.Serializable;

/**
 * Created by ruedi on 07.07.14.
 */
public class AuthRequest implements Serializable {
    String user;
    String pwd;
    String misc;

    public AuthRequest() {
    }

    public AuthRequest(String user, String pwd, String misc) {
        this.user = user;
        this.pwd = pwd;
        this.misc = misc;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }
}
