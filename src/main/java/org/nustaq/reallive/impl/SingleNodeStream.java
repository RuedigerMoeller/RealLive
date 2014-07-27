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
    ArrayList<SubscriptionImpl<T>> subscribers = new ArrayList<>();

    public SingleNodeStream() {
    }

    public void $init(String name, RLTableImpl<T> tableActor) {
        Thread.currentThread().setName("SNStream:"+name);
        checkThread();
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
            checkThread();
            if ( e == RLTable.FIN ) {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewSnapFin(tableActor.getTableId(),0));
            } else if ( e == null ) {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewAdd(tableActor.getTableId(),r,0));
            } else {
                resultReceiver.onChangeReceived( ChangeBroadcast.NewError(tableActor.getTableId(), e,0));
            }
        });
    }

    @Override
    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback<ByteSource> resultReceiver) {
        tableActor.$filterBinary(doProcess, terminate, resultReceiver);
    }

    @Override @CallerSideMethod
    public Subscription<T> subscribeKey(String key, ChangeBroadcastReceiver<T> resultReceiver) {
        SubscriptionImpl subs = new SubscriptionImpl(getActor().tableActor.getTableId(), new SubscriptionImpl.KeyPredicate(key),inThread(sender.get(),resultReceiver));
        self().$subscribe(subs);
        return subs;
    }

    @Override @CallerSideMethod
    public Subscription<T> subscribe(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        SubscriptionImpl<T> subs = new SubscriptionImpl<>(getActor().tableActor.getTableId(),matches,inThread(sender.get(),resultReceiver));
        self().$subscribe(subs);
        return subs;
    }

    @Override @CallerSideMethod
    public Subscription<T> listen(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        SubscriptionImpl<T> subs = new SubscriptionImpl<>(getActor().tableActor.getTableId(),matches,inThread(sender.get(),resultReceiver));
        self().$listen(subs);
        return subs;
    }

    public void $subscribe(SubscriptionImpl subs) { // fixme: inthread missing, not possible
        checkThread();
        subscribers.add(subs);
        if ( subs.getFilter() instanceof SubscriptionImpl.KeyPredicate ) {
            tableActor.$get( ((SubscriptionImpl.KeyPredicate) subs.getFilter()).getKey())
                .then(
                    (record,e) -> {
                        checkThread();
                        if ( record == null ) {
                            ChangeBroadcast changeBC = ChangeBroadcast.NewSnapFin(tableActor.getTableId(),0);
                            subs.getChangeReceiver().onChangeReceived(changeBC);
                        } else {
                            ChangeBroadcast changeBC = ChangeBroadcast.NewAdd(tableActor.getTableId(), record,0);
                            subs.getChangeReceiver().onChangeReceived(changeBC);
                        }
                    });
            return;
        }
        tableActor.$filter(subs.getFilter(),null, (r,e) -> {
            checkThread();
            if ( e == null ) {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewAdd(tableActor.getTableId(), r,0));
            } else if ( e == RLTable.FIN ) {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewSnapFin(tableActor.getTableId(),0));
            } else {
                subs.getChangeReceiver().onChangeReceived(ChangeBroadcast.NewError(tableActor.getTableId(), e,0));
            }
        });
    }

    public void $listen(SubscriptionImpl subs) {
        subscribers.add(subs);
    }

    @Override
    public void unsubscribe(Subscription<T> subs) {
        subscribers.remove(subs);
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        checkThread();
        if ( changeBC.isARU() ) {
            changeBC.getRecord()._setTable(tableActor);
        }
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                for (int i = 0; i < subscribers.size(); i++) {
                    SubscriptionImpl<T> subs = subscribers.get(i);
                    if (subs.getFilter().test(changeBC.getRecord())) {
                        subs.getChangeReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.REMOVE:
                for (int i = 0; i < subscribers.size(); i++) {
                    SubscriptionImpl<T> subs = subscribers.get(i);
                    if ( subs.getFilter().test(changeBC.getRecord()) ) {
                        subs.getChangeReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.UPDATE:
                changeBC.toOld();
                for (int i = 0; i < subscribers.size(); i++) {
                    SubscriptionImpl<T> subs = subscribers.get(i);
                    subs.__matched = subs.getFilter().test(changeBC.getRecord());
                }
                changeBC.toNew();
                for (int i = 0; i < subscribers.size(); i++) {
                    SubscriptionImpl<T> subs = subscribers.get(i);
                    boolean matchesOld = subs.__matched;
                    boolean matchesNew = subs.getFilter().test(changeBC.getRecord());
                    if ( matchesOld && matchesNew ) {
                        subs.onChangeReceived(changeBC); // directly forward change
                    } else if ( matchesOld && ! matchesNew ) {
                        subs.onChangeReceived(
                            ChangeBroadcast.NewRemove(
                                changeBC.getTableId(),
                                changeBC.getRecord(),
                                changeBC.getOriginator()
                            ));
                    } else if ( ! matchesOld && matchesNew ) {
                        subs.onChangeReceived(
                            ChangeBroadcast.NewAdd(
                                changeBC.getTableId(),
                                changeBC.getRecord(),
                                changeBC.getOriginator()
                        ));
                    } else {
                        // silent
                    }
                }
                break;
            default:
                throw new RuntimeException("not implemented");
        }
    }
}