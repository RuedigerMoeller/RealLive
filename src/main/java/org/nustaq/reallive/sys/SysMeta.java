package org.nustaq.reallive.sys;

import minbin.gen.GenMeta;
import org.nustaq.reallive.sys.messages.AuthRequest;
import org.nustaq.reallive.sys.messages.AuthResponse;

/**
 * Created by ruedi on 07.07.14.
 */
public class SysMeta implements GenMeta {
    @Override
    public Class[] getClasses() {
        return new Class[] {
            SysTable.class, // has to be first always
            AuthRequest.class,
            AuthResponse.class,
            ClusterClients.class,
        };
    }
}
