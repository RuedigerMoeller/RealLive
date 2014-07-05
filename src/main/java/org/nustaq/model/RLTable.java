package org.nustaq.model;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.impl.RLTableImpl;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public interface RLTable<T extends Record> {
    public final String FIN = "FIN";

    // sync methods
    public String getTableId();
    Schema getSchema();

    public T createRecordForAdd();
    public T createRecordForUpdate(String key, boolean addIfNotPresent);
    public void prepareRecordForUpdate(T original);

    // async methods

    /**
     * add the given record and assign it a new unique id. The new record id is returned with the resulting future.
     * @param object
     * @return
     */
    public Future<String> $addGetId(T object);

    /**
     * add the given record and assign it a new unique id. The new id is not returned.
     * @param object
     * @return
     */
    public void $add(T object);

    /**
     * create a new unique record id and return a future to it
     * @return
     */
    public Future<String> $nextKey();

    /**
     * remove the record associated with given key
     * @param key
     */
    public void $remove(String key);

    /**
     * get the record associated with the given key
     * @param key
     * @return
     */
    public Future<T> $get(String key);

    /**
     * update a given record. Usually done calling record.apply().
     * @param change
     * @param addIfNotPresent
     */
    public void $update(Change<String,T> change, boolean addIfNotPresent );

    /**
     * when the resulting future is triggered, all operations that have been sent before are guaranteed to
     * have been finished.
     *
     * @return
     */
    public Future $sync();

    public RLStream<T> getStream();
}
