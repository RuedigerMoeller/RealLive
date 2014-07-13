package org.nustaq.reallive.sys.messages;

import java.io.Serializable;

/**
 * Created by ruedi on 08.07.2014.
 */
public class Invocation<T> implements Serializable {
    String name;
    T argument;
    String cbId;

    transient Object currentContext;

    public String getCbId() {
        return cbId;
    }

    public void setCbId(String cbId) {
        this.cbId = cbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getArgument() {
        return argument;
    }

    public void setArgument(T argument) {
        this.argument = argument;
    }

    public Object getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(Object currentContext) {
        this.currentContext = currentContext;
    }
}
