package org.nustaq.reallive;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 05.07.14.
 */
public class ChangeBroadcast<T extends Record> implements Serializable {

    public static final int UPDATE    = 0;
    public static final int ADD       = 1;
    public static final int REMOVE    = 2;
    public static final int OPERATION = 3;
    public static final int SNAPSHOT_DONE = 4;
    public static final int ERROR = 5;

    public static <T extends Record> ChangeBroadcast<T> NewSnapFin(String tableId) {
        return new ChangeBroadcast(ChangeBroadcast.SNAPSHOT_DONE, tableId, null, null, null);
    }

    public static <T extends Record> ChangeBroadcast NewUpdate(String tableId, T t, RecordChange appliedChange) {
        return new ChangeBroadcast(UPDATE,tableId,t.getRecordKey(),t,appliedChange);
    }

    public static <T extends Record> ChangeBroadcast<T> NewAdd(String tableId, T record) {
        return new ChangeBroadcast<>(ADD,tableId,record.getRecordKey(),record,null);
    }

    public static <T extends Record> ChangeBroadcast<T> NewError(String tableId, Object e) {
        ChangeBroadcast<T> tChangeBroadcast = new ChangeBroadcast<>(ERROR, tableId, null, null, null);
        tChangeBroadcast.setError(e);
        return tChangeBroadcast;
    }

    public static <T extends Record> ChangeBroadcast<T> NewRemove(String tableId, T record) {
        return new ChangeBroadcast<>(REMOVE,tableId,record.getRecordKey(),record,null);
    }

    private ChangeBroadcast(int type, String tableId, String recordKey, T newRecord, RecordChange<String, T> appliedChange) {
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
    Object error;

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public boolean isSnapshotDone() {
        return getType() == SNAPSHOT_DONE;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getType() {
        return type;
    }

    public String getTableId() {
        return tableId;
    }

    public T getRecord() {
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

    public String toString() {
        switch (type) {
            case ADD:
                return "ChangeBC ADD on " + recordKey + " " + newRecord;
            case REMOVE:
                return "ChangeBC REMOVE on " + recordKey + " " + newRecord;
            case UPDATE:
                return "ChangeBC UPDATE on " + recordKey + " " + Arrays.toString(newRecord.toFieldNames(appliedChange.getChangedFields()));
            case SNAPSHOT_DONE:
                return "ChangeBC SNAPSHOT_DONE on " + tableId;
            case ERROR:
                return "ChangeBC ERROR on " + tableId+" "+error;
            default:
                return super.toString();
        }
    }

    public boolean isError() {
        return getType() == ERROR;
    }

    public boolean isAdd() {
        return getType() == ADD;
    }

    public boolean isARU() {
        return type == ADD || type == UPDATE || type == REMOVE;
    }
}
