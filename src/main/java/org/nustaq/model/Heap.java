package org.nustaq.model;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

/**
 * Created by ruedi on 01.06.14.
 */
public interface Heap<K,T> {

    public Future<K> $add(T object);
    public Future<K> $nextKey();
    public void $remove(K key);

    public Future<T> $get(K key);

}
