package org.nustaq.reallive;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.impl.SubscriptionImpl;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public interface RLStream<T extends Record> {

    public void each(ChangeBroadcastReceiver<T> resultReceiver);
    public void filter(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public void filterUntil(Predicate<T> matches, Predicate<T> terminateQuery, ChangeBroadcastReceiver<T> resultReceiver);
    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback<ByteSource> resultReceiver);

    public Subscription<T> subscribeKey(String key, ChangeBroadcastReceiver<T> resultReceiver);
    public Subscription<T> subscribe(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public Subscription<T> listen(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver);
    public void unsubscribe( Subscription<T> subs );

}
