package org.nustaq.reallive.sys.metadata;

import java.io.Serializable;

/**
 * Created by ruedi on 09.07.2014.
 */
public class TableMeta implements Serializable {
    String name;
    String displayName;
    ColumnMeta columns[];
    String customMeta;

    public TableMeta() {
    }

    public TableMeta(String name, String displayName, ColumnMeta[] columns) {
        this.name = name;
        this.displayName = displayName;
        this.columns = columns;
    }

    public String getCustomMeta() {
        return customMeta;
    }

    public void setCustomMeta(String customMeta) {
        this.customMeta = customMeta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ColumnMeta[] getColumns() {
        return columns;
    }

    public void setColumns(ColumnMeta[] columns) {
        this.columns = columns;
    }
}
