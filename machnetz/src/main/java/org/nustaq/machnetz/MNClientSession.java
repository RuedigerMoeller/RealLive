package org.nustaq.machnetz;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.*;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.reallive.sys.SysMeta;
import org.nustaq.reallive.sys.messages.Invocation;
import org.nustaq.reallive.sys.messages.InvocationCallback;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.webserver.ClientSession;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Created by ruedi on 25.05.14.
 */
public class MNClientSession<T extends MNClientSession> extends Actor<T> implements ClientSession {

    static FSTConfiguration conf = FSTConfiguration.createCrossPlatformConfiguration();
    static {
        conf.registerCrossPlatformClassMappingUseSimpleName(new SysMeta().getClasses());
    }

    protected MachNetz server; // FIXME: iface

    int sessionId;
    MethodHandles.Lookup lookup;

    public void $init(MachNetz machNetz, int sessionId) {
        server = machNetz;
        lookup = MethodHandles.lookup();
    }

    @CallerSideMethod
    public int getSessionId() { return getActor().sessionId; }

    public void $onOpen(ChannelHandlerContext ctx) {

    }

    public void $onClose(ChannelHandlerContext ctx) {
        self().$stop();
    }

    public void $onTextMessage(ChannelHandlerContext ctx, String text) {
        System.out.println("textmsg");
    }

    final MethodType rpctype = MethodType.methodType(Object.class,Invocation.class);
    public void $onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        System.out.println("minmsg");
        final Object msg = conf.asObject(buffer);
        if (msg instanceof Invocation) {
            final Invocation inv = (Invocation) msg;
            try {
                final MethodHandle method = lookup.findVirtual(getClass(), inv.getName(), rpctype);
                Object invoke = method.invoke(this, inv);
                if ( ! "0".equals(inv.getCbId()) ) {
                    InvocationCallback cb = new InvocationCallback(invoke,inv.getCbId());
                    server.sendWSBinaryMessage(ctx,conf.asByteArray(cb));
                }
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.printf("unable to find method " + inv.getName() + "( " + inv.getClass() + " )");
            }
        }
    }

    Object initModel(Invocation inv) {
        System.out.println("Called method initModel !!!");
        return "Yep";
    }

}
