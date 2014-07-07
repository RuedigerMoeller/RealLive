package org.nustaq.reallive.sys.messages;

import java.io.Serializable;

/**
 * Created by ruedi on 07.07.14.
 */
public class AuthResponse implements Serializable {
    String sessionKey;
    boolean sucess;

    public AuthResponse() {
    }

    public AuthResponse(String sessionKey, boolean sucess) {
        this.sessionKey = sessionKey;
        this.sucess = sucess;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean isSucess() {
        return sucess;
    }

    public void setSucess(boolean sucess) {
        this.sucess = sucess;
    }

}
