package org.nustaq.storage;

import net.openhft.collections.*;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.io.NativeBytes;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.io.serialization.BytesMarshallerFactory;
import net.openhft.lang.io.serialization.impl.NoMarshaller;
import net.openhft.lang.io.serialization.impl.ObjectStreamFactory;
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
    public static BytesMarshallerFactory myFac;
    static {
//        conf.setShareReferences(false);
        myFac = new BytesMarshallerFactory() {

            @Override
            public BytesMarshaller acquireMarshaller(@NotNull Class eClass, boolean create) {
                return NoMarshaller.DISABLED;
            }

            @Override
            public BytesMarshaller getMarshaller(byte code) {
                return NoMarshaller.DISABLED;
            }

            @Override
            public <E> void addMarshaller(Class<E> eClass, BytesMarshaller<E> marshaller) {

            }

            @Override
            public void writeSerializable(Bytes bytes, Object obj) {
                try {
                    ObjectOutput objectOutput = conf.getObjectOutput(bytes.outputStream());
                    objectOutput.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public Object readSerializable(Bytes bytes) {
                try {
                    return conf.getObjectInput(bytes.inputStream()).readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
    }
    SharedHashMap map;

    public HugeCollectionsBinaryStorage(String finam) throws IOException {
//        map = new SharedHashMapBuilder().entrySize(32).actualSegments(1).actualEntriesPerSegment(50*1000*1000).create(new File(finam), String.class, byte[].class);
        map = new SharedHashMapBuilder().bytesMarshallerFactory(myFac).entrySize(64).minSegments(10000).actualEntriesPerSegment(5 * 1000).create(new File(finam), String.class, Record.class);
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
