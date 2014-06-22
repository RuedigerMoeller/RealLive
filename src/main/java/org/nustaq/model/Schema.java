package org.nustaq.model;

import org.nustaq.serialization.FSTConfiguration;

/**
 * Created by ruedi on 21.06.14.
 */
public class Schema {

    protected FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    public FSTConfiguration getConf() {
        return conf;
    }

    public byte[] toByte( Object o ) {
        return conf.asByteArray((java.io.Serializable) o);
    }

    public Object fromByte( byte b[] ) {
        return conf.asObject(b);
    }

}
