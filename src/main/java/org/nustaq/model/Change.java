package org.nustaq.model;

/**
 * Created by ruedi on 21.06.14.
 */
public interface Change<K,T extends Record> {

    public String getTableId();
    public K getId();
    public RecordChange apply( T rec );

}
