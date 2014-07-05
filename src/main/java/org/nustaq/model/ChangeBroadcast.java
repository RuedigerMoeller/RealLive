package org.nustaq.model;

/**
 * Created by ruedi on 05.07.14.
 */
public class ChangeBroadcast<T extends Record> {

    public static final int UPDATE    = 0;
    public static final int ADD       = 1;
    public static final int REMOVE    = 2;
    public static final int OPERATION = 3;


    public ChangeBroadcast(int type, String tableId, String recordKey, T newRecord, RecordChange<String, T> appliedChange) {
        this.type = type;
        this.tableId = tableId;
        this.newRecord = newRecord;
        this.appliedChange = appliedChange;
        this.recordKey = recordKey;
    }

    int type;
    String tableId;
    String recordKey;

    T newRecord; // state of record after update
    RecordChange<String,T> appliedChange; // in case of update contains old values of updated fields

    public String getRecordKey() {
        return recordKey;
    }

    public int getType() {
        return type;
    }

    public String getTableId() {
        return tableId;
    }

    public T getNewRecord() {
        return newRecord;
    }

    public RecordChange<String, T> getAppliedChange() {
        return appliedChange;
    }

    public void toOld() {
        appliedChange.setOld(newRecord);
    }

    public void toNew() {
        appliedChange.setNew(newRecord);
    }

}
