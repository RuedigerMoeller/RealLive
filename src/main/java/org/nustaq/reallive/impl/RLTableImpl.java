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

    String tableId;
    IdGenerator<String> idgen;
    BinaryStorage<String,Record> storage;
    volatile Class clazz;

    Schema schema; // shared
    private RLStream streamActor;
    private ChangeBroadcastReceiver receiver;

    public void $init( String tableId, Schema schema, Class<T> clz, SingleNodeStream streamActor ) {
        this.clazz = clz;
        this.tableId = tableId;
        this.schema = schema;
        new File(schema.getDataDirectory()).mkdirs();
        idgen = new StringIdGen(tableId+":");
        this.streamActor = streamActor;
        this.receiver = streamActor;
        try {
            FSTBinaryStorage<Record> recordFSTBinaryStorage = new FSTBinaryStorage<>();
            storage = recordFSTBinaryStorage;
            recordFSTBinaryStorage.init(
                schema.getDataDirectory() + File.separator + tableId+".mmf",
                4000, // 4 GB init size
                100000, // num records
                20, // keylen
                clz);
            idgen.setState(storage.getCustomStorage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override @CallerSideMethod
    public String getTableId() {
        return getActor().tableId;
    }

    @Override @CallerSideMethod
    public Schema getSchema() {
        return getActor().schema;
    }

    @Override @CallerSideMethod
    public T createRecordForAdd() {
        try {
            T res = (T) getActor().clazz.newInstance();
            res._setTable(this);
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
    public T createRecordForUpdate(String key, boolean addIfNotPresent) {
        T res = createRecordForAdd();
        res._setMode(addIfNotPresent ? Record.Mode.UPDATE_OR_ADD : Record.Mode.UPDATE);
        res._setId(key);
        T org = createRecordForAdd();
        org._setId(key);
        res._setOriginalRecord(org);
        return res;
    }

    @Override @CallerSideMethod
    public void prepareRecordForUpdate(T record) {
        T res = null;
        try {
            res = (T) record.getClass().newInstance();
            res._setTable(this);
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
    public Future<String> $addGetId(T object) {
        String nextKey = idgen.nextid();
        object._setId(nextKey);
        put(nextKey, object);
        broadCastAdd(object);
        return new Promise<>(nextKey);
    }

    @Override
    public void $add(T object) {
        String nextKey = idgen.nextid();
        object._setId(nextKey);
        put(nextKey, object);
        broadCastAdd(object);
    }

    @Override
    public void $update(RecordChange<String,T> change, boolean addIfNotPresent ) {
        T t = get(change.getId());
        if ( t != null ) {
            RecordChange appliedChange = change.apply(t);
            t.incVersion();
            put(t.getId(), t);
            broadCastUpdate(appliedChange, t);
        } else if (addIfNotPresent) {
            t = createRecordForAdd();
            RecordChange appliedChange = change.apply(t);
            put(t.getId(), t);
            broadCastAdd(t);
        }
    }

    @Override
    public void $remove(String key) {
        Record record = storage.removeAndGet(key);
        broadCastRemove(record);
    }

    //
    // mutation
    //
    //////////////////////////////////////////////////////////////////////

    private void broadCastRemove(Record rec) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewRemove(tableId, rec));
    }

    private void broadCastAdd(T t) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, t));
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
        Iterator<Record> vals = storage.values();
        while( vals.hasNext() ) {
            try {
                T t = (T) vals.next();
                t._setTable(this);
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
        res._setTable(this);
        return res;
    }

    private void put(String key, T object) {
        storage.put(key, object);
    }

    @CallerSideMethod
    public RLStream<T> getStream() {
        if ( isProxy() )
            return getActor().getStream();
        return streamActor;
    }
}
