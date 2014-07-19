package org.nustaq.reallive.sys.messages;

import java.io.Serializable;

/**
 * Created by ruedi on 08.07.14.
 */
public class InvocationCallback implements Serializable {
    Object result;
    String cbId;
    int sequence;

    public InvocationCallback() {
    }

    public InvocationCallback(Object result, String cbId) {
        this.result = result;
        this.cbId = cbId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
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
