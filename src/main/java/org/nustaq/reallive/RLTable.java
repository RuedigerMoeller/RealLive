package org.nustaq.reallive;

import org.nustaq.kontraktor.IPromise;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public interface RLTable<T extends Record> {
    public final String END = "FIN";

    ////////////////////////////////////////////////////////////////////////////////////////
    // sync methods
    //

    public String getTableId();
    RealLive getRealLive();
    public Class getRowClazz();
    public T createForAddWith(Class<? extends Record> clazz);
    public T createForAdd();
    public T createForAddWithKey(String key);
    public T createForUpdateWith(Class<? extends Record> clazz, String key, boolean addIfNotPresent);
    public T createForUpdate(String key, boolean addIfNotPresent);
    public void prepareForUpdate(T original);
    public RLStream<T> stream();

    ////////////////////////////////////////////////////////////////////////////////////////
    // async methods
    //

    /**
     * add the given record and assign it a new unique key. The new record key is returned with the resulting future.
     * @param object
     * @return
     */
    public IPromise<String> $addGetId(T object, int originator);

    /**
     * add the given record and assign it a new unique key. The new key is not returned.
     * @param object
     * @return
     */
    public void $add(T object, int originator);


    /**
     * puts the given object at key. Note its faster to use 'createForUpdate(.., true)' as this will
     * submit differences to default values only and work exactly like a put operation.
     *
     * @param key
     * @param object
     */
    public void $put(String key, T object, int originator);

    public void $putIfAbsent(String key, T object, int originator);

    public IPromise<T> $putIfAbsentWithResult(String key, T object, int originator);

    /**
     * create a new unique record key and return a future to it
     * @return
     */
    public IPromise<String> $nextKey();

    /**
     * remove the record associated with given key
     * @param key
     */
    public void $remove(String key, int originator);

    /**
     * get the record associated with the given key
     * @param key
     * @return
     */
    public IPromise<T> $get(String key);

    /**
     * update a given record. Usually done calling record.apply().
     * @param change
     * @param addIfNotPresent
     */
    public void $update(RecordChange<String,T> change, boolean addIfNotPresent );

    /**
     * update a given record. Usually done calling record.apply().
     * @param change
     */
    public IPromise<Boolean> $updateCAS(RecordChange<String,T> change, Predicate<T> condition);

    /**
     * when the resulting future is triggered, all operations that have been sent before are guaranteed to
     * have been finished.
     *
     * @return
     */
    public IPromise $sync();

    public IPromise $shutDown();

    public IPromise<Integer> $getKeyLen();
}
