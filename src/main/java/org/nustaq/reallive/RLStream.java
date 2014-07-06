package org.nustaq.reallive;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public interface RLStream<T extends Record> extends ChangeBroadcastReceiver<T>{

    public void $each(Callback<T> resultReceiver);
    public void $filter(Predicate<T> matches, Callback<T> resultReceiver);
    public void $filterUntil(Predicate<T> matches, Predicate<T> terminateQuery, Callback<T> resultReceiver);
    public void $filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver);

    public Subscription subscribe( Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public Subscription listen( Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public void unsubscribe( Subscription subs );

}
