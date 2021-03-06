package reallive;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.reallive.impl.RLImpl;
import org.nustaq.reallive.impl.RLTableImpl;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.RLStream;
import org.nustaq.reallive.RLTable;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.reallive.impl.storage.TestRec;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 21.06.14.
 */
public class TableTest {

    private String longString = "pasodkapsodkapsdokapsdoka aspdok aspdoak dpask dlfdknlfgnj " +
                                "pasodkapsodkapsdokapsdoka aspdok aspdoak dpask dlfdknlfgnj " +
                                "pasodkapsodkapsdokapsdoka aspdok aspdoak dpask dlfdknlfgnj " +
                                "eiruhpefo psdfsko fpsodkf psdokf ldskf ld";

    @Test
    public void machVollSync() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();;
        RLTableImpl<TestRec> test = Actor.AsActor(RLTableImpl.class);
        test.$init("test",schema, TestRec.class, null);
        TestRec newRec = new TestRec(null, test);
        long tim = System.currentTimeMillis();
        int MAX = 5*1000000;
        for ( int i = 0; i < MAX; i++ ) {
            newRec.setX(i);
            int finalI = i;
            test.$addGetId(newRec,0);
            if ( (finalI%1000) == 0 ) {
                System.out.println("adding .. "+finalI );
            }
        }
        test.ping().await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void addExpandQuery() throws InterruptedException {
        CountDownLatch testLatch = new CountDownLatch(1);
        new File("/tmp/reallive/test1.mmf").delete();
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test1", TestRec.class );
        RLTable<TestRec> table = schema.getTable("test1");
        RLStream<TestRec> stream = table.stream();

        TestRec add0 = table.createForAdd();
        TestRec add1 = table.createForAdd();

        IPromise<String> k1 = table.$addGetId(add1,0);
        IPromise<String> k0 = table.$addGetId(add0,0);

        Actors.all(k0, k1).then( (_r,e) -> {

            TestRec forUpdate1 = table.createForUpdate(k1.get(), false);
            forUpdate1.setAnother("sodi");
            forUpdate1.$apply(0);

            forUpdate1.setAnother("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            forUpdate1.$apply(0);

            forUpdate1.setAnother("asd");
            forUpdate1.$apply(0);

            forUpdate1.setAnother("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab");
            forUpdate1.$apply(0);

            TestRec forUpdate = table.createForUpdate(k0.get(), false);
            forUpdate.setAnother("sodifjsodifjsodifjsodifjsodifjsodijfsodifjsoidfjsoidfjsoidfjsofijdsodijfsodfijsoijfd");
            forUpdate.$apply(0);

            forUpdate.setAnother("sod");
            forUpdate.$apply(0);

            forUpdate.setAnother("sodaosidjaosidjaosidjaosdijweofdfkgjdkfgjhdkfghjdfkgjdfkvbjdvksdvkdscbnkscdnksdbjcsdf");
            forUpdate.$apply(0);

            forUpdate.setAnother("pok");

            forUpdate.$apply(0).then( (key1,e1) -> {
                System.out.println("org " + k0 + " applied " + key1);
                stream.forEach((change) -> {
                    if (change.isSnapshotDone()) {
                        testLatch.countDown();
                    } else if (change.isError()) {
                        System.out.println("ERROR");
                    } else
                        System.out.println(change.getRecord());
                });
            });
        });
        testLatch.await();
    }

    @Test
    public void machVoll() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();;
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> table = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("record size:"+ FSTConfiguration.getDefaultConfiguration().asByteArray(table.createForAdd()).length);


        long tim = System.currentTimeMillis();
        int MAX = 1000000;
//        int MAX = 200000;
        for ( int i = 0; i < MAX; i++ ) {
            TestRec newRec = table.createForAdd();
            while ( ((RLTableImpl)table).__mailbox.size() > 10000 ) {
                LockSupport.parkNanos(1);
            }
            newRec.setX(i);
            int finalI = i;
            table.$addGetId(newRec,0).then((r, e) -> {
                if ((finalI % 1000) == 0) {
                    System.out.println("adding .. " + finalI);
                }
                if (finalI == MAX - 1)
                    latch.countDown();
            });
        }
        latch.await();

        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void update() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
//        while( true )
            mutateOnce(test);
    }

    private void mutateOnce(RLTable<TestRec> table) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
        int MAX = 100000;
//        int MAX = 1000;
        table.stream().filterUntil(
            (rec) -> true,
            (rec,i) -> i >= MAX,
            (r) -> {
                if (r.isSnapshotDone()) {
                    latch.countDown();
                } else if (r.isError()) {
                    System.out.println("ERROR---------------");
                } else {
                    TestRec record = r.getRecord();// fixme: should clone
                    table.prepareForUpdate(record);
                    record.setName(longString.substring((int) (longString.length() * Math.random())));
//                    r.setName(longString.substring(0,(int) (20 * Math.random())));
                    record.$apply(0);
                }
            }
                                     );
        latch.await();
        long dur = System.currentTimeMillis() - tim + 1;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void query() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");

//        while( true )
            oneQLoop(test);
//        Thread.sleep(100000);
    }

    private void oneQLoop(RLTable<TestRec> test) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 100000;
        test.stream().filterUntil(
            (rec) -> true,
            (rec,i) -> i >= MAX,
            (r) -> {
                if (r.isSnapshotDone())
                    latch.countDown();
                if (r.isError()) {
                    System.out.println("ERROR");
                    System.out.println("count " + MAX);
                }
            }
                                    );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void queryBinary() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");

//        while( true )
            oneQBinaryLoop(test);
//        Thread.sleep(100000);
    }

    private void oneQBinaryLoop(RLTable<TestRec> test) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 100000;
        int count[] = {0};
        test.stream().filterBinary(
            (rec) -> {
                return true;
            },
            (rec) -> count[0]++ >= MAX,
            (r, e) -> {
                if (e == RLTable.END)
                    latch.countDown();
                if (e instanceof Exception) {
                    System.out.println("count " + count[0]);
                    ((Exception) e).printStackTrace();
                }
            }
                                     );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
    }

    @Test
    public void queryNoMatch() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");

//        Thread.sleep(20000);
        System.out.println("start");

        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 1000000;
        int count[] = {0};
        test.stream().filterUntil(
            (rec) -> false,
            (rec,i) -> i >= MAX,
            (r) -> {
                if (r.isSnapshotDone())
                    latch.countDown();
            }
        );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
        Thread.sleep(5000);
    }

    @Test
    public void testBasics() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
        TestRec newRec = new TestRec(null, test);
        IPromise<String> res[] = new IPromise[10];
        CountDownLatch latch = new CountDownLatch(1);
        for ( int i = 0; i < 10; i++ ) {
            newRec.setX(i);
            res[i] = test.$addGetId(newRec,0);
        }
        Actors.all(res).then( (r, e) -> {
            for (int i = 0; i < r.length; i++) {
                IPromise future = r[i];
                System.out.println("key: '" + future.get() + "'");
            }
        }).then((r,e) ->
            test.$get("test:a").then( (r1,e1) -> System.out.println(r1) ).then( (r2,e2)-> latch.countDown() )
        );

        latch.await();
    }

    @Test
    public void testRepeatedRead() throws InterruptedException {
        RLImpl schema = new RLImpl().initSync();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        test.$get("test:a")
            .then( (r1,e1) -> System.out.println(r1) )
            .then( (r2,e2) -> latch.countDown() );

        latch.await();

    }

}
