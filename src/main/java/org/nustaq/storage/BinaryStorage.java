package org.nustaq.storage;

import java.util.Iterator;

/**
 * Created by ruedi on 21.06.14.
 */
public interface BinaryStorage<K,V> {

    public void put(K key, byte value[],  int off, int len );
    public byte[] get(K key);
    public Iterator<K> keys();
    public Iterator<V> values();

    public void remove(K key);
}
