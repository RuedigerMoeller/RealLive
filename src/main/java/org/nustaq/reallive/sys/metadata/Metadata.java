package org.nustaq.reallive.sys.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ruedi on 10.07.2014.
 */
public class Metadata implements Serializable {

    String name;
    Map<String,TableMeta> tables = new HashMap<>();

    public TableMeta getTable(String name) {
        return tables.get(name);
    }

    public void putTable(String name, TableMeta meta) {
        tables.put(name,meta);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
