package org.nustaq.machnetz;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.machnetz.model.TestRecord;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.RealLive;
import org.nustaq.reallive.Record;
import org.nustaq.reallive.impl.RLImpl;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
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
        realLive.createTable( "person", TestRecord.class );
        RLTable person = realLive.getTable("person");
        person.$put("ruedi", new TestRecord("Möller", "Rüdiger", 1968, "m", "Key Business Management Consulting Agent (KBMCA)"));
        person.$put("other", new TestRecord("Huber", "Heinz", 1988, "m", "Project Office Support Consultant"));
        person.$put("another", new TestRecord("Huber", "Heinz", 1988, "m", "Back Office Facility Management Officer"));
        person.$put("ruedi1", new TestRecord("Möller", "Rüdiger", 1968, "m", "Key Business Management Consulting Agent (KBMCA)"));
        person.$put("other2", new TestRecord("Huber", "Heinz", 1988, "m", "Project Office Support Consultant"));
        person.$put("another2", new TestRecord("Huber", "Heinz", 1988, "m", "Back Office Facility Management Officer"));
        new Thread(() -> {
            while( true ) {
                TestRecord forUpdate = (TestRecord) getRealLive().getTable("person").createForUpdate("ruedi", false);
                forUpdate.setYearOfBirth((int) (1900+Math.random()*99));
                forUpdate.$apply();
                LockSupport.parkNanos(1000*1000*1000);
            }
        }).start();
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
