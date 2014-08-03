package org.nustaq.reallive.sys.config;

import org.nustaq.serialization.dson.Dson;

import java.io.File;

/**
 * Created by ruedi on 03.08.14.
 */
public class ConfigReader {

    public static SchemaConfig readConfig(String file) throws Exception {
        return (SchemaConfig) new Dson()
            .map("schema", SchemaConfig.class)
            .map("table", TableConfig.class)
            .map("column", ColumnConfig.class)
            .readObject(new File(file));
    }
}
