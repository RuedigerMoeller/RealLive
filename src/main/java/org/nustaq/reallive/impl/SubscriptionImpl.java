package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
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

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        cb.onChangeReceived(changeBC);
    }

}
