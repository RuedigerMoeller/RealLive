package org.nustaq.reallive.sys.metadata;

import java.io.Serializable;

/**
 * Created by ruedi on 09.07.2014.
 */
public class ColumnMeta implements Serializable{
    String name;
    String displayName;
    int fieldId;
    String customMeta;

    public ColumnMeta() {
    }

    public ColumnMeta(String name, String displayName, int fieldId) {
        this.name = name;
        this.displayName = displayName;
        this.fieldId = fieldId;
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

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    public String getCustomMeta() {
        return customMeta;
    }

    public void setCustomMeta(String customMeta) {
        this.customMeta = customMeta;
    }
}
