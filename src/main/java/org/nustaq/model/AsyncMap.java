package org.nustaq.model;

import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.Future;
import de.ruedigermoeller.kontraktor.Promise;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 29.05.14.
 */
public interface AsyncMap<K,T> {

    public Future<K> add(T object);
    public K nextKey();
    public void remove( K key );
    public Future<T> get( K key );
    public void iterate( Callback<Assoc<K,T>> cb );

}
