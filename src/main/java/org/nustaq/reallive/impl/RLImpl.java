package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.sys.metadata.ColumnMeta;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.reallive.sys.metadata.TableMeta;
import org.nustaq.reallive.sys.tables.ClusterClients;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.FSTClazzInfo;

import static java.util.Arrays.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 21.06.14.
 */
public class RLImpl extends RealLive {

    public static int CHANGE_Q_SIZE = 10000;
    public static int FILTER_Q_SIZE = 100000;
    ConcurrentHashMap<String, RLTable> tables = new ConcurrentHashMap<>();
    Metadata model;

    public RLImpl() {
        model = new Metadata();
        // configure conf
        initSystemTables();
    }

    protected void initSystemTables() {
        stream( new Class[] { SysTable.class, ClusterClients.class } ).forEach(
            (clz) -> createTable(clz.getSimpleName(), clz)
        );
    }

    public void createTable(String name, Class<? extends Record> clazz) {
        if (tables.get(name) != null ) {
            throw new RuntimeException("table already created");
        }
        pureCreateTable(name,clazz);
        addToSysTable(name, clazz);
    }

    private void addToSysTable(String name, Class rowClass) {
        TableMeta tableMeta = new TableMeta();
        tableMeta.setName(name);
        //fixme: annotation processing
        final FSTClazzInfo classInfo = conf.getClassInfo(rowClass);
        final FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();
        ColumnMeta cols[] = new ColumnMeta[fieldInfo.length];
        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            ColumnMeta cm = new ColumnMeta();
            cm.setName(fi.getField().getName());
            cm.setFieldId(fi.getStructOffset());
            cm.setDisplayName(cm.getName()); // annotation
            cols[i] = cm;
        }
        tableMeta.setColumns(cols);
        model.putTable(name,tableMeta);

        RLTable<SysTable> sysTables = getTable("SysTable");
        SysTable sysTab = sysTables.createForUpdate("name", true);
        sysTab.setTableName(name);
        sysTab.setDescription("Sample");
        sysTab.setMeta(tableMeta);
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

    @Override
    public Metadata getMetadata() {
        return model;
    }

}
