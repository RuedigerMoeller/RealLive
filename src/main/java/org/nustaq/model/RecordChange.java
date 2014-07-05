package org.nustaq.model;

import org.nustaq.serialization.FSTClazzInfo;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ruedi on 21.06.14.
 */
public class RecordChange<K, T extends Record> implements Change<K,T> {

    K recordId;
    String tableId;

    Object[] newVal;
    Object[] oldVals;
    int [] fieldIndex;

    public RecordChange(K id) {
        recordId = id;
    }

    public RecordChange(RecordChange<K, T> rc) {
        this.recordId = rc.recordId;
        this.tableId = rc.tableId;
        this.newVal = rc.newVal;
        this.fieldIndex = rc.fieldIndex;
        this.oldVals = rc.oldVals;
    }

    public void setChanges(List<FSTClazzInfo.FSTFieldInfo> fieldInfos, List<Object> value ) {
        newVal = value.toArray();
        fieldIndex = new int[newVal.length];
        for (int i = 0; i < fieldIndex.length; i++) {
            fieldIndex[i] = fieldInfos.get(i).getIndexId();
        }
    }

    @Override
    public String toString() {
        return "RecordChange{" +
                "recordId=" + recordId +
                ", tableId='" + tableId + '\'' +
                ", newVal=" + Arrays.toString(newVal) +
                ", oldVals=" + Arrays.toString(oldVals) +
                ", fieldIndex=" + Arrays.toString(fieldIndex) +
                '}';
    }

    @Override
    public String getTableId() {
        return tableId;
    }

    @Override
    public K getId() {
        return recordId;
    }

    /**
     * apply change to a record thereby collecting original values to the oldVal array
     * @param rec
     * @return
     */
    @Override
    public RecordChange<K,T> apply(T rec) {
        RecordChange<K,T> res = new RecordChange<>(this);
        Object oldValues[] = new Object[newVal.length];
        for (int i = 0; i < newVal.length; i++) {
            Object val = newVal[i];
            int id = fieldIndex[i];
            oldValues[i] = rec.getField(id);
            rec.setField(id,val);
        }
        res.oldVals = oldValues;
        return res;
    }

    /**
     * revert record original by applying old values
     * @param rec
     * @return
     */
    public void setOld(T rec) {
        for (int i = 0; i < oldVals.length; i++) {
            Object val = oldVals[i];
            int id = fieldIndex[i];
            rec.setField(id,val);
        }
    }

    /**
     * apply new values to record
     * @param rec
     * @return
     */
    public void setNew(T rec) {
        for (int i = 0; i < newVal.length; i++) {
            Object val = newVal[i];
            int id = fieldIndex[i];
            rec.setField(id,val);
        }
    }

    /**
     * can be used to update several records with the same change object. Use with care
     * @param recordId
     */
    public void _setRecordId(K recordId) {
        this.recordId = recordId;
    }
}
