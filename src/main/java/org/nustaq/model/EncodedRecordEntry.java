package org.nustaq.model;

import net.openhft.lang.io.NativeBytes;

/**
 * Created by ruedi on 25.06.14.
 */
public interface EncodedRecordEntry<K,V> {
    NativeBytes getValueBytes();
    NativeBytes getBytes();
    K getKey();
    V getValue();
}
