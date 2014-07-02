package org.nustaq.storage;

import org.nustaq.heapoff.FSTAsciiStringOffheapMap;
import org.nustaq.heapoff.FSTBinaryOffheapMap;
import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.serialization.FSTConfiguration;

import java.util.Iterator;

/**
 * Created by ruedi on 02.07.14.
 */
public class FSTBinaryStorage<V> implements BinaryStorage<String,V> {

    FSTAsciiStringOffheapMap<V> store;
    FSTConfiguration conf;

    public void init(Class ... toReg) {
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerClass(toReg);
        store = new FSTAsciiStringOffheapMap<>(20, FSTBinaryOffheapMap.GB*4,100000,conf);
    }

    @Override
    public void put(String key, V toWrite) {
        store.put(key,toWrite);
    }

    @Override
    public V get(String key) {
        return store.get(key);
    }

    @Override
    public Iterator<ByteSource> binaryValues() {
        return store.binaryValues();
    }

    @Override
    public Iterator<V> values() {
        return store.values();
    }

    @Override
    public V decodeValue(ByteSource value) {
        return null;//store.de;
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }
}
