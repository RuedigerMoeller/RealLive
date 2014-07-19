package org.nustaq.machnetz;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.machnetz.model.TestRecord;
import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.impl.RLImpl;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 25.05.14.
 */
public class MachNetz extends WebSocketHttpServer {

    // FIXME: need exception mode for blocking clients

    // don't buffer too much.
    public static int CLIENTQ_SIZE = 1000;
    public static int MAX_THREADS = 8;

    Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);
    private RealLive realLive;

    public MachNetz(File contentRoot) {
        super(contentRoot);
        initServer();
    }

    public RealLive getRealLive() {
        return realLive;
    }

    protected void initServer() {
        realLive = new RLImpl();

        initTestTable();
        initRLExchange();
        openMarket();
    }

    private void openMarket() {
        // fixme: should clear orders. trades
        RLTable market = realLive.getTable("Market");
        realLive.getTable("Instrument").stream().each((change) -> {
            if ( change.isAdd() ) {
                Instrument record = (Instrument) change.getRecord();
                market.$put(record.getRecordKey(), new Market(record.getRecordKey(),0,0,0,0,0,0,0,"n/a"));
            }
        });
    }

    private void initRLExchange() {
        Arrays.stream( new Class[] {
                TestRecord.class,
                Instrument.class,
                Market.class,
                Order.class,
                Trade.class,
                Trader.class
            }
        ).forEach( (clz) -> realLive.createTable(clz) );

        String description = "10€ in case $X wins";
        long expiry = System.currentTimeMillis()+4*7*24*60*60*1000;
        String expString = new Date(expiry).toString();
        Arrays.stream( new Instrument[] {
            new Instrument("Germany", description, expiry, expString),
            new Instrument("Italy", description, expiry, expString),
            new Instrument("Brazil", description, expiry, expString),
            new Instrument("France", description, expiry, expString),
            new Instrument("Greece", description, expiry, expString),
            new Instrument("Croatia", description, expiry, expString),
            new Instrument("Bavaria", description, expiry, expString),
            new Instrument("Belgium", description, expiry, expString),
            new Instrument("USA", description, expiry, expString),
            new Instrument("Argentina", description, expiry, expString),
            new Instrument("Cameroun", description, expiry, expString),
            new Instrument("Nigeria", description, expiry, expString),
            new Instrument("Netherlands", description, expiry, expString),
            new Instrument("Meckpomm", description, expiry, expString),
            new Instrument("Russia", description, expiry, expString),
            new Instrument("Iran", description, expiry, expString),
        }).forEach((instr) -> {
            instr.setDescription(instr.getDescription().replace("$X",instr.getRecordKey()));
            realLive.getTable("Instrument").$put(instr.getRecordKey(),instr);
        });

        Arrays.stream( new Trader[] {
            new Trader("Hans", "hans@wurst.de", 100),
            new Trader("Hubert", "hans@wurst.de", 300),
            new Trader("Ruedi", "hans@wurst.de", 200),
            new Trader("Hara", "hans@wurst.de", 2000),
            new Trader("Kiri", "hans@wurst.de", 3000),
            new Trader("Angela", "hans@wurst.de", 11),
            new Trader("Mutti", "hans@wurst.de", 10),
        }).forEach((trader) -> realLive.getTable("Trader").$put(trader.getRecordKey(),trader));

    }

    private void initTestTable() {
        realLive.createTable( "person", TestRecord.class );
        RLTable person = realLive.getTable("person");
        for ( int i = 0; i < 100; i++ ) {
            person.$put("ruedi"+i, new TestRecord("Möller", "Rüdiger", 1968, "m", "Key Business Management Consulting Agent (KBMCA)"));
            person.$put("other"+i, new TestRecord("Huber", "Heinz", 1988, "m", "Project Office Support Consultant"));
            person.$put("another"+i, new TestRecord("Huber", "Heinz", 1988, "m", "Back Office Facility Management Officer"));
        }
        ArrayList<String> keys = new ArrayList<>();
        person.stream().each( (change) -> {
            if ( change.isSnapshotDone() ) {
                Collections.shuffle(keys);
                new Thread(() -> {
                    while( true ) {
                        keys.stream().forEach((key) -> {
                            if (key.indexOf("ruedi") >= 0) {
                                TestRecord forUpdate = (TestRecord) getRealLive().getTable("person").createForUpdate(key, false);
                                forUpdate.setYearOfBirth((int) (1900 + Math.random() * 99));
                                forUpdate.setPreName("POK " + (int) (Math.random() * 5));
                                forUpdate.setName("Name " + (int) (Math.random() * 5));
                                forUpdate.$apply();
//                                LockSupport.parkNanos(1000*1000*100);
                                LockSupport.parkNanos(1000 * 1000 * 200);
                            }
                        });
                        LockSupport.parkNanos(1000*1000*100);
                    }
                }).start();
            } else {
                keys.add(change.getRecordKey());
            }
        });
    }

    @Override
    public void onHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, NettyWSHttpServer.HttpResponseSender sender) {
        super.onHttpRequest(ctx, req, sender);
    }

    @Override
    public void onOpen(ChannelHandlerContext ctx) {
        super.onOpen(ctx);
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onOpen without session");
        } else {
            session.$onOpen(ctx);
        }
    }

    @Override
    public void onClose(ChannelHandlerContext ctx) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onClose without session");
        } else {
            session.$onClose(ctx);
        }
    }

    @Override
    public void onTextMessage(ChannelHandlerContext ctx, String text) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onTextMessage without session");
        } else {
            session.$onTextMessage(ctx, text);
        }
    }

    @Override
    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onBinaryMessage without session");
        } else {
            session.$onBinaryMessage(ctx, buffer);
        }
    }

    @Override
    protected MNClientSession getSession(ChannelHandlerContext ctx) {
        return (MNClientSession) super.getSession(ctx);
    }

    AtomicInteger sessionid = new AtomicInteger(1);
    @Override
    protected ClientSession createNewSession() {
        MNClientSession session = Actors.AsActor(MNClientSession.class, clientScheduler );
        session.$init(this,sessionid.incrementAndGet());
        return session;
    }

    public static class CmdLine {
        @Parameter(names = {"-port", "-p"}, description = "port to listen")
        Integer port = 8887;

        @Parameter(names = {"-cr"}, description = "directory to serve files from")
        String contentRoot = ".";
    }

    public static void main(String[] args) throws Exception {
        CmdLine params = new CmdLine();
        new JCommander(params, args);
        new NettyWSHttpServer(params.port, new MachNetz(new File(params.contentRoot))).run();
    }

}
