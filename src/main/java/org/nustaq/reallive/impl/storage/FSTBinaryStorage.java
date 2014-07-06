package org.nustaq.reallive.impl.storage;

import org.nustaq.heapoff.FSTAsciiStringOffheapMap;
import org.nustaq.heapoff.FSTBinaryOffheapMap;
import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.heapoff.bytez.Bytez;
import org.nustaq.heapoff.bytez.bytesource.ByteArrayByteSource;
import org.nustaq.heapoff.bytez.bytesource.BytezByteSource;
import org.nustaq.serialization.FSTConfiguration;

import java.util.Iterator;

/**
 * Created by ruedi on 02.07.14.
 */
public class FSTBinaryStorage<V> implements BinaryStorage<String,V> {

    FSTAsciiStringOffheapMap<V> store;
    FSTConfiguration conf;

    public FSTBinaryStorage() {
    }

    public void init(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen, Class ... toReg) throws Exception {
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerClass(toReg);
        store = new FSTAsciiStringOffheapMap<>(tableFile, keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords,conf);
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
        return store.decodeValue((org.nustaq.heapoff.bytez.bytesource.BytezByteSource) value);
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    @Override
    public V removeAndGet(String key) {
        V res = store.get(key);
        if ( res != null )
            store.remove(key);
        return res;
    }

    @Override
    public Bytez getCustomStorage() {
        return store.getCustomFileHeader();
    }

}
