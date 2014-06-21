package org.nustaq.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.model.Record;
import org.nustaq.model.Schema;
import org.nustaq.model.Table;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 21.06.14.
 */
public class InMemSchema extends Schema {

    ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

    public void createTable(String name, Class<? extends Record> clazz) {
        TableImpl table = Actors.AsActor(TableImpl.class);
        table.$init(name,this);
        tables.put( name, table );
    }

    public Table getTable(String tableId) {
        return tables.get(tableId);
    }
}
