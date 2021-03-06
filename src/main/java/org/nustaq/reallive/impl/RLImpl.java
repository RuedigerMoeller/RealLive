package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    }

    public RLImpl initSync() {
        CountDownLatch latch = new CountDownLatch(1);
        $init().onResult( r -> latch.countDown() );
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public IPromise $init() {
        return initSystemTables();
    }

    public RLImpl(String dataDir) {
        model = new Metadata();
        dataDirectory = dataDir;
    }

    protected IPromise initSystemTables() {
        Object futs = Arrays.stream(new Class[]{SysTable.class}).map(
                (clz) -> createTable(clz.getSimpleName(), clz)
        ).collect(Collectors.toList());
        return Actors.all((List<IPromise<Object>>) futs);
    }

    public IPromise createTable(String name, Class<? extends Record> clazz) {
        if (tables.get(name) != null ) {
            throw new RuntimeException("table already created");
        }
        if ( clazz.getAnnotation(Virtual.class) == null ) {
            pureCreateTable(name, clazz).await();
            addToSysTable(name, clazz).await();
            return new Promise("done");
        }
        return addToSysTable(name, clazz);
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
        List<IPromise<Object>> futs = new ArrayList<>();
        tables.values().stream().map( rlTab -> {
            IPromise iPromise = rlTab.$shutDown();
            futs.add(iPromise);
            return iPromise;
        });
        CountDownLatch latch = new CountDownLatch(futs.size());
        Actors.all(futs).then((r, e) -> latch.countDown());
        try {
            latch.await(6000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // specialized to also include transient fields for virtual recs
    final List<Field> getTransientFields(Class c, List<Field> res) {
        if ( c == Record.class )
            return res;
        if (res == null) {
            res = new ArrayList<Field>();
        }
        if (c == null) {
            return res;
        }
        List<Field> c1 = Arrays.asList(c.getDeclaredFields());
        Collections.reverse(c1);
        for (int i = 0; i < c1.size(); i++) {
            Field field = c1.get(i);
            if ( Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) ) {
                res.add(field);
            }
        }
        return getTransientFields(c.getSuperclass(), res);
    }

    private IPromise addToSysTable(String name, Class rowClass) {
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
            Field field = fi.getField();
            processFieldAnnotations(i, cm, field);
            tableMeta.putColumn(cm.getName(),cm);
        }
        List<Field> transientFields = getTransientFields(classInfo.getClazz(), null);
        int fieldOffset = fieldInfo.length;
        for (int i = 0; i < transientFields.size(); i++) {
            Field field = transientFields.get(i);
            ColumnMeta cm = new ColumnMeta();
            processFieldAnnotations(i+fieldOffset, cm, field);
            tableMeta.putColumn(cm.getName(),cm);
        }

        model.putTable(name,tableMeta);

        RLTable<SysTable> sysTables = getTable("SysTable");
        SysTable sysTab = sysTables.createForUpdate("name", true);
        sysTab.setTableName(name);
        sysTab.setDescription(model.getTable(name).getDescription());
        sysTab.setMeta(tableMeta);
        sysTab._setRecordKey(name);
        return sysTab.$apply(0);
    }

    private void processFieldAnnotations(int i, ColumnMeta cm, Field field) {
        Description desc;
        DisplayName ds;
        cm.setName(field.getName());
        cm.setFieldId(i);

        desc = field.getAnnotation(Description.class);
        if ( desc != null ) {
            cm.setDescription(desc.value());
        }

        ds = field.getAnnotation(DisplayName.class);
        if ( ds != null ) {
            cm.setDisplayName(ds.value());
        } else {
            cm.setDisplayName(decamel(cm.getName()));
        }

        ColOrder ord = field.getAnnotation(ColOrder.class);
        if ( ord != null ) {
            cm.setOrder(ord.value());
        } else {
            cm.setOrder(cm.getName().hashCode()&0xff+0xffff00);
        }

        Align al = field.getAnnotation(Align.class);
        if ( al != null ) {
            cm.setAlign(al.value());
        }

        BGColor bg = field.getAnnotation(BGColor.class);
        if ( bg != null ) {
            cm.setBgColor(bg.value());
        }

        TextColor tc = field.getAnnotation(TextColor.class);
        if ( tc != null ) {
            cm.setTextColor(tc.value());
        }

        DisplayWidth dw = field.getAnnotation(DisplayWidth.class);
        if ( dw != null ) {
            cm.setDisplayWidth(dw.value());
        }

        RenderStyle rs = field.getAnnotation(RenderStyle.class);
        if ( rs != null ) {
            cm.setRenderStyle(rs.value());
        }

        Hidden hid = field.getAnnotation(Hidden.class);
        if ( hid != null ) {
            cm.setHidden(true);
        } else {
            cm.setHidden(false);
        }

        cm.setJavaType(field.getType().getSimpleName());
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

    protected IPromise pureCreateTable(String name, Class<? extends Record> clazz) {
        Promise p = new Promise();
        RLTableImpl table = Actors.AsActor( RLTableImpl.class, CHANGE_Q_SIZE );
        SingleNodeStream stream = Actors.AsActor(SingleNodeStream.class, FILTER_Q_SIZE);
        stream.$init(name,table);
        table.$init(name, this, clazz, stream);
        table.$sync().then( p );
        tables.put( name, table );
        return p;
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
