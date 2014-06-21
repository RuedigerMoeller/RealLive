package org.nustaq.impl;

import org.nustaq.model.IdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Threadsafe String id generator.
 */
public class StringIdGen implements IdGenerator<String> {

    String prefix;
    AtomicLong count = new AtomicLong(1);

    public StringIdGen(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String nextid() {
        return prefix+Long.toHexString(count.incrementAndGet());
    }
}
