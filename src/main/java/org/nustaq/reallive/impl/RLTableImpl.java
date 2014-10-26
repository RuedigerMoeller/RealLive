package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.annotations.AsCallback;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.*;
import org.nustaq.reallive.impl.storage.BinaryStorage;
import org.nustaq.reallive.impl.storage.FSTBinaryStorage;
import org.nustaq.reallive.sys.annotations.InMem;
import org.nustaq.reallive.sys.annotations.KeyLen;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.FSTClazzInfo;

import java.io.File;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 *
 * todo:
 * add CAS
 * add Client Object to be able to correlate changes and broadcasts
 * add striping
 */
public class RLTableImpl<T extends Record> extends Actor<RLTableImpl<T>> implements RLTable<T> {

    public static int DEFAULT_TABLE_MEM_MB = 1000;

    String tableId;
    IdGenerator<String> idgen;
    BinaryStorage<String,Record> storage;
    Class clazz;

    RealLive realLive; // shared
    private RLStream streamActor;
    private ChangeBroadcastReceiver receiver;
    boolean isShutDown;

    public void $init( String tableId, RealLive realLive, Class<T> clz, SingleNodeStream streamActor ) {
        Thread.currentThread().setName("TableImpl:"+tableId);
        checkThread();
        this.clazz = clz;
        this.tableId = tableId;
        this.realLive = realLive;
        new File(realLive.getDataDirectory()).mkdirs();
        idgen = new StringIdGen(tableId.substring(0,2)+":");
        this.streamActor = streamActor;
        this.receiver = streamActor;
        try {
            FSTBinaryStorage<Record> recordFSTBinaryStorage = new FSTBinaryStorage<>();
            storage = recordFSTBinaryStorage;
            int keyLen = 16;
            KeyLen ks = clz.getAnnotation(KeyLen.class);
            if ( ks != null ) {
                keyLen = Math.max(ks.value(),keyLen);
            }
            InMem inMem = clz.getAnnotation(InMem.class);
            recordFSTBinaryStorage.init(
                realLive.getDataDirectory() + File.separator + tableId+".mmf",
                DEFAULT_TABLE_MEM_MB, // 1 GB init size
                100000, // num records
                keyLen, // keylen
                inMem == null,
                clz
             );
            idgen.setState(storage.getCustomStorage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        delayed( 3000, ()-> $reportStats() );
    }

    @CallerSideMethod
    public Class getRowClazz() {
        return clazz;
    }

    @Override @CallerSideMethod
    public String getTableId() {
        return getActor().tableId;
    }

    @Override @CallerSideMethod
    public RealLive getRealLive() {
        return getActor().realLive;
    }

    @Override @CallerSideMethod
    public T createForAddWith(Class<? extends Record> clazz) {
        try {
            T res = (T) clazz.newInstance();
            res._setTable(self());
            res.setClazzInfo(getClazzInfo(res));
            res._setMode(Record.Mode.ADD);
            return res;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override @CallerSideMethod
    public T createForAdd() {
        return createForAddWith((Class<T>) getActor().clazz);
    }

    @Override @CallerSideMethod
    public T createForAddWithKey(String key) {
        T forAdd = createForAdd();
        forAdd._setRecordKey(key);
        return forAdd;
    }

    @Override @CallerSideMethod
    public T createForUpdateWith(Class<? extends Record> clazz, String key, boolean addIfNotPresent) {
        T res = createForAddWith(clazz);
        res._setMode(addIfNotPresent ? Record.Mode.UPDATE_OR_ADD : Record.Mode.UPDATE);
        res._setRecordKey(key);
        T org = createForAdd();
        org._setRecordKey(key);
        res._setOriginalRecord(org);
        return res;
    }

    @Override @CallerSideMethod
    public T createForUpdate(String key, boolean addIfNotPresent) {
        return createForUpdateWith((Class<T>) getActor().clazz, key, addIfNotPresent);
    }

    @Override @CallerSideMethod
    public void prepareForUpdate(T record) {
        T res = null;
        try {
            res = (T) record.getClass().newInstance();
            res._setTable(self());
            if ( record.getClassInfo() == null )
                record.setClazzInfo(getClazzInfo(record));
            record._setMode(Record.Mode.UPDATE);
            record.copyTo(res);
            record._setOriginalRecord(res);
            record._setTable(self());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // mutation
    //

    @Override
    public Future<String> $addGetId(T object, int originator) {
        if (isShutDown)
            return null;
        checkThread();
        if ( object.getRecordKey() == null ) {
            String nextKey = idgen.nextid();
            object._setRecordKey(nextKey);
        }
        put(object.getRecordKey(), object);
        broadCastAdd(object,originator);
        return new Promise<>(object.getRecordKey());
    }

    @Override
    public void $add(T object, int originator) {
        if (isShutDown)
            return;
        checkThread();
        if ( object.getRecordKey() == null ) {
            String nextKey = idgen.nextid();
            object._setRecordKey(nextKey);
        }
        put(object.getRecordKey(), object);
        broadCastAdd(object,originator);
    }

    @Override
    public void $put(String key, T newRec, int originator) {
        if (isShutDown)
            return;
        checkThread();
        if ( newRec == null ) {
            $remove(key,originator);
            return;
        }
        Record record = storage.get(key);
        newRec._setRecordKey(key);
        if ( record != null ) {
            record.setClazzInfo(getClazzInfo(record));
            newRec.setClazzInfo(getClazzInfo(newRec));
            RecordChange recordChange = newRec.computeDiff(record, true);
            $update(recordChange, false);
        } else {
            storage.put(key, newRec);
            broadCastAdd(newRec, originator);
        }
    }

    private FSTClazzInfo getClazzInfo(Record record) {
        return getRealLive().getConf().getClazzInfo(record.getClass());
    }

    @Override
    public void $update(RecordChange<String,T> change, boolean addIfNotPresent ) {
        if (isShutDown)
            return;
        checkThread();
        T t = get(change.getId());
        if ( t != null ) {
            RecordChange appliedChange = change.apply(t);
            t.incVersion();
            put(t.getRecordKey(), t);
            broadCastUpdate(appliedChange, t);
        } else if (addIfNotPresent) {
            t = createForAdd();
            RecordChange appliedChange = change.apply(t);
            put(t.getRecordKey(), t);
            broadCastAdd(t,change.getOriginator());
        }
    }

    @Override
    public Future<Boolean> $updateCAS(RecordChange<String, T> change, Predicate<T> condition) {
        if (isShutDown)
            return null;
        T t = get(change.getId());
        if ( t == null ) {
            return new Promise<>(false);
        }
        boolean success = condition.test(t);
        if ( success ) {
            $update(change,false);
        }
        return new Promise<>(true);
    }

    @Override
    public void $remove(String key, int originator) {
        if (isShutDown)
            return;
        checkThread();
        Record record = storage.removeAndGet(key);
        if ( record != null )
            broadCastRemove(record,originator);
    }

    @AsCallback
    public void $reportStats() {
        if (isShutDown)
            return;
        checkThread();
        final RLTable st = getRealLive().getTable("SysTable");
        if (st!=null) {
            SysTable sysTable = (SysTable) st.createForUpdate(tableId, true);
            sysTable.setNumElems(storage.size());
            sysTable.setSizeMB(storage.getSizeMB());
            sysTable.setFreeMB(storage.getFreeMB());
            sysTable.$apply(0);
            delayed(3000, () -> $reportStats());
        }
    }

    //
    // mutation
    //
    //////////////////////////////////////////////////////////////////////

    private void broadCastRemove(Record rec, int originator) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewRemove(tableId, rec, originator));
    }

    private void broadCastAdd(T t, int originator) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, t, originator));
    }

    private void broadCastUpdate(RecordChange appliedChange, T newRecord) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewUpdate(tableId, newRecord, appliedChange));
    }


    @Override
    public Future<String> $nextKey() {
        return new Promise<>(idgen.nextid());
    }

    @Override
    public Future<T> $get(String key) {
        return new Promise<>(get(key));
    }


    @Override
    public Future $sync() {
        return new Promise("void");
    }

    @Override
    public Future $shutDown() {
        isShutDown = true;
        storage.close();
        return new Promise("void");
    }


    public void $filter(Predicate<T> doProcess, Predicate<T> terminate, Callback<T> resultReceiver) {
        if (isShutDown)
            return;
        checkThread();
        Iterator<Record> vals = storage.values();
        while( vals.hasNext() ) {
            try {
                T t = (T) vals.next();
                t._setTable(self());
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.receive(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.receive(null, e);
            }
        }
        resultReceiver.receive(null, END);
    }

    public void $filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver) {
        if (isShutDown)
            return;
        Iterator<ByteSource> entries = storage.binaryValues();

        while( entries.hasNext() ) {
            try {
                ByteSource t = entries.next();
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.receive(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.receive(null, e);
            }
        }
        resultReceiver.receive(null, END);
    }

    private T get(String key) {
        if (isShutDown)
            return null;
        T res = (T) storage.get(key);
        if ( res == null )
            return null;
        res._setTable(self());
        return res;
    }

    private void put(String key, T object) {
        if (isShutDown)
            return;
        storage.put(key, object);
    }

    @CallerSideMethod
    public RLStream<T> stream() {
        if (isShutDown)
            return null;
        if ( isProxy() )
            return getActor().stream();
        return streamActor;
    }
}
