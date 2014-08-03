package org.nustaq.reallive.sys.config;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 03.08.14.
 *
 * Can be used to override Annotations done on the schema
 *
 */
public class SchemaConfig implements Serializable {

    HashMap<String,TableConfig> tables;

    public TableConfig getTable(String table) {
        return tables.get(table);
    }

    public HashMap<String, TableConfig> getTables() {
        if ( tables == null )
            tables = new HashMap<>();
        return tables;
    }
}
