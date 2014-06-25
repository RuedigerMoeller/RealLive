package org.nustaq.storage.fststorage;

import org.nustaq.heapoff.structs.FSTStruct;

/**
 * Created by ruedi on 25.06.14.
 */
public class HashValue extends FSTStruct {

    protected long valueOffset;
    protected long chunkNum;

    public HashValue() {
    }

    public HashValue(long valueOffset, long chunkNum) {
        this.valueOffset = valueOffset;
        this.chunkNum = chunkNum;
    }

    public long getValueOffset() {
        return valueOffset;
    }

    public void setValueOffset(long valueOffset) {
        this.valueOffset = valueOffset;
    }

    public long getChunkNum() {
        return chunkNum;
    }

    public void setChunkNum(long chunkNum) {
        this.chunkNum = chunkNum;
    }

    @Override
    public String toString() {
        return "HashValue{" +
                   "valueOffset=" + valueOffset +
                   ", chunkNum=" + chunkNum +
                   '}';
    }
}
