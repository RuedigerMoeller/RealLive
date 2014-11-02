package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.StoppedActorTargetedException;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.ChangeBroadcastReceiver;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.Subscription;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public class SubscriptionImpl<T extends Record> implements ChangeBroadcastReceiver<T>,Subscription<T> {

    public static class KeyPredicate implements Predicate<Record> {

        String key;

        public KeyPredicate(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean test(Record o) {
            return o.getRecordKey().equals(key);
        }

    }

    Predicate<T> TRUE = (x) -> true;

    ChangeBroadcastReceiver<T> cb;
    Predicate<T> filter;
    String tableKey;

    public boolean __matched; // for internal use

    SubscriptionImpl(String tab, Predicate<T> filter, ChangeBroadcastReceiver<T> cb) {
        this.cb = cb;
        this.filter = filter;
        this.tableKey = tab;
    }

    public String getTableKey() {
        return tableKey;
    }

    public ChangeBroadcastReceiver<T> getChangeReceiver() {
        return cb;
    }

    public Predicate<T> getFilter() {
        if ( filter == null )
            return TRUE;
        return filter;
    }

    Thread _t; //
    protected final void checkThread() {
        if (_t==null) {
            _t = Thread.currentThread();
        } else {
            if ( _t != Thread.currentThread() ) {
                throw new RuntimeException("Wrong Thread");
            }
        }
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        checkThread();
        cb.onChangeReceived(changeBC);
    }

}
