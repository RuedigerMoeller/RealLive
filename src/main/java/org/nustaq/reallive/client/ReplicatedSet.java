package org.nustaq.reallive.client;

import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.ChangeBroadcastReceiver;
import org.nustaq.reallive.Record;

import java.util.HashMap;

/**
 * Created by ruedi on 06.07.14.
 */
public class ReplicatedSet<T extends Record> implements ChangeBroadcastReceiver<T> {

    HashMap<String,T> map = new HashMap<>();

    @Override
    public void onChangeReceived(ChangeBroadcast<T> changeBC) {
        T newRec = changeBC.getRecord();
        switch (changeBC.getType()) {
            case ChangeBroadcast.ADD:
                map.put(newRec.getId(),newRec);
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
                break;
            case ChangeBroadcast.ERROR:
            default:
        }

    }

    public void dump() {
        map.values().stream().sorted( (a,b) -> a.getId().compareTo(b.getId()) ).forEach( (r) -> {
            System.out.println(r);
        });
    }

}
