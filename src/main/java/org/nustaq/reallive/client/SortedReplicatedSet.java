package org.nustaq.reallive.client;

import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.Record;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by ruedi on 19.07.14.
 */
public class SortedReplicatedSet<T extends Record> extends ReplicatedSet<T> {

    TreeSet<T> treeSet;

    public SortedReplicatedSet(Comparator<T> comp) {
        super();
        treeSet = new TreeSet<>(comp);
    }

    @Override
    protected void handleUpdate(ChangeBroadcast<T> changeBC) {
        treeSet.remove(map.get(changeBC.getRecordKey()));
        super.handleUpdate(changeBC);
        treeSet.add(changeBC.getRecord());
    }

    @Override
    protected void handleRemove(ChangeBroadcast<T> changeBC) {
        super.handleRemove(changeBC);
        treeSet.remove(changeBC.getRecord());
    }

    @Override
    protected void handleAdd(T newRec) {
        super.handleAdd(newRec);
        treeSet.add(newRec);
    }

    public TreeSet<T> getTreeSet() {
        return treeSet;
    }

}
