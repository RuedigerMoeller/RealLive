package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.reallive.RLStream;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.sys.annotations.*;
import org.nustaq.reallive.sys.metadata.ColumnMeta;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.reallive.sys.metadata.TableMeta;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.FSTClazzInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 21.06.14.
 */
public class RLImpl extends RealLive {

    public static int CHANGE_Q_SIZE = 50000;
    public static int FILTER_Q_SIZE = 100000;

    ConcurrentHashMap<String, RLTable> tables = new ConcurrentHashMap<>();
    Metadata model;

    public RLImpl() {
        model = new Metadata();
        // configure conf
        initSystemTables();
    }

    public RLImpl(String dataDir) {
        model = new Metadata();
        dataDirectory = dataDir;
        // configure conf
        initSystemTables();
    }

    protected void initSystemTables() {
        Arrays.stream(new Class[]{SysTable.class}).forEach(
            (clz) -> createTable(clz.getSimpleName(), clz)
        );
    }

    public RealLive createTable(String name, Class<? extends Record> clazz) {
        if (tables.get(name) != null ) {
            throw new RuntimeException("table already created");
        }
        if ( clazz.getAnnotation(Virtual.class) == null ) {
            pureCreateTable(name, clazz);
        }
        addToSysTable(name, clazz);
        return this;
    }

    @Override
    public RealLive createTable(Class<? extends Record> recordClass) {
        createTable(recordClass.getSimpleName(), recordClass);
        return this;
    }

    @Override
    public void createVirtualStream(String name, ReplicatedSet set) {
        throw new RuntimeException("not supported in core implementation");
    }

    @Override
    public void shutDown() {
        final List<Future> futs = tables.values().stream().map((rlTab) -> rlTab.$shutDown()).collect(Collectors.toList());
        CountDownLatch latch = new CountDownLatch(futs.size());
        Actors.yield(futs).then( (r,e) -> latch.countDown() );
        try {
            latch.await(6000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addToSysTable(String name, Class rowClass) {
        TableMeta tableMeta = new TableMeta();
        tableMeta.setName(name);
        final FSTClazzInfo classInfo = conf.getClassInfo(rowClass);

        Description desc = (Description) classInfo.getClazz().getAnnotation(Description.class);
        if ( desc != null ) {
            tableMeta.setDescription(desc.value());
        }

        DisplayName ds = (DisplayName) classInfo.getClazz().getAnnotation(DisplayName.class);
        if ( ds != null ) {
            tableMeta.setDisplayName(ds.value());
        } else {
            tableMeta.setDisplayName(tableMeta.getName());
        }

        final FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();
        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            ColumnMeta cm = new ColumnMeta();
            cm.setName(fi.getField().getName());
            cm.setFieldId(i);

            desc = fi.getField().getAnnotation(Description.class);
            if ( desc != null ) {
                cm.setDescription(desc.value());
            }

            ds = fi.getField().getAnnotation(DisplayName.class);
            if ( ds != null ) {
                cm.setDisplayName(ds.value());
            } else {
                cm.setDisplayName(decamel(cm.getName()));
            }

            ColOrder ord = (ColOrder) fi.getField().getAnnotation(ColOrder.class);
            if ( ord != null ) {
                cm.setOrder(ord.value());
            } else {
                cm.setOrder(cm.getName().hashCode()&0xff+0xffff00);
            }

            Align al = fi.getField().getAnnotation(Align.class);
            if ( al != null ) {
                cm.setAlign(al.value());
            }

            BGColor bg = fi.getField().getAnnotation(BGColor.class);
            if ( bg != null ) {
                cm.setBgColor(bg.value());
            }

            TextColor tc = fi.getField().getAnnotation(TextColor.class);
            if ( tc != null ) {
                cm.setTextColor(tc.value());
            }

            DisplayWidth dw = fi.getField().getAnnotation(DisplayWidth.class);
            if ( dw != null ) {
                cm.setDisplayWidth(dw.value());
            }

            RenderStyle rs = fi.getField().getAnnotation(RenderStyle.class);
            if ( rs != null ) {
                cm.setRenderStyle(rs.value());
            }

            Hidden hid = fi.getField().getAnnotation(Hidden.class);
            if ( hid != null ) {
                cm.setHidden(true);
            } else {
                cm.setHidden(false);
            }

            cm.setJavaType(fi.getType().getSimpleName());

            tableMeta.putColumn(cm.getName(),cm);
        }
        model.putTable(name,tableMeta);

        RLTable<SysTable> sysTables = getTable("SysTable");
        SysTable sysTab = sysTables.createForUpdate("name", true);
        sysTab.setTableName(name);
        sysTab.setDescription(model.getTable(name).getDescription());
        sysTab.setMeta(tableMeta);
        sysTab._setRecordKey(name);
        sysTab.$apply(0);
    }

    private String decamel(String name) {
        String res = "";
        char prevChar = ' ';
        for ( int i = 0; i < name.length(); i++ ) {
            char c = name.charAt(i);
            if ( i == 0 ) {
                prevChar = Character.toUpperCase(c);
                res += prevChar;
                continue;
            }
            if ( Character.isUpperCase(c) && (Character.isLowerCase(prevChar) || !Character.isLetter(prevChar)) ) {
                res += ' ';
                prevChar = c;
                res += c;
            } else {
                prevChar = c;
                res += c;
            }
        }
        return res;
    }

    protected void pureCreateTable(String name, Class<? extends Record> clazz) {
        RLTableImpl table = Actors.AsActor( RLTableImpl.class, CHANGE_Q_SIZE );
        SingleNodeStream stream = Actors.AsActor(SingleNodeStream.class, FILTER_Q_SIZE);
        stream.$init(name,table);
        table.$init(name, this, clazz, stream);
        tables.put( name, table );
    }

    public RLTable getTable(String tableId) {
        return tables.get(tableId);
    }

    @Override @CallerSideMethod
    public RLStream stream(String tableId) {
        return getTable(tableId).stream();
    }

    @Override
    public Metadata getMetadata() {
        return model;
    }

}
