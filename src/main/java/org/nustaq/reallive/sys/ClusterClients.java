package org.nustaq.reallive.sys;

import org.nustaq.reallive.Record;

import java.io.Serializable;

/**
 * Created by ruedi on 07.07.14.
 */
public class ClusterClients extends Record {

    String name;
    String state;
    int instanceNum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(int instanceNum) {
        this.instanceNum = instanceNum;
    }
}
