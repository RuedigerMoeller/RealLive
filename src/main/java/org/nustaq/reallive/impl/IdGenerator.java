package org.nustaq.reallive.impl;

import org.nustaq.offheap.bytez.Bytez;

/**
 * Created by ruedi on 21.06.14.
 */
public interface IdGenerator<K> {
    K nextid();

    /**
     * return length used.
     * @param bytes
     * @return
     */
    int setState( Bytez bytes );
}
