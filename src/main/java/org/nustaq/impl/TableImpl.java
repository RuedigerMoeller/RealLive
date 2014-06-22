package org.nustaq.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.model.*;
import org.nustaq.storage.BinaryStorage;
import org.nustaq.storage.HugeCollectionsBinaryStorage;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public class TableImpl<T extends Record> extends Actor<TableImpl<T>> implements Table<T> {

    String tableId;
    IdGenerator<String> idgen;
    BinaryStorage<String,Record> storage;

    Schema schema; // shared

    public void $init(String tableId, Schema schema ) {
        this.tableId = tableId;
        this.schema = schema;
        idgen = new StringIdGen(tableId+":");
        try {
            storage = new HugeCollectionsBinaryStorage("/tmp/storage.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override @CallerSideMethod
    public String getTableId() {
        return tableId;
    }

    @Override
    public Future<String> $add(T object) {
        String nextKey = idgen.nextid();
        object._setId(nextKey);
        put(nextKey, object);
        return new Promise<>(nextKey);
    }

    @Override
    public Future<String> $nextKey() {
        return new Promise<>(idgen.nextid());
    }

    @Override
    public void $remove(String key) {
        storage.remove(key);
    }

    @Override
    public Future<T> $get(String key) {
        return new Promise<>(get(key));
    }

    @Override
    public void $update(Change<String,T> change) {
        T t = get(change.getId());
        if ( t != null ) {
            RecordChange appliedChange = change.apply(t);
            put(t.getId(), t);
//            broadCast(appliedChange);
        }
    }

    @Override
    public Future $sync() {
        return new Promise("void");
    }

    @Override
    public void $filter(Predicate<T> doProcess, Predicate<T> terminate, Callback<T> resultReceiver) {
        Iterator<Record> vals = storage.values();
        while( vals.hasNext() ) {
            try {
                T t = (T) vals.next();
                t._setSchema(schema);
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
        res._setSchema(schema);
        return res;
    }

    private void put(String key, T object) {
        storage.put(key, object);
    }

}
