package org.nustaq.machnetz;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.machnetz.model.rlxchange.*;
import org.nustaq.machnetz.rlxchange.Matcher;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.impl.RLImpl;
import org.nustaq.reallive.sys.config.ColumnConfig;
import org.nustaq.reallive.sys.config.ConfigReader;
import org.nustaq.reallive.sys.config.SchemaConfig;
import org.nustaq.reallive.sys.config.TableConfig;
import org.nustaq.serialization.dson.Dson;
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
    public static int MAX_THREADS = 1;

    Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);
    private RealLive realLive;
    Matcher matcher;

    public MachNetz(File contentRoot) {
        super(contentRoot);
        initServer();
    }

    public RealLive getRealLive() {
        return realLive;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    protected void initServer() {
        realLive = new RLImpl();

        initRLExchange();
        openMarket();
    }

    private void openMarket() {
        // fixme: should clear orders. trades
        RLTable market = realLive.getTable("Market");
        realLive.getTable("Instrument").stream().each((change) -> {
            if ( change.isAdd() ) {
                Instrument record = (Instrument) change.getRecord();
                market.$put(record.getRecordKey(), new Market(record.getRecordKey(),0,0,0,0,0,0,0,"n/a"), 0);
            }
        });
    }

    private void initRLExchange() {
        Arrays.stream( new Class[] {
                Instrument.class,
                Market.class,
                Order.class,
                Trade.class,
                Trader.class,
                Position.class,
                Session.class,
                Asset.class
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
            realLive.getTable("Instrument").$put(instr.getRecordKey(),instr,0);
        });

        Arrays.stream( new Trader[] {
            new Trader("Hans", "hans@wurst.de"),
            new Trader("Hubert", "hans@wurst.de"),
            new Trader("Ruedi", "hans@wurst.de"),
            new Trader("Hara", "hans@wurst.de"),
            new Trader("Kiri", "hans@wurst.de"),
            new Trader("Angela", "hans@wurst.de"),
            new Trader("Mutti", "hans@wurst.de"),
            new Trader("Feed0", "hans@wurst.de"),
            new Trader("Feed1", "hans@wurst.de"),
        }).forEach((trader) -> {
            realLive.getTable("Trader").$put(trader.getRecordKey(),trader,0);
            realLive.getTable("Asset").$put(trader.getRecordKey()+"#cash",new Asset(trader.getRecordKey()+"#cash",trader.getRecordKey().startsWith("Feed")?Integer.MAX_VALUE : 30000),0);
        });

        realLive.stream("Trader").each( (change) -> {
            if ( change.isAdd() ) {
                Trader t = (Trader) change.getRecord();

            }
        });

        realLive.getTable("Instrument").$sync().then((r,e) -> {

            realLive.getTable("Instrument").stream().each( change -> {
                if ( change.isSnapshotDone() ) {
                    matcher = Actors.AsActor(Matcher.class);
                    matcher.$init(realLive);

                    Feeder feeder = Actors.AsActor(Feeder.class);
                    feeder.$feed0(realLive,matcher);
                } else {
                    System.out.println("instr change "+change.getRecord());
                }
            });

        });

        try {
            ConfigReader.init();
            SchemaConfig c = new SchemaConfig();
            TableConfig tc = new TableConfig();
            ColumnConfig cf = new ColumnConfig();
            cf.setBgColor("#fff");
            tc.getColumns().put("bcColor",cf);
            c.getTables().put("SysTable",tc);
            String s = Dson.getInstance().writeObject(c);
            System.out.println(s);

            if ( new File("./annotations.dson").exists() ) {
                SchemaConfig schemaConfig = ConfigReader.readConfig("./annotations.dson");
                System.out.println("read config");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    volatile static boolean stopF = false;
    public static void stopFeed() {
        stopF = true;
    }
    public static class Feeder extends Actor<Feeder> {

        public void $feed0( RealLive rl, Matcher m ) {
            Thread.currentThread().setName("Feeder");
            delayed(5000, () -> self().$feed(rl,m));
        }

        int orderCount = 0;
        public void $feed( RealLive rl, Matcher m ) {
            RLTable<Instrument> instr = rl.getTable("Instrument");
            instr.stream().each((change) -> {
                if ("USA".equals(change.getRecordKey())) {
                    return;
                }
                if ( orderCount > 1000 ) {
                    RLTable orTable = rl.getTable("Order");
                    orTable.stream().each((delChange) -> {
                        if ( delChange.isAdd() ) {
                            String text = ((Order) delChange.getRecord()).getText();
                            if (text != null && text.startsWith("Feeder")) {
                                orTable.$remove(delChange.getRecordKey(), 2);
                            }
                        }
                        if ( delChange.isSnapshotDone() ) {
                            orderCount = 0;
                        }
                    });
                } else {
                    if ( change.isAdd() ) {
                        double rand = 1 - (change.getRecordKey().charAt(0) - 65) / 100.0;
                        if ( Math.random() < rand ) {
                            Instrument instrument = change.getRecord();
                            //                        Order newOrder = (Order) rl.getTable("Order").createForAddWithKey();
                            Order newOrder = new Order();
                            newOrder.setInstrumentKey(instrument.getRecordKey());
                            boolean isBuy = Math.random() > .5;
                            if (isBuy) {
                                newOrder.setBuy(isBuy);
                                newOrder.setQty((int) (Math.random() * 70 + 50));
                                newOrder.setLimitPrice((int) (Math.random() * 888 + 1));
                            } else {
                                newOrder.setBuy(isBuy);
                                newOrder.setQty((int) (Math.random() * 70 + 50));
                                newOrder.setLimitPrice((int) (Math.random() * 500 + 500));
                            }
                            switch ((int) (Math.random() * 2)) {
                                case 0:
                                    newOrder.setTraderKey("Feed0");
                                    break;
                                case 1:
                                    newOrder.setTraderKey("Feed1");
                                    break;
                            }
                            newOrder.setCreationTime(System.currentTimeMillis());
                            int len = (int) (Math.random() * 20 + 1);
                            String t = "Feeder ";
                            for (int i = 0; i < len; i++)
                                t += " poaskdpaokds";
                            newOrder.setText(t);
                            // newOrder.$apply(2);
                            m.$addOrder(newOrder);
                            orderCount++;
                        }
                    }
                }
            });
            if ( ! stopF ) {
                delayed(2000, () -> self().$feed(rl,m));
            }
        }

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
