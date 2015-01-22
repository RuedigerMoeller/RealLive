package org.nustaq.reallive;

import org.nustaq.kontraktor.Future;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.sys.metadata.Metadata;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 23.07.14.
 *
 * adds virtual set management. Can be used to create per client virtual sets.
 *
 */
public class RealLiveClientWrapper extends RealLive {

    RealLive realRealLive;
    ConcurrentHashMap<String,RLStream> virtualStreams = new ConcurrentHashMap<>();

    public RealLiveClientWrapper(RealLive realRealLive) {
        this.realRealLive = realRealLive;
    }

    @Override
    public Future $init() {
        return realRealLive.$init();
    }

    @Override
    public RLTable getTable(String tableId) {
        return realRealLive.getTable(tableId);
    }

    @Override
    public RLStream stream(String tableId) {
        RLStream rlStream = virtualStreams.get(tableId);
        if ( rlStream != null )
            return rlStream;
        return realRealLive.stream(tableId);
    }

    @Override
    public Metadata getMetadata() {
        return realRealLive.getMetadata();
    }

    @Override
    public Future createTable(String name, Class<? extends Record> recordClass) {
        return realRealLive.createTable(name,recordClass);
    }

    @Override
    public RealLive createTable(Class<? extends Record> recordClass) {
        realRealLive.createTable(recordClass);
        return this;
    }

    @Override
    public void createVirtualStream(String name, ReplicatedSet set) {
        set.setTableId(name);
        virtualStreams.put(name,set);
    }

    @Override
    public void shutDown() {
        realRealLive.shutDown();
    }

}
