package org.nustaq.reallive.sys;

import org.nustaq.reallive.Record;

/**
 * Created by ruedi on 07.07.14.
 */
public class SysTable extends Record {

    String tableName;
    String description;

    int sizeMB;
    int freeMB;
    int numElems;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSizeMB() {
        return sizeMB;
    }

    public void setSizeMB(int sizeMB) {
        this.sizeMB = sizeMB;
    }

    public int getFreeMB() {
        return freeMB;
    }

    public void setFreeMB(int freeMB) {
        this.freeMB = freeMB;
    }

    public int getNumElems() {
        return numElems;
    }

    public void setNumElems(int numElems) {
        this.numElems = numElems;
    }
}
