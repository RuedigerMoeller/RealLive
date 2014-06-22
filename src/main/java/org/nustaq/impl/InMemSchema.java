package org.nustaq.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.model.Record;
import org.nustaq.model.Schema;
import org.nustaq.model.Table;
import org.nustaq.serialization.FSTConfiguration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 21.06.14.
 */
public class InMemSchema extends Schema {

    public static final int Q_SIZE = 1000000;
    ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

    public InMemSchema() {
        // configure conf
    }

    public void createTable(String name, Class<? extends Record> clazz) {
        TableImpl table = Actors.AsActor(TableImpl.class, Q_SIZE);
        table.$init(name,this);
        tables.put( name, table );
    }

    public Table getTable(String tableId) {
        return tables.get(tableId);
    }
}
