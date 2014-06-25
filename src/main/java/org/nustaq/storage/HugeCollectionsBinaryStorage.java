package org.nustaq.storage;

import net.openhft.collections.*;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.NativeBytes;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.io.serialization.BytesMarshallerFactory;
import net.openhft.lang.io.serialization.JDKObjectSerializer;
import net.openhft.lang.io.serialization.ObjectSerializer;
import net.openhft.lang.io.serialization.impl.NoMarshaller;
import net.openhft.lang.io.serialization.impl.VanillaBytesMarshallerFactory;
import net.openhft.lang.model.constraints.NotNull;
import org.nustaq.model.Record;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ruedi on 21.06.14.
 */
public class HugeCollectionsBinaryStorage implements BinaryStorage<String,Record> {

    public static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    public static ObjectSerializer myFac;
    static {
//        conf.setShareReferences(false);
        myFac = new JDKObjectSerializer() {
            @Override
            public void writeSerializable(Bytes bytes, Object object) throws IOException {
                FSTObjectOutput objectOutput = conf.getObjectOutput(bytes.outputStream());
                objectOutput.writeObject(object);
                objectOutput.flush();
            }

            @Override
            public Object readSerializable(Bytes bytes) throws IOException, ClassNotFoundException {
                return conf.getObjectInput(bytes.inputStream()).readObject();
            }
        };
    }
    SharedHashMap map;

    public HugeCollectionsBinaryStorage(String finam) throws IOException {
        JDKObjectSerializer.INSTANCE = (JDKObjectSerializer) myFac;
        map = new SharedHashMapBuilder().entrySize(64).minSegments(1500).actualEntriesPerSegment(10 * 1000).create(new File(finam), String.class, Record.class);
//        HugeConfig config = HugeConfig.DEFAULT.clone()
//            .setSegments(128)
//            .setSmallEntrySize(128)
//            .setCapacity(10*1000*1000);
//        map = new HugeHashMap<String, byte[]>( config, String.class, byte[].class );
    }

    public ByteEntryIterator entryIterator() {
        return map.getByteEntryIterator();
    }
    @Override
    public void put(String key, Record value) {
        map.put(key, value);
    }

    @Override
    public Record get(String key) {
        return (Record) map.get(key);
    }

    @Override
    public Iterator<String> keys() {
        return map.keySet().iterator();
    }

    @Override
    public Iterator<Record> values() {
        return map.values().iterator();
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

}
