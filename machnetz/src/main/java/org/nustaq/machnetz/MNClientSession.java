package org.nustaq.machnetz;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.*;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.machnetz.model.TestModel;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.Subscription;
import org.nustaq.reallive.queries.JSQuery;
import org.nustaq.reallive.sys.SysMeta;
import org.nustaq.reallive.sys.messages.Invocation;
import org.nustaq.reallive.sys.messages.InvocationCallback;
import org.nustaq.reallive.sys.messages.QueryTuple;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MBPrinter;
import org.nustaq.serialization.minbin.MinBin;
import org.nustaq.webserver.ClientSession;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 25.05.14.
 */
public class MNClientSession<T extends MNClientSession> extends Actor<T> implements ClientSession {

    private static final Object NO_RESULT = "NO_RESULT";
    static FSTConfiguration conf = FSTConfiguration.createCrossPlatformConfiguration();
    static {
        conf.registerCrossPlatformClassMappingUseSimpleName(new TestModel().getClasses());
    }

    protected MachNetz server; // FIXME: iface

    int sessionId;
    MethodHandles.Lookup lookup;

    public void $init(MachNetz machNetz, int sessionId) {
        server = machNetz;
        lookup = MethodHandles.lookup();
    }

    protected RealLive getRLDB() {
        return server.getRealLive();
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
//        System.out.println("minmsg");
        final Object msg;
        try {
            msg = conf.asObject(buffer);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.out.println("received:");
            MBPrinter.printMessage(buffer);
            return;
        }
//        System.out.println("  minmsg "+msg);
        if (msg instanceof Invocation) {
            final Invocation inv = (Invocation) msg;
            inv.setCurrentContext(ctx);
            try {
                final MethodHandle method = lookup.findVirtual(getClass(), inv.getName(), rpctype);
                Object result = method.invoke(this, inv);
                if ( ! "0".equals(inv.getCbId()) && result != NO_RESULT ) {
                    String cbId = inv.getCbId();
                    sendReply(inv, result);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.printf("unable to find method " + inv.getName() + "( " + inv.getClass() + " )");
            }
        }
    }

    AtomicInteger msgCount = new AtomicInteger(1);
    protected void sendReply(Invocation inv, Object msg) {
        String cbId = inv.getCbId();
        ChannelHandlerContext ctx = (ChannelHandlerContext) inv.getCurrentContext();
        InvocationCallback cb = new InvocationCallback(msg, cbId);
        cb.setSequence(msgCount.incrementAndGet());
        server.sendWSBinaryMessage(ctx,conf.asByteArray(cb));
    }

    Object initModel(Invocation inv) {
        System.out.println("Called method initModel !!!");
        return getRLDB().getMetadata();
    }

    Object streamTable(Invocation inv) {
        getRLDB().stream("" + inv.getArgument()).each((change) -> sendReply(inv, change));
        return NO_RESULT;
    }

    Object addOrder(Invocation inv) {
        RLTable<Order> order = getRLDB().getTable("Order");
        Order toAdd = (Order) inv.getArgument();
        if ( "stop".equals(toAdd.getText() ) ) {
            MachNetz.stopFeed();
            return NO_RESULT;
        }
        toAdd.setCreationTime(System.currentTimeMillis());
        order.$addGetId(toAdd).then((orderId,e)->sendReply(inv,orderId==null ? e:orderId));
        return NO_RESULT;
    }

    //////////// stream api ///////////////////////////////////////////////////////////////

    HashMap<String, Subscription> subscriptions = new HashMap<>();

    Object unsubscribe(Invocation<String> inv) {
        String subsId = inv.getArgument();
        Subscription subs = subscriptions.get(subsId);
        if ( subs != null ) {
            getRLDB().stream(subs.getTableKey()).unsubscribe(subs);
            subscriptions.remove(subsId);
        }
        return NO_RESULT;
    }

    // expect [tableName,recordkey]
    Object subscribeKey(Invocation<QueryTuple> inv) {
        QueryTuple argument = inv.getArgument();
        Subscription subs = getRLDB().stream("" + argument.getTableName()).subscribeKey(argument.getQuerySource(), (change) -> sendReply(inv, change));
        subscriptions.put(inv.getCbId(),subs);
        return NO_RESULT;
    }

    // expect [tableName,filterString]
    Object subscribe(Invocation<QueryTuple> inv) {
        QueryTuple argument = inv.getArgument();
        Subscription subs = getRLDB().stream("" + argument.getTableName()).subscribe( new JSQuery(argument.getQuerySource()), (change) -> sendReply(inv, change));
        subscriptions.put(inv.getCbId(),subs);
        return NO_RESULT;
    }

    // expect [tableName,filterString]
    Object listen(Invocation<QueryTuple> inv) {
        QueryTuple argument = inv.getArgument();
        Subscription subs = getRLDB().stream("" + argument).listen(new JSQuery(argument.getQuerySource()), (change) -> sendReply(inv, change));
        subscriptions.put(inv.getCbId(), subs);
        return NO_RESULT;
    }

    // expect [tableName,filterString]
    Object query(Invocation<QueryTuple> inv) {
        getRLDB().stream("" + inv.getArgument()).filter(new JSQuery(inv.getArgument().getQuerySource()),(change) -> sendReply(inv, change));
        return NO_RESULT;
    }

}
