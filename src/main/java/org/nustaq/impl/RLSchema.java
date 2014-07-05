package org.nustaq.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.model.Record;
import org.nustaq.model.Schema;
import org.nustaq.model.Table;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 21.06.14.
 */
public class RLSchema extends Schema {

    public static final int Q_SIZE = 100000;
    ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

    public RLSchema() {
        // configure conf
    }

    public void createTable(String name, Class<? extends Record> clazz) {
        TableImpl table = Actors.AsActor(TableImpl.class, Q_SIZE);
        table.$init(name, this, clazz);
        CountDownLatch latch = new CountDownLatch(1);
        table.$sync().then( (r,e) -> latch.countDown() );
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tables.put( name, table );
    }

    public Table getTable(String tableId) {
        return tables.get(tableId);
    }
}
