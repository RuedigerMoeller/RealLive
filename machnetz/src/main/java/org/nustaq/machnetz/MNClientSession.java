package org.nustaq.machnetz;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
import org.nustaq.machnetz.model.DataModel;
import org.nustaq.machnetz.model.rlxchange.Order;
import org.nustaq.machnetz.model.rlxchange.Position;
import org.nustaq.machnetz.model.rlxchange.Session;
import org.nustaq.machnetz.model.rlxchange.Trade;
import org.nustaq.reallive.*;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.queries.JSQuery;
import org.nustaq.reallive.sys.config.ConfigReader;
import org.nustaq.reallive.sys.config.SchemaConfig;
import org.nustaq.reallive.sys.messages.Invocation;
import org.nustaq.reallive.sys.messages.InvocationCallback;
import org.nustaq.reallive.sys.messages.QueryTuple;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MBPrinter;
import org.nustaq.webserver.ClientSession;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 25.05.14.
 */
public class MNClientSession<T extends MNClientSession> extends ActorWSClientSession<T> implements ClientSession {

    private static final Object NO_RESULT = "NO_RESULT";
    static FSTConfiguration conf = FSTConfiguration.createCrossPlatformConfiguration();
    static {
        conf.registerCrossPlatformClassMappingUseSimpleName(new DataModel().getClasses());
    }

    protected MachNetz mnserver; // FIXME: iface

    String sessionKey;
    Session session;
    MethodHandles.Lookup lookup;
    RealLive realLive;
    String traderKey;

    public void $init(ActorWSServer machNetz, int sessionId) {
        super.$init(machNetz, sessionId);
        Thread.currentThread().setName("MNClientSession"+sessionId);
        mnserver = (MachNetz) machNetz;
        lookup = MethodHandles.lookup();
        realLive = new RealLiveClientWrapper(mnserver.getRealLive());
        session = (Session) realLive.getTable("Session").createForAddWithKey(sessionKey);
        session.setLastPing(System.currentTimeMillis());
        session.$apply(0).then((recordId, err) -> {
            sessionKey = recordId;
            realLive.getTable("Session").prepareForUpdate(session);
            $updateSession();
        });
    }

    int SESSION_TIMOUT = 120000;
    public void $updateSession() {
        if ( System.currentTimeMillis() - session.getLastPing() > SESSION_TIMOUT) {
            $onClose();
        } else {
            session.setSubscriptions(subscriptions.size());
            session.$apply(0);
            delayed(10000, () -> self().$updateSession());
        }
    }

    protected RealLive getRLDB() {
        return realLive;
    }

    public void $onOpen(ChannelHandlerContext ctx) {
        checkThread();
        super.$onOpen(ctx);
    }

    public void $onClose() {
        System.out.println("closing session "+sessionKey);
        checkThread();
        ArrayList subs = new ArrayList(subscriptions.keySet());
        subs.forEach((subsid) -> unsubscribe((String) subsid));
        getRLDB().getTable("Session").$remove(sessionKey, 0);
        self().$stop();
        mnserver.removeSession(context);
    }

    public void $onTextMessage(String text) {
        System.out.println("textmsg");
    }

    final MethodType rpctype = MethodType.methodType(Object.class,Invocation.class);
    public void $onBinaryMessage(byte[] buffer) {
        checkThread();
        session.setRequests(session.getRequests()+1);
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
            inv.setCurrentContext(context);
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
        session.setBcasts(session.getBcasts()+1);
        String cbId = inv.getCbId();
        InvocationCallback cb = new InvocationCallback(msg, cbId);
        cb.setSequence(msgCount.incrementAndGet());
        final byte[] b = conf.asByteArray(cb);
        sendBinaryMessage(b);
    }

    Object initModel(Invocation inv) {
        System.out.println("Called method initModel !!!");
        Metadata metadata = FSTConfiguration.getDefaultConfiguration().deepCopy( getRLDB().getMetadata() );
        try {
            if ( new File("./annotations.dson").exists() ) {
                SchemaConfig schemaConfig = ConfigReader.readConfig("./annotations.dson");
                System.out.println("read config");
                metadata.overrideWith(schemaConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
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
                            session.setTraderKey(traderKey);
                            session.setLoginTime(DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())));
                            session.$apply(0);
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
            p._setRecordKey(trade.getInstrumentKey() + "#" + traderKey);
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
        mnserver.getMatcher().$delOrder(ord).then((r,e) -> sendReply(inv,r));
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
        mnserver.getMatcher().$addOrder(toAdd).then( (r,e) -> {
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

    Object ping(Invocation inv) {
        session.setLastPing(System.currentTimeMillis());
        return NO_RESULT;
    }

}
