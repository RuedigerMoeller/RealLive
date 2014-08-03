package org.nustaq.reallive.sys.config;

import org.nustaq.serialization.dson.Dson;

import java.io.File;

/**
 * Created by ruedi on 03.08.14.
 */
public class ConfigReader {

    public static void init() {
        Dson.defaultMapper
            .map("schema", SchemaConfig.class)
            .map("table", TableConfig.class)
            .map("column", ColumnConfig.class);
        Dson.defaultMapper.implyAttrFromType("schema", "tables" );
        Dson.defaultMapper.implyAttrFromType("table", "columns" );
    }

    public static SchemaConfig readConfig(String file) throws Exception {
        return (SchemaConfig) Dson.getInstance().readObject(new File(file));
    }
}
