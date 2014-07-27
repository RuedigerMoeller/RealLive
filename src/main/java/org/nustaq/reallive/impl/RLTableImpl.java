package org.nustaq.reallive.impl;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.*;
import org.nustaq.reallive.impl.storage.BinaryStorage;
import org.nustaq.reallive.impl.storage.FSTBinaryStorage;
import org.nustaq.reallive.sys.annotations.KeyLen;
import org.nustaq.reallive.sys.tables.SysTable;

import java.io.File;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 *
 * add CAS
 * add Client Object to be able to correlate changes and broadcasts
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
            recordFSTBinaryStorage.init(
                realLive.getDataDirectory() + File.separator + tableId+".mmf",
                DEFAULT_TABLE_MEM_MB, // 1 GB init size
                100000, // num records
                keyLen, // keylen
                clz);
            idgen.setState(storage.getCustomStorage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        delayed( 3000, ()-> self().$reportStats() );
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
    public T createForUpdateWith(Class<? extends Record> clazz, String key, boolean addIfNotPresent) {
        T res = createForAddWith(clazz);
        res._setMode(addIfNotPresent ? Record.Mode.UPDATE_OR_ADD : Record.Mode.UPDATE);
        res._setId(key);
        T org = createForAdd();
        org._setId(key);
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
            record._setMode(Record.Mode.UPDATE);
            record.copyTo(res);
            record._setOriginalRecord(res);
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
        checkThread();
        String nextKey = idgen.nextid();
        object._setId(nextKey);
        put(nextKey, object);
        broadCastAdd(object,originator);
        return new Promise<>(nextKey);
    }

    @Override
    public void $add(T object, int originator) {
        checkThread();
        String nextKey = idgen.nextid();
        object._setId(nextKey);
        put(nextKey, object);
        broadCastAdd(object,originator);
    }

    @Override
    public void $put(String key, T object, int originator) {
        checkThread();
        object._setId(key);
        if ( storage.contains(key) ) {
            broadCastRemove(storage.get(key),originator);
        }
        storage.put(key,object);
        broadCastAdd(object,originator);
    }

    @Override
    public void $update(RecordChange<String,T> change, boolean addIfNotPresent ) {
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
        checkThread();
        Record record = storage.removeAndGet(key);
        if ( record != null )
            broadCastRemove(record,originator);
    }

    public void $reportStats() {
        checkThread();
        SysTable sysTable = (SysTable) getRealLive().getTable("SysTable").createForUpdate(tableId, true);
        sysTable.setNumElems(storage.size());
        sysTable.setSizeMB(storage.getSizeMB());
        sysTable.setFreeMB(storage.getFreeMB());
        sysTable.$apply(0);
        delayed( 3000, () -> self().$reportStats() );
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

    private void broadCastUpdate(RecordChange appliedChange, T t) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewUpdate(tableId, t, appliedChange));
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


    public void $filter(Predicate<T> doProcess, Predicate<T> terminate, Callback<T> resultReceiver) {
        checkThread();
        Iterator<Record> vals = storage.values();
        while( vals.hasNext() ) {
            try {
                T t = (T) vals.next();
                t._setTable(self());
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.receiveResult(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.receiveResult(null,e);
            }
        }
        resultReceiver.receiveResult(null,FIN);
    }

    public void $filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver) {
        Iterator<ByteSource> entries = storage.binaryValues();

        while( entries.hasNext() ) {
            try {
                ByteSource t = entries.next();
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.receiveResult(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.receiveResult(null,e);
            }
        }
        resultReceiver.receiveResult(null,FIN);
    }

    private T get(String key) {
        T res = (T) storage.get(key);
        if ( res == null )
            return null;
        res._setTable(self());
        return res;
    }

    private void put(String key, T object) {
        storage.put(key, object);
    }

    @CallerSideMethod
    public RLStream<T> stream() {
        if ( isProxy() )
            return getActor().stream();
        return streamActor;
    }
}