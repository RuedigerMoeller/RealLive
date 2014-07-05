package org.nustaq.model;

import org.nustaq.kontraktor.Callback;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public class Subscription<T extends Record> implements ChangeBroadcastReceiver<T> {
    Callback cb;
    Predicate<T> filter;

    public boolean __matched; // for internal use

    public Subscription(Callback<ChangeBroadcast<T>> cb, Predicate<T> filter) {
        this.cb = cb;
        this.filter = filter;
    }

    public Callback getCallback() {
        return cb;
    }

    public Predicate<T> getFilter() {
        return filter;
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        cb.receiveResult(changeBC, null);
    }

}
