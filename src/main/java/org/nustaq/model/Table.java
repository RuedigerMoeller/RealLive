package org.nustaq.model;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public interface Table<T extends Record> {
    public final String FIN = "FIN";

    // sync methods
    public String getTableId();
    Schema getSchema();

    public T createRecordForAdd();
    public T createRecordForUpdate(String key, boolean addIfNotPresent);
    public void prepareRecordForUpdate(T original);

    // async methods
    public Future<String> $add(T object);
    public Future<String> $nextKey();
    public void $remove(String key);
    public Future<T> $get(String key);
    public void $update(Change<String,T> change, boolean addIfNotPresent );
    public void $filter(Predicate<T> matches, Predicate<T> terminateQuery, Callback<T> resultReceiver);
    public void $filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver);
    public Future $sync();
}
