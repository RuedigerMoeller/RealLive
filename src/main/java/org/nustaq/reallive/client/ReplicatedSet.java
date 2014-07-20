package org.nustaq.reallive.client;

import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.ChangeBroadcastReceiver;
import org.nustaq.reallive.Record;

import java.util.HashMap;

/**
 * Created by ruedi on 06.07.14.
 */
public class ReplicatedSet<T extends Record> implements ChangeBroadcastReceiver<T> {

    protected HashMap<String,T> map = new HashMap<>();
    protected Promise snapFin;
    boolean snaphotFinished = false;

    public ReplicatedSet() {
        snapFin = new Promise();
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
                snapFin.receiveResult("void",null);
                break;
            case ChangeBroadcast.ERROR:
            default:
        }

    }

    protected void handleUpdate(ChangeBroadcast<T> changeBC) {
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

}

