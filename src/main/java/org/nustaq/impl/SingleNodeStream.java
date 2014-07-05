package org.nustaq.impl;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

/**
 * Created by ruedi on 05.07.14.
 */
public class SingleNodeStream<T extends Record> extends Actor<SingleNodeStream<T>> implements RLStream<T>, ChangeBroadcastReceiver<T> {

    RLTableImpl<T> tableActor;
    ArrayList<Subscription<T>> subscribers = new ArrayList<>();

    public SingleNodeStream() {
    }

    public void $init(RLTableImpl<T> tableActor) {
        this.tableActor = tableActor;
    }

    @Override
    public void $each(Callback<T> resultReceiver) {
        tableActor.$filter(null,null,resultReceiver);
    }

    @Override
    public void $filter(Predicate<T> matches, Callback<T> resultReceiver) {
        tableActor.$filter(matches,null,resultReceiver);
    }

    @Override
    public void $filterUntil(Predicate<T> matches, Predicate<T> terminateQuery, Callback<T> resultReceiver) {
        tableActor.$filter(matches,terminateQuery,resultReceiver);
    }

    @Override
    public void $filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver) {
        tableActor.$filterBinary(doProcess, terminate, resultReceiver);
    }

    @Override @CallerSideMethod
    public Subscription subscribe(Predicate<T> matches, Callback<ChangeBroadcast<T>> resultReceiver) {
        Subscription<T> subs = new Subscription<>(resultReceiver,matches);
        self().$subscribe(subs);
        return subs;
    }

    @Override @CallerSideMethod
    public Subscription listen(Predicate<T> matches, Callback<ChangeBroadcast<T>> resultReceiver) {
        Subscription<T> subs = new Subscription<>(resultReceiver,matches);
        self().$listen(subs);
        return subs;
    }

    public void $subscribe(Subscription subs) {
        subscribers.add(subs);
        tableActor.$filter(subs.getFilter(),null,(r,e) -> {
            Callback callback = subs.getCallback();
            callback.receiveResult(r,e);
        });
    }

    public void $listen(Subscription subs) {
        subscribers.add(subs);
    }

    @Override
    public void unsubscribe(Subscription subs) {
        subscribers.remove(subs);
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    if (subs.getFilter().test(changeBC.getNewRecord())) {
                        subs.getCallback().receiveResult(changeBC, null);
                    }
                }
                break;
            case ChangeBroadcast.REMOVE:
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    if ( subs.getFilter().test(changeBC.getNewRecord()) ) {
                        subs.getCallback().receiveResult(changeBC,null);
                    }
                }
                break;
            case ChangeBroadcast.UPDATE:
                changeBC.toOld();
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    subs.__matched = subs.getFilter().test(changeBC.getNewRecord());
                }
                changeBC.toNew();
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    boolean matchesOld = subs.__matched;
                    boolean matchesNew = subs.getFilter().test(changeBC.getNewRecord());
                    if ( matchesOld && matchesNew ) {
                        subs.onChangeReceived(changeBC);
                    } else if ( matchesOld && ! matchesNew ) {
                        subs.onChangeReceived(
                            new ChangeBroadcast<T>(
                                ChangeBroadcast.REMOVE,
                                changeBC.getTableId(),
                                changeBC.getRecordKey(),
                                changeBC.getNewRecord(),
                                null
                            ));
                    } else if ( ! matchesOld && matchesNew ) {
                        subs.onChangeReceived(
                            new ChangeBroadcast<T>(
                                ChangeBroadcast.ADD,
                                changeBC.getTableId(),
                                changeBC.getRecordKey(),
                                changeBC.getNewRecord(),
                                null
                            ));
                    } // else did not match and does not match
                }
                break;
            default:
                throw new RuntimeException("not implemented");
        }
    }
}
