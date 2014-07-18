package org.nustaq.reallive.impl;

import org.nustaq.heapoff.bytez.ByteSource;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.reallive.*;

import java.util.ArrayList;
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
    public void each(@InThread ChangeBroadcastReceiver<T> resultReceiver) {
        filterUntil(null,null,resultReceiver);
    }

    @Override
    public void filter(Predicate<T> matches, @InThread ChangeBroadcastReceiver<T> resultReceiver) {
        filterUntil(matches,null,resultReceiver);
    }

    @Override
    public void filterUntil(Predicate<T> matches, Predicate<T> terminateQuery, @InThread ChangeBroadcastReceiver<T> resultReceiver) {
        tableActor.$filter(matches,terminateQuery, (r,e) -> {
            if ( e == RLTable.FIN ) {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewSnapFin(tableActor.getTableId()));
            } else if ( e == null ) {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewAdd(tableActor.getTableId(),r));
            } else {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewError(tableActor.getTableId(), e));
            }
        });
    }

    @Override
    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback<ByteSource> resultReceiver) {
        tableActor.$filterBinary(doProcess, terminate, resultReceiver);
    }

    @Override @CallerSideMethod
    public Subscription subscribeKey( String key, ChangeBroadcastReceiver<T> resultReceiver) {
        Subscription subs = new Subscription(getActor().tableActor.getTableId(), new Subscription.KeyPredicate(key),resultReceiver);
        self().$subscribe(subs);
        return subs;
    }

    @Override @CallerSideMethod
    public Subscription subscribe(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        Subscription<T> subs = new Subscription<>(getActor().tableActor.getTableId(),matches,resultReceiver);
        self().$subscribe(subs);
        return subs;
    }

    @Override @CallerSideMethod
    public Subscription listen(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        Subscription<T> subs = new Subscription<>(getActor().tableActor.getTableId(),matches,resultReceiver);
        self().$listen(subs);
        return subs;
    }

    public void $subscribe(Subscription subs) {
        subscribers.add(subs);
        if ( subs.getFilter() instanceof Subscription.KeyPredicate ) {
            self().$listen(subs);
            tableActor.$get( ((Subscription.KeyPredicate) subs.getFilter()).getKey())
                .then(
                    (record,e) -> {
                        if ( record == null ) {
                            ChangeBroadcast changeBC = ChangeBroadcast.NewSnapFin(tableActor.getTableId());
                            subs.getChangeReceiver().onChangeReceived(changeBC);
                        } else {
                            ChangeBroadcast changeBC = ChangeBroadcast.NewAdd(tableActor.getTableId(), record);
                            subs.getChangeReceiver().onChangeReceived(changeBC);
                        }
                    });
            return;
        }
        tableActor.$filter(subs.getFilter(),null, (r,e) -> {
            if ( e == null ) {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewAdd(tableActor.getTableId(), r));
            } else if ( e == RLTable.FIN ) {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewSnapFin(tableActor.getTableId()));
            } else {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewError(tableActor.getTableId(), e));
            }
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
                    if (subs.getFilter().test(changeBC.getRecord())) {
                        subs.getChangeReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.REMOVE:
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    if ( subs.getFilter().test(changeBC.getRecord()) ) {
                        subs.getChangeReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.UPDATE:
                changeBC.toOld();
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    subs.__matched = subs.getFilter().test(changeBC.getRecord());
                }
                changeBC.toNew();
                for (int i = 0; i < subscribers.size(); i++) {
                    Subscription<T> subs = subscribers.get(i);
                    boolean matchesOld = subs.__matched;
                    boolean matchesNew = subs.getFilter().test(changeBC.getRecord());
                    if ( matchesOld && matchesNew ) {
                        subs.onChangeReceived(changeBC); // directly forward change
                    } else if ( matchesOld && ! matchesNew ) {
                        subs.onChangeReceived(
                            ChangeBroadcast.NewRemove(
                                changeBC.getTableId(),
                                changeBC.getRecord()
                            ));
                    } else if ( ! matchesOld && matchesNew ) {
                        subs.onChangeReceived(
                            ChangeBroadcast.NewAdd(
                                changeBC.getTableId(),
                                changeBC.getRecord()
                                                  ));
                    } // else did not match and does not match
                }
                break;
            default:
                throw new RuntimeException("not implemented");
        }
    }
}
