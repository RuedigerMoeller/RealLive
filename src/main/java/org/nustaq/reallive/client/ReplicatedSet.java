package org.nustaq.reallive.client;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Created by ruedi on 06.07.14.
 */
public class ReplicatedSet<T extends Record> implements ChangeBroadcastReceiver<T>, RLStream<T> {

    protected HashMap<String,T> map = new HashMap<>();
    protected Promise snapFin;
    protected String tableId = "replicated";
    protected boolean snaphotFinished = false;

    public ReplicatedSet() {
        reset();
    }
    protected List<MySubscription> subscribers;

    public ReplicatedSet(String tableId) {
        this();
        this.tableId = tableId;
    }

    public void reset() {
        snaphotFinished = false;
        snapFin = new Promise();
        map.clear();
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        T newRec = changeBC.getRecord();
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                handleAdd(newRec);
                break;
            case ChangeBroadcast.REMOVE:
                handleRemove(changeBC);
                break;
            case ChangeBroadcast.UPDATE:
                handleUpdate(changeBC);
                break;
            case ChangeBroadcast.SNAPSHOT_DONE:
                snaphotFinished = true;
                snapFin.complete("void", null);
                break;
            case ChangeBroadcast.ERROR:
            default:
        }
        if ( subscribers != null ) {
            processSubscribers(changeBC);
        }

    }

    public void processSubscribers(ChangeBroadcast<T> changeBC) { // FIXME mostly copied from SingleNodeStream
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                for (int i = 0; i < subscribers.size(); i++) {
                    MySubscription<T> subs = (MySubscription<T>) subscribers.get(i);
                    if (subs.getFilter().test(changeBC.getRecord())) {
                        subs.getReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.REMOVE:
                for (int i = 0; i < subscribers.size(); i++) {
                    MySubscription<T> subs = (MySubscription<T>) subscribers.get(i);
                    if (subs.getFilter().test(changeBC.getRecord())) {
                        subs.getReceiver().onChangeReceived(changeBC);
                    }
                }
                break;
            case ChangeBroadcast.UPDATE:
                changeBC.toOld();
                for (int i = 0; i < subscribers.size(); i++) {
                    MySubscription<T> subs = (MySubscription<T>) subscribers.get(i);
                    subs.__matched(subs.getFilter().test(changeBC.getRecord()));
                }
                changeBC.toNew();
                for (int i = 0; i < subscribers.size(); i++) {
                    MySubscription<T> subs = (MySubscription<T>) subscribers.get(i);
                    boolean matchesOld = subs.__matched();
                    boolean matchesNew = subs.getFilter().test(changeBC.getRecord());
                    if (matchesOld && matchesNew) {
                        subs.getReceiver().onChangeReceived(changeBC); // directly forward change
                    } else if (matchesOld && !matchesNew) {
                        subs.getReceiver().onChangeReceived(
                            ChangeBroadcast.NewRemove(
                                changeBC.getTableId(),
                                changeBC.getRecord(),
                                changeBC.getOriginator()
                                                     ));
                    } else if (!matchesOld && matchesNew) {
                        subs.getReceiver().onChangeReceived(
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

    Thread _t;
    protected final void checkThread() {
        if (_t==null) {
            _t = Thread.currentThread();
        } else {
            if ( _t != Thread.currentThread() ) {
                throw new RuntimeException("Wrong Thread");
            }
        }
    }

    protected void handleUpdate(ChangeBroadcast<T> changeBC) {
        checkThread();
        T t = map.get(changeBC.getRecordKey());
        if ( t == null ) {
            System.out.println("replication error: unknown record updated: "+changeBC.getRecordKey()+" "+changeBC.getRecord());
        } else {
            changeBC.getAppliedChange().apply(t);
        }
    }

    protected void handleRemove(ChangeBroadcast<T> changeBC) {
        map.remove(changeBC.getRecordKey());
    }

    protected void handleAdd(T newRec) {
        map.put(newRec.getRecordKey(),newRec);
    }

    /**
     * can only be used once. Fires upon SNAPSHOT_DONE
     * @param r
     */
    public void onFinished( Runnable r ) {
        snapFin.then((res, err) -> r.run());
    }

    public void dump() {
        map.values().stream().sorted( (a,b) -> a.getRecordKey().compareTo(b.getRecordKey()) ).forEach( (r) -> {
            System.out.println(r);
        });
    }

    public int getSize() {
        return map.size();
    }

    public boolean isSnaphotFinished() {
        return snaphotFinished;
    }

    /**
     * warning, this can make the set corrupt. Usable in case one updates a replication locally at the
     * same time ignoring all change broadcasts induced by self (using originatorId)
     * @param key
     */
    public void unsafeRemove(String key) {
        map.remove(key);
    }

    /**
     * warning, this can make the set corrupt. Usable in case one updates a replication locally at the
     * same time ignoring all change broadcasts induced by self (using originatorId)
     * @param r
     */
    public void unsafeAdd(T r) {
        map.put(r.getRecordKey(),r);
    }

    @Override
    public void forEach(ChangeBroadcastReceiver<T> resultReceiver) {
        map.values().forEach((record) -> {
            resultReceiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, record, 0));
        });
        resultReceiver.onChangeReceived(ChangeBroadcast.NewSnapFin(tableId, 0));
    }

    @Override
    public void filter(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        map.values().forEach((record) -> {
            if ( matches.test(record) )
                resultReceiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId,record,0));
        });
        resultReceiver.onChangeReceived(ChangeBroadcast.NewSnapFin(tableId, 0));
    }

    @Override
    public IPromise filterUntil(Predicate<T> matches, BiPredicate<T, Integer> terminateQuery, ChangeBroadcastReceiver<T> resultReceiver) {
        int count[] = {0};
        map.values().forEach((record) -> {
            boolean terminate = terminateQuery.test(record, count[0]);
            if ( matches.test(record) && !terminate) {
                count[0]++;
                resultReceiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, record, 0));
            }
        });
        resultReceiver.onChangeReceived(ChangeBroadcast.NewSnapFin(tableId, 0));
        return new Promise<>("void");
    }

    @Override
    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback<ByteSource> resultReceiver) {
        throw new RuntimeException("unsupported for replicated sets");
    }

    public T get(String recordKey) {
        return map.get(recordKey);
    }

    static abstract class MySubscription<T extends Record> implements Subscription<T> {
        boolean matched;
        public boolean __matched() {
            return matched;
        }
        public void __matched(boolean b) {
            matched = b;
        }

        public abstract ChangeBroadcastReceiver<T> getReceiver();
    }

    protected List getSubscribers() {
        if ( subscribers == null ) {
            subscribers = new ArrayList<>();
        }
        return subscribers;
    }

    @Override
    public Subscription<T> subscribeKey(String key, ChangeBroadcastReceiver<T> resultReceiver) {
        Predicate filter = new Predicate() {
            @Override
            public boolean test(Object o) {
                return ((Record)o).getRecordKey().equals(key);
            }
        };
        MySubscription subs = new MySubscription() {
            public ChangeBroadcastReceiver<T> getReceiver() {
                return resultReceiver;
            }
            @Override
            public String getTableKey() {
                return tableId;
            }
            @Override
            public Predicate getFilter() {
                return filter;
            }
        };
        if ( map.get(key) != null ) {
            resultReceiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, map.get(key), 0));
        }
        resultReceiver.onChangeReceived(ChangeBroadcast.NewSnapFin(tableId, 0));
        getSubscribers().add(subs);
        return subs;
    }

    @Override
    public Subscription<T> subscribe(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        MySubscription subs = new MySubscription() {
            public ChangeBroadcastReceiver<T> getReceiver() {
                return resultReceiver;
            }
            @Override
            public String getTableKey() {
                return tableId;
            }
            @Override
            public Predicate getFilter() {
                return matches;
            }
        };
        getSubscribers().add(subs);
        filter(matches,resultReceiver);
        return subs;
    }

    @Override
    public Subscription<T> listen(Predicate<T> matches, ChangeBroadcastReceiver<T> resultReceiver) {
        MySubscription subs = new MySubscription() {
            public ChangeBroadcastReceiver<T> getReceiver() {
                return resultReceiver;
            }
            @Override
            public String getTableKey() {
                return tableId;
            }
            @Override
            public Predicate getFilter() {
                return matches;
            }
        };
        getSubscribers().add(subs);
        return subs;
    }

    @Override
    public void unsubscribe(Subscription<T> subs) {
        getSubscribers().remove(subs);
    }
}

