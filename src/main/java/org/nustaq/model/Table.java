package org.nustaq.model;

import net.openhft.lang.io.NativeBytes;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.serialization.FSTObjectInput;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public interface Table<T extends Record> {
    public final String FIN = "FIN";

    // sync methods
    public String getTableId();
    Schema getSchema();

    public T createForAdd();
    public T getForUpdate(String key, boolean addIfNotPresent);
    public void prepareForUpdate(T original);

    // async methods
    public Future<String> $add(T object);
    public Future<String> $nextKey();
    public void $remove(String key);
    public Future<T> $get(String key);
    public void $update(Change<String,T> change, boolean addIfNotPresent );
    public void $filter(Predicate<T> matches, Predicate<T> terminateQuery, Callback<T> resultReceiver);
    public void $filterBinary(Predicate<NativeBytes> doProcess, Predicate<NativeBytes> terminate, Callback resultReceiver);
    public Future $sync();
}
