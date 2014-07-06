package org.nustaq.reallive;

import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public class Subscription<T extends Record> implements ChangeBroadcastReceiver<T> {

    Predicate<T> TRUE = (x) -> true;

    ChangeBroadcastReceiver<T> cb;
    Predicate<T> filter;

    public boolean __matched; // for internal use

    public Subscription(ChangeBroadcastReceiver<T> cb, Predicate<T> filter) {
        this.cb = cb;
        this.filter = filter;
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
