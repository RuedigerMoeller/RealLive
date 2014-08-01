package org.nustaq.machnetz;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.*;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.machnetz.model.TestModel;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.machnetz.model.rlxchange.Position;
import org.nustaq.machnetz.model.rlxchange.Trade;
import org.nustaq.reallive.*;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.impl.SubscriptionImpl;
import org.nustaq.reallive.queries.JSQuery;
import org.nustaq.reallive.sys.messages.Invocation;
import org.nustaq.reallive.sys.messages.InvocationCallback;
import org.nustaq.reallive.sys.messages.QueryTuple;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MBPrinter;
import org.nustaq.webserver.ClientSession;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
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
    RealLive realLive;
    String traderKey;

    public void $init(MachNetz machNetz, int sessionId) {
        Thread.currentThread().setName("MNClientSession"+sessionId);
        server = machNetz;
        lookup = MethodHandles.lookup();
        realLive = new RealLiveClientWrapper(server.getRealLive());
    }

    protected RealLive getRLDB() {
        return realLive;
    }

    @CallerSideMethod
    public int getSessionId() { return getActor().sessionId; }

    public void $onOpen(ChannelHandlerContext ctx) {
        checkThread();
    }

    public void $onClose(ChannelHandlerContext ctx) {
        checkThread();
        subscriptions.keySet().forEach((subsid) -> unsubscribe(subsid));
        self().$stop();
        server.removeSession(ctx);
    }

    public void $onTextMessage(ChannelHandlerContext ctx, String text) {
        System.out.println("textmsg");
    }

    final MethodType rpctype = MethodType.methodType(Object.class,Invocation.class);
    public void $onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        checkThread();
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

    Object login(Invocation inv) {
        if ( traderKey != null )
        {
            sendReply(inv, "session already logged in.");
        }
        try {
            List argument = (List) inv.getArgument();
            realLive.getTable("Trader").$get((String) argument.get(0)).then((rec, e) -> {
                try {
                    if (rec != null) {
                        traderKey = ((Record) rec).getRecordKey();
                        if (traderKey != null) {
                            initPositionStream();
                            sendReply(inv, "success");
                        }
                    } else {
                        sendReply(inv, "Invalid user or password.");
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                    sendReply(inv, "Login failure "+ee.getMessage());
                }
            });
        } catch (Exception ex) {
            sendReply(inv, "Invalid user or password [EX].");
        }
        return NO_RESULT;
    }

    HashMap<String,Position> positionMap = new HashMap<>();
    private void initPositionStream() {
        ReplicatedSet<Position> myPosition = new ReplicatedSet<>();
        realLive.createVirtualStream("Position",myPosition);
        FSTClazzInfo classInfo = realLive.getConf().getClassInfo(Position.class);
        // subscribe + install virtual streams
        realLive.stream("Trade").subscribe(
            (record) -> {
                Trade trade = (Trade) record;
                boolean isBuyer = trade.getBuyTraderKey().equals(traderKey);
                boolean isSeller = trade.getSellTraderKey().equals(traderKey);
                return ( isBuyer || isSeller );
            },
            (change) -> {
                if (change.isAdd()) {
                    Trade trade = (Trade) change.getRecord();
                    boolean isBuyer = trade.getBuyTraderKey().equals(traderKey);
                    boolean isSeller = trade.getSellTraderKey().equals(traderKey);
                    if (isBuyer) {
                        processBSPosition(myPosition, classInfo, trade, isBuyer);
                    }
                    if ( isSeller ) {
                        processBSPosition(myPosition, classInfo, trade, !isSeller);
                    }
                } else if (!change.isSnapshotDone()) {
                    throw new RuntimeException("Wat ?");
                }
            }
        );
    }

    private void processBSPosition(ReplicatedSet<Position> myPosition, FSTClazzInfo classInfo, Trade trade, boolean isBuyer) {
        Position p = positionMap.get(trade.getInstrumentKey());
        if (p == null) {
            p = new Position();
            p.setInstrKey(trade.getInstrumentKey());
            positionMap.put(trade.getInstrumentKey(), p);
            p._setId(trade.getInstrumentKey() + "#" + traderKey);
            p.prepareForUpdate(false, classInfo);
            myPosition.onChangeReceived(ChangeBroadcast.NewAdd("Position", p, 0));
        }
        int oldQty = p.getQty();
        int oldSum = p.getSumPrice();
        if (isBuyer) {
            p.setQty(oldQty + trade.getTradeQty());
            p.setSumPrice(oldSum - trade.getTradeQty()*trade.getTradePrice());
        } else {
            p.setQty(oldQty - trade.getTradeQty());
            p.setSumPrice(oldSum + trade.getTradeQty()*trade.getTradePrice());
        }
        p.updateAvg();
        myPosition.onChangeReceived(p.computeBcast("Position", 0));
    }

    Object deleteOrder(Invocation inv) {
        Order ord = (Order) inv.getArgument();
        server.getMatcher().$delOrder(ord).then((r,e) -> sendReply(inv,r));
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
        server.getMatcher().$addOrder(toAdd).then( (r,e) -> {
            sendReply(inv, r != null ? r : "");
        });
        return NO_RESULT;
    }

    private void unsubscribe(String subsId) {
        Subscription subs = subscriptions.get(subsId);
        if ( subs != null ) {
            getRLDB().stream(subs.getTableKey()).unsubscribe(subs);
            subscriptions.remove(subsId);
        }
    }

    //////////// stream api ///////////////////////////////////////////////////////////////

    HashMap<String, Subscription> subscriptions = new HashMap<>();

    Object unsubscribe(Invocation inv) {
        String subsId = ""+inv.getArgument();
        unsubscribe(subsId);
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
