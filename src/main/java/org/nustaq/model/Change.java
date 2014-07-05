package org.nustaq.model;

/**
 * Created by ruedi on 21.06.14.
 */
public interface Change<K,T extends Record> {

    public String getTableId();
    public K getId();

    /**
     * applies a change and returns an applied change where oldvals denote the previous values
     * @param rec
     * @return
     */
    public RecordChange apply( T rec );

}
