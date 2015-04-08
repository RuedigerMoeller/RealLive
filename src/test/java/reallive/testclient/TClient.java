package reallive.testclient;

import org.nustaq.kontraktor.Actors;
import org.nustaq.reallive.Subscription;
import org.nustaq.reallive.client.ReplicatedSet;
import org.nustaq.reallive.impl.RLImpl;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.RLTable;
import org.nustaq.reallive.Record;

import static org.nustaq.kontraktor.Actors.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 06.07.14.
 *
 * modify data an verify concurrent subscriptions are consistent
 *
 */
public class TClient {

    public static final double FILTER_PRC = 1.8;

    public static class TCRecord extends Record {
        String instrument;

        double bidPrc, askPrc;
        int    bidQty, askQty;

        public boolean equals( Object rec ) {
            TCRecord b = (TCRecord) rec;
            return b.bidPrc == bidPrc && b.askPrc == askPrc && b.bidQty == bidQty && b.askQty == askQty && b.instrument.equals(instrument);
        }

        @Override
        public int hashCode() {
            return instrument.hashCode();
        }

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public double getBidPrc() {
            return bidPrc;
        }

        public void setBidPrc(double bidPrc) {
            this.bidPrc = bidPrc;
        }

        public double getAskPrc() {
            return askPrc;
        }

        public void setAskPrc(double askPrc) {
            this.askPrc = askPrc;
        }

        public int getBidQty() {
            return bidQty;
        }

        public void setBidQty(int bidQty) {
            this.bidQty = bidQty;
        }

        public int getAskQty() {
            return askQty;
        }

        public void setAskQty(int askQty) {
            this.askQty = askQty;
        }
    }


    public static class ClientActor extends Actor<ClientActor> {

        ReplicatedSet<TCRecord> replica = new ReplicatedSet<>();
        Subscription<TCRecord> subscription;

        public IPromise $run(RLTable<TCRecord> table ) {
            Thread.currentThread().setName("TCClient");
            Promise p = new Promise();
            replica.reset();
            long tim = System.currentTimeMillis();
            subscription = table.stream().subscribe((r) -> r.getAskPrc() > FILTER_PRC, (change) -> {
                replica.onChangeReceived(change);
                if ( change.isSnapshotDone() ) {
                    System.out.println("query duration "+(System.currentTimeMillis()-tim)+" result size:"+replica.getSize());
                    p.complete();
                }
            });
            return p;
        }

        public IPromise $checkCorrectness(RLTable<TCRecord> table) {
            Promise p = new Promise();
            ArrayList res = new ArrayList();
            table.stream().filter((r) -> r.getAskPrc() > FILTER_PRC, (change) -> {
                if (change.isAdd()) {
                    res.add(change.getRecord());
                }
                if (change.isSnapshotDone()) {
                    if (replica.getSize() != res.size())
                        p.complete(null, "Different size: " + replica.getSize() + " " + res.size());
                    else {
                        for (int i = 0; i < res.size(); i++) {
                            TCRecord rec = (TCRecord) res.get(i);
                            TCRecord tcRecord = replica.get(rec.getRecordKey());
                            if (!rec.equals(tcRecord)) {
                                p.complete(null, "diff: " + tcRecord + " \n    " + rec);
                                return;
                            }
                        }
                        System.out.println("comparision successfull");
                        p.complete(null, null);
                    }
                }
            });
            return p;
        }

        public IPromise $unsubscribe(RLTable<TCRecord> table) {
            table.stream().unsubscribe(subscription);
            return new Promise<>("void");
        }
    }

    static volatile int updateCount = 0;

    public static class TCMutator extends Actor<TCMutator> {
        ArrayList<IPromise<String>> recids;

        public IPromise $init(RLTable<TCRecord> table ) {
            Thread.currentThread().setName("TCMutator");
            recids = new ArrayList();

            ReplicatedSet<TCRecord> set = new ReplicatedSet<>();
            table.stream().filterUntil(null, (r,i) -> set.getSize() > 50000, set );
            set.onFinished( ()-> System.out.println("** Set size: "+set.getSize()) );

            for ( int i = 0; i < 500; i++) {
                TCRecord recordForAdd = table.createForAdd();
                recordForAdd.setAskPrc(1+Math.random());
                recordForAdd.setBidPrc(0+Math.random());
                recordForAdd.setBidQty(100);
                recordForAdd.setAskQty(110);
                recordForAdd.setInstrument("FDS "+i);
                recids.add(recordForAdd.$apply(0));
            }
            Promise res = new Promise();
            Actors.all((List) recids).then((r, e) -> {
                res.complete("void", null);
            });
            return res;
        }

        public IPromise $run(RLTable<TCRecord> table, int times, IPromise p) {
            if ( p == null )
                p = new Promise();
            for (int i = 0; i < recids.size(); i++) {
                if ( ((Actor) table).isMailboxPressured() )
                    break;
                IPromise<String> stringFuture = recids.get(i);
                TCRecord recordForUpdate = table.createForUpdate(stringFuture.get(), false);
                recordForUpdate.setAskPrc(1+Math.random());
                recordForUpdate.setBidPrc(0+Math.random());
                recordForUpdate.$apply(0);
                updateCount++;
            }

            if ( times > 0 ) {
                final IPromise finalP = p;
                delayed(1, () -> $run(table, times - 1, finalP));
            } else {
                p.complete();
            }
            return p;
        }

        public void $dumpTables() {
            schema.getTable("SysTable").stream().forEach(
                    (change) -> System.out.println(change.getRecord()));
            delayed(3000, () -> self().$dumpTables() );
        }

    }

    static RLImpl schema;
    public static void main( String arg[] ) {
        schema = new RLImpl().initSync();
        schema.createTable( "mkt", TCRecord.class );

        schema.getTable("SysTable").stream().forEach(
                (change) -> System.out.println(change.getRecord())
        );

        RLTable<TCRecord> table = schema.getTable("mkt");

        TCMutator mutator = AsActor(TCMutator.class);
        ClientActor client = AsActor(ClientActor.class);

        testRun(table, mutator, client);

    }

    private static void testRun(RLTable<TCRecord> table, TCMutator mutator, ClientActor client) {
        mutator.$init(table).await();

        client.$run(table);
        mutator.$run(table, 1000, null).await();

        table.$sync().await();
        client.$unsubscribe(table).await();
        client.$checkCorrectness(table).await();
        System.out.println("updated: "+updateCount+" NEXT RUN ..");
        updateCount = 0;
        table.$sync();
        testRun(table, mutator, client);
// same with chained futures
//        mutator.$init(table).then(() -> {
//            client.$run(table);
//            mutator.$run(table, 1000, null).then( () ->
//                table.$sync().then( () ->
//                    client.$unsubscribe(table).then( () ->
//                        client.$checkCorrectness(table).then((res, err) -> {
//                            if (err == null) {
//                                System.out.println("NEXT RUN");
//                                table.$sync();
//                                testRun(table, mutator, client);
//                            } else {
//                                System.out.println(err);
//                                System.exit(1);
//                            }
//                        })
//                    )
//                )
//            );
//        });
    }
}
