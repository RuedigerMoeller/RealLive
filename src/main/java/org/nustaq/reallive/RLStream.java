package org.nustaq.reallive;

import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public interface RLStream<T extends Record> {

    public void forEach(ChangeBroadcastReceiver<T> resultReceiver);
    public void filter(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);

    /**
     * filter until terminate query returns true
     * @param matches
     * @param terminateQuery
     * @param resultReceiver
     */
    public void filterUntil(Predicate<T> matches, Predicate<T> terminateQuery, ChangeBroadcastReceiver<T> resultReceiver);
    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback<ByteSource> resultReceiver);

    public Subscription<T> subscribeKey(String key, ChangeBroadcastReceiver<T> resultReceiver);
    public Subscription<T> subscribe(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public Subscription<T> listen(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public void unsubscribe( Subscription<T> subs );

}
