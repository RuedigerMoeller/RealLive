package org.nustaq.reallive.sys.messages;

import java.io.Serializable;

/**
 * Created by ruedi on 08.07.14.
 */
public class InvocationCallback implements Serializable {
    Object result;
    String cbId;

    public InvocationCallback() {
    }

    public InvocationCallback(Object result, String cbId) {
        this.result = result;
        this.cbId = cbId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getCbId() {
        return cbId;
    }

    public void setCbId(String cbId) {
        this.cbId = cbId;
    }
}
