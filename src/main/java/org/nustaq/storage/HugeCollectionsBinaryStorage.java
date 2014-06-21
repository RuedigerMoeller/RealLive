package org.nustaq.storage;

import net.openhft.collections.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ruedi on 21.06.14.
 */
public class HugeCollectionsBinaryStorage implements BinaryStorage<String,byte[]> {

    SharedHashMap map;

    public HugeCollectionsBinaryStorage(String finam) throws IOException {
        map = new SharedHashMapBuilder().entrySize(32).actualSegments(1).actualEntriesPerSegment(50*1000*1000).create(new File(finam), String.class, byte[].class);
    }

    @Override
    public void put(String key, byte[] value, int off, int len) {
        if ( off == 0 && len == value.length ) {
            map.put(key, value);
        } else {
            byte tmp[] = new byte[len];
            System.arraycopy(value,off,tmp,0,len);
            map.put(key,tmp);
        }
    }

    @Override
    public byte[] get(String key) {
        return (byte[]) map.get(key);
    }

    @Override
    public Iterator<String> keys() {
        return map.keySet().iterator();
    }

    @Override
    public Iterator<byte[]> values() {
        return map.entrySet().iterator();
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

}
