package org.nustaq.model;

import org.nustaq.heapoff.bytez.ByteSource;

/**
 * Created by ruedi on 25.06.14.
 */
public interface EncodedRecordEntry<K,V> {
    ByteSource getValueBytes();
    ByteSource getBytes();
    K getKey();
    V getValue();
}
