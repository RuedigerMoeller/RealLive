package org.nustaq.reallive;

import org.nustaq.serialization.FSTClazzInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ruedi on 21.06.14.
 */
public class RecordChange<K, T extends Record> implements Serializable {

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

    public String getTableId() {
        return tableId;
    }

    public K getId() {
        return recordId;
    }

    /**
     * apply change to a record thereby collecting original values to the oldVal array
     * @param rec
     * @return
     */
    final static Object EMPTY = new Object() { public String toString() { return "EMPTY"; }};
    public RecordChange<K,T> apply(T rec) {
        RecordChange<K,T> res = new RecordChange<>(this);
        Object oldValues[] = new Object[newVal.length];
        int eqCounter = 0;
        for (int i = 0; i < newVal.length; i++) {
            Object val = newVal[i];
            int id = fieldIndex[i];
            final Object oldVal = rec.getField(id);

            if ( (oldVal == null && newVal == null) || (oldVal != null && oldVal.equals(val)) ) {
                eqCounter++;
                oldValues[i] = EMPTY;
            } else {
                oldValues[i] = oldVal;
                rec.setField(id, val);
            }
        }
        res.oldVals = oldValues;
        if (eqCounter>0) {
            res.shrink(eqCounter);
        }
        return res;
    }

    private void shrink(int eqCounter) {
        int newFieldIndex[] = new int[fieldIndex.length-eqCounter];
        Object newOldVals[] = new Object[oldVals.length-eqCounter];
        Object newNewVals[] = new Object[newVal.length-eqCounter];
        int idx = 0;
        for (int i = 0; i < oldVals.length; i++) {
            Object oldVal = oldVals[i];
            if (oldVal != EMPTY) {
                newFieldIndex[idx] = fieldIndex[i];
                newOldVals[idx] = oldVals[i];
                newNewVals[idx] = newVal[i];
                idx++;
            }
        }
        fieldIndex = newFieldIndex;
        oldVals = newOldVals;
        newVal = newNewVals;
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

    public int[] getChangedFields() {
        return fieldIndex;
    }
}
