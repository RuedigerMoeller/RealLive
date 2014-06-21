package org.nustaq.model;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public interface Table<T extends Record> extends Heap<String,T> {
    public String getTableId();
    public void $update(Change<String,T> change);
    public Future $sync();

    public void $filter(Predicate<T> doProcess, Predicate<T> terminate, Callback<T> resultReceiver);
}
