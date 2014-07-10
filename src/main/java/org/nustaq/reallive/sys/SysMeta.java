package org.nustaq.reallive.sys;

import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.sys.messages.InvocationCallback;
import org.nustaq.reallive.sys.metadata.ColumnMeta;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.reallive.sys.metadata.TableMeta;
import org.nustaq.reallive.sys.tables.ClusterClients;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.minbin.GenMeta;
import org.nustaq.reallive.sys.messages.AuthRequest;
import org.nustaq.reallive.sys.messages.AuthResponse;
import org.nustaq.reallive.sys.messages.Invocation;

/**
 * Created by ruedi on 07.07.14.
 */
public class SysMeta implements GenMeta {
    @Override
    public Class[] getClasses() {
        return new Class[] {
            SysTable.class,
            Invocation.class,
            AuthRequest.class,
            AuthResponse.class,
            ClusterClients.class,
            InvocationCallback.class,
            ChangeBroadcast.class,
            Metadata.class,
            TableMeta.class,
            ColumnMeta.class
        };
    }
}
