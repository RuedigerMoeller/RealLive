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

    HashMap<String,T> map = new HashMap<>();
    Promise snapFin;

    public ReplicatedSet() {
        snapFin = new Promise();
    }

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        T newRec = changeBC.getRecord();
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                map.put(newRec.getRecordKey(),newRec);
                break;
            case ChangeBroadcast.REMOVE:
                map.remove(changeBC.getRecordKey());
                break;
            case ChangeBroadcast.UPDATE:
                T t = map.get(changeBC.getRecordKey());
                if ( t == null ) {
                    System.out.println("replication error: unknown record updated: "+changeBC.getRecordKey()+" "+changeBC.getRecord());
                } else {
                    changeBC.getAppliedChange().apply(t);
                }
                break;
            case ChangeBroadcast.SNAPSHOT_DONE:
                snapFin.receiveResult("void",null);
                break;
            case ChangeBroadcast.ERROR:
            default:
        }

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
}
