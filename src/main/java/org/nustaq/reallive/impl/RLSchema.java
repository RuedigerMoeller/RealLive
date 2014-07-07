package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.Schema;
import org.nustaq.reallive.sys.ClusterClients;
import org.nustaq.reallive.sys.SysMeta;
import org.nustaq.reallive.sys.SysTable;

import static java.util.Arrays.*;
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
        initSystemTables();
    }

    protected void initSystemTables() {
        stream( new Class[] { SysTable.class, ClusterClients.class } ).forEach(
            (clz) -> createTable(clz.getSimpleName(), clz)
        );
    }

    public void createTable(String name, Class<? extends Record> clazz) {
        pureCreateTable(name,clazz);
        addToSysTable(name);
    }

    private void addToSysTable(String name) {
        RLTable<SysTable> sysTables = getTable("SysTable");
        SysTable sysTab = sysTables.createForUpdate("name", true);
        sysTab.setTableName(name);
        sysTab.setDescription("Sample");
        sysTab._setId(name);
        sysTab.$apply();
    }

    protected void pureCreateTable(String name, Class<? extends Record> clazz) {
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
