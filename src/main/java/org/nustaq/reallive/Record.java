package org.nustaq.reallive;

import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.sys.annotations.Order;
import org.nustaq.serialization.FSTClazzInfo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ruedi on 01.06.14.
 */
public class Record implements Serializable {

    public static enum Mode {
        ADD,
        UPDATE,
        NONE, UPDATE_OR_ADD,
    }
    transient Mode mode = Mode.NONE;

    @Order(-2)
    String key;
    @Order(-1)
    int version;

    transient Record originalRecord;
    transient RLTable table;

    public Record() {

    }

    public Record(Record originalRecord) {
        this.originalRecord = originalRecord;
        this.key = originalRecord.getKey();
        this.table = originalRecord.table;
    }

    public Record(String key, RLTable schema) {
        this.key = key;
        this.table = schema;
    }

    public void _setTable(RLTable table) {
        this.table = table;
    }

    public void _setId(String id) {
        this.key = id;
    }

    public void _setOriginalRecord(Record org) {
        originalRecord = org;
    }

    public void _setMode(Mode newMode) {
        mode = newMode;
    }

    public Mode getMode() {
        return mode;
    }

    public String getKey()
    {
        return key;
    }

    public void copyTo( Record other ) {
        if ( other.getClass() != getClass() )
            throw new RuntimeException("other record must be of same type");
        FSTClazzInfo classInfo = getClassInfo();
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();
        other.table = table;

        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            try {
                if ( fi.isPrimitive() ) {
                    switch (fi.getIntegralCode(fi.getType())) {
                        case FSTClazzInfo.FSTFieldInfo.BOOL:
                            fi.setBooleanValue(other, fi.getBooleanValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.BYTE:
                            fi.setByteValue(other, (byte) fi.getByteValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.CHAR:
                            fi.setCharValue(other, (char) fi.getCharValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.SHORT:
                            fi.setShortValue(other, (short) fi.getShortValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.INT:
                            fi.setIntValue(other, fi.getIntValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.LONG:
                            fi.setLongValue(other, fi.getLongValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.FLOAT:
                            fi.setFloatValue(other, fi.getFloatValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.DOUBLE:
                            fi.setDoubleValue(other, fi.getDoubleValue(this) );
                            break;
                    }
                } else {
                    fi.setObjectValue(other, fi.getObjectValue(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * persist an add or update of a record
     */
    public Future<String> $apply() {
        if ( mode == Mode.ADD ) {
            return table.$addGetId(this);
        } else
        if ( mode == Mode.UPDATE || mode == Mode.UPDATE_OR_ADD ) {
            if ( originalRecord == null )
                throw new RuntimeException("original record must not be null for update");
            if ( key == null )
                throw new RuntimeException("key must not be null on update");
            RecordChange recordChange = computeDiff();
            table.$update(recordChange, mode == Mode.UPDATE_OR_ADD);
            copyTo(originalRecord);
            return new Promise<>(key);
        } else
            throw new RuntimeException("wrong mode. Use table.create* and table.prepare* methods.");
    }

    public RecordChange computeDiff() {
        FSTClazzInfo classInfo = getClassInfo();
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();

        RecordChange change = new RecordChange(getKey());

        ArrayList<FSTClazzInfo.FSTFieldInfo> changedFields = new ArrayList<>();
        ArrayList changedValues = new ArrayList();

        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            boolean changed = false;
            try {
                if ( fi.isPrimitive() ) {
                    switch (fi.getIntegralCode(fi.getType())) {
                        case FSTClazzInfo.FSTFieldInfo.BOOL:
                            changed = fi.getBooleanValue(originalRecord) != fi.getBooleanValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.BYTE:
                            changed = fi.getByteValue(originalRecord) != fi.getByteValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.CHAR:
                            changed = fi.getCharValue(originalRecord) != fi.getCharValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.SHORT:
                            changed = fi.getShortValue(originalRecord) != fi.getShortValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.INT:
                            changed = fi.getIntValue(originalRecord) != fi.getIntValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.LONG:
                            changed = fi.getLongValue(originalRecord) != fi.getLongValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.FLOAT:
                            changed = fi.getFloatValue(originalRecord) != fi.getFloatValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.DOUBLE:
                            changed = fi.getDoubleValue(originalRecord) != fi.getDoubleValue(this);
                            break;
                    }
                } else {
                    changed = fi.getObjectValue(originalRecord) != fi.getObjectValue(this);
                }
                if ( changed ) {
                    changedFields.add(fi);
                    changedValues.add(fi.getField().get(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        change.setChanges(changedFields,changedValues);
        return change;
    }

    public FSTClazzInfo getClassInfo() {
        return getRealLive().getConf().getClassInfo(getClass());
    }

    public RealLive getRealLive() {
        return table.getRealLive();
    }

    public String toString() {
        String res = "["+getClass().getSimpleName()+" ";
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = getClassInfo().getFieldInfo();
        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fstFieldInfo = fieldInfo[i];
            try {
                res += fstFieldInfo.getField().get(this) + "\t ";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return res+" ]";
    }

    public String[] toFieldNames(int fieldIndex[]) {
        String res[] = new String[fieldIndex.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = getClassInfo().getFieldInfo()[fieldIndex[i]].getField().getName();
        }
        return res;
    }

    public Object getField( int indexId ) {
        FSTClazzInfo.FSTFieldInfo fi = getClassInfo().getFieldInfo()[indexId];
        try {
            return fi.getField().get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setField( int indexId, Object value ) {
        FSTClazzInfo.FSTFieldInfo fi = getClassInfo().getFieldInfo()[indexId];
        try {
            fi.getField().set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public int getVersion() {
        return version;
    }
    public void incVersion() {
        version++;
    }
}
