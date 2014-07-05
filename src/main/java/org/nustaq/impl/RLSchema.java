package org.nustaq.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.model.RLStream;
import org.nustaq.model.RLTable;
import org.nustaq.model.Record;
import org.nustaq.model.Schema;
import org.nustaq.storage.TestRec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 21.06.14.
 */
public class RLSchema extends Schema {

    public static int CHANGE_Q_SIZE = 10000;
    public static int FILTER_Q_SIZE = 100000;
    ConcurrentHashMap<String, RLTable> tables = new ConcurrentHashMap<>();

    public RLSchema() {
        // configure conf
    }

    public void createTable(String name, Class<? extends Record> clazz) {
        RLTableImpl table = Actors.AsActor( RLTableImpl.class, new ElasticScheduler(1), CHANGE_Q_SIZE );
        SingleNodeStream stream = Actors.AsActor(SingleNodeStream.class,new ElasticScheduler(1), FILTER_Q_SIZE);
        stream.$init(table);
        table.$init(name, this, clazz, stream);
        CountDownLatch latch = new CountDownLatch(1);
        table.$sync().then( (r,e) -> latch.countDown() );
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tables.put( name, table );
    }

    public RLTable getTable(String tableId) {
        return tables.get(tableId);
    }

}
