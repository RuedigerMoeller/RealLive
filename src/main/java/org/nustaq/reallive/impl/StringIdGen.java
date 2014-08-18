package org.nustaq.reallive.impl;

import org.nustaq.offheap.bytez.Bytez;

/**
 * Threadsafe String id generator.
 */
public class StringIdGen implements IdGenerator<String> {

    String prefix;
    Bytez state;

    public StringIdGen(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String nextid() {
        long count = state.getLong(0);
        while( ! state.compareAndSwapLong(0, count, count +1) ) {
            count = state.getLong(0);
        }
        return prefix+Long.toHexString(count);
    }

    @Override
    public int setState(Bytez bytes) {
        long aLong = bytes.getLong(0);
        if ( aLong == 0 ) // new file
            bytes.putLong(0,1);
        state = bytes;
        return 8;
    }
}
