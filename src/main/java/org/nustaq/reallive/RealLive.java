package org.nustaq.reallive;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.serialization.FSTConfiguration;

import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 */
public abstract class RealLive {

    protected FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    protected String dataDirectory = "/tmp/reallive";

    public FSTConfiguration getConf() {
        return conf;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public abstract IPromise $init();

    public abstract RLTable getTable(String tableId);
    public abstract RLStream stream(String tableId);
    public abstract Metadata getMetadata();

    public abstract IPromise createTable(String name, Class<? extends Record> recordClass);
    public abstract RealLive createTable(Class<? extends Record> recordClass);
    public abstract void createVirtualStream(String name, ReplicatedSet set);

    public abstract void shutDown();
}
