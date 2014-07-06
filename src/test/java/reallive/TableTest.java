package reallive;

import org.junit.Test;
import org.nustaq.reallive.impl.RLSchema;
import org.nustaq.reallive.impl.RLTableImpl;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
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
        RLSchema schema = new RLSchema();
        RLTableImpl<TestRec> test = new RLTableImpl<>();
        test.$init("test",schema, TestRec.class, null);
        TestRec newRec = new TestRec(null, test);
        long tim = System.currentTimeMillis();
        int MAX = 5*1000000;
        for ( int i = 0; i < MAX; i++ ) {
            newRec.setX(i);
            int finalI = i;
            test.$addGetId(newRec);
            if ( (finalI%1000) == 0 ) {
                System.out.println("adding .. "+finalI );
            }
        }
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void addExpandQuery() throws InterruptedException {
        CountDownLatch testLatch = new CountDownLatch(1);
        new File("/tmp/reallive/test1.mmf").delete();
        RLSchema schema = new RLSchema();
        schema.createTable( "test1", TestRec.class );
        RLTable<TestRec> table = schema.getTable("test1");
        RLStream<TestRec> stream = table.getStream();

        TestRec add0 = table.createForAdd();
        TestRec add1 = table.createForAdd();

        Future<String> k1 = table.$addGetId(add1);
        Future<String> k0 = table.$addGetId(add0);

        Actors.yield(k0, k1).then( (_r,e) -> {

            TestRec forUpdate1 = table.createForUpdate(k1.getResult(), false);
            forUpdate1.setAnother("sodi");
            forUpdate1.$apply();

            forUpdate1.setAnother("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            forUpdate1.$apply();

            forUpdate1.setAnother("asd");
            forUpdate1.$apply();

            forUpdate1.setAnother("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab");
            forUpdate1.$apply();

            TestRec forUpdate = table.createForUpdate(k0.getResult(), false);
            forUpdate.setAnother("sodifjsodifjsodifjsodifjsodifjsodijfsodifjsoidfjsoidfjsoidfjsofijdsodijfsodfijsoijfd");
            forUpdate.$apply();

            forUpdate.setAnother("sod");
            forUpdate.$apply();

            forUpdate.setAnother("sodaosidjaosidjaosidjaosdijweofdfkgjdkfgjhdkfghjdfkgjdfkvbjdvksdvkdscbnkscdnksdbjcsdf");
            forUpdate.$apply();

            forUpdate.setAnother("pok");

            forUpdate.$apply().then( (key1,e1) -> {
                System.out.println("org " + k0 + " applied " + key1);
                stream.each( (change) -> {
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
        RLSchema schema = new RLSchema();
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
            table.$addGetId(newRec).then((r, e) -> {
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
        RLSchema schema = new RLSchema();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
        while( true )
            mutateOnce(test);
    }

    private void mutateOnce(RLTable<TestRec> table) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
        int MAX = 100000;
//        int MAX = 1000;
        int count[] = {0};
        table.getStream().filterUntil(
            (rec) -> true,
            (rec) -> count[0]++ >= MAX,
            (r) -> {
                if (r.isSnapshotDone()) {
                    latch.countDown();
                } else if (r.isError()) {
                    System.out.println("ERROR---------------");
                } else {
                    TestRec record = r.getRecord();// fixme: should clone
                    table.prepareRecordForUpdate(record);
                    record.setName(longString.substring((int) (longString.length() * Math.random())));
//                    r.setName(longString.substring(0,(int) (20 * Math.random())));
                    record.$apply();
                }
            }
                                     );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
    }

    @Test
    public void query() throws InterruptedException {
        RLSchema schema = new RLSchema();
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
        int count[] = {0};
        test.getStream().filterUntil(
            (rec) -> true,
            (rec) -> count[0]++ >= MAX,
            (r) -> {
                if (r.isSnapshotDone())
                    latch.countDown();
                if (r.isError()) {
                    System.out.println("ERROR");
                    System.out.println("count " + count[0]);
                }
            }
                                    );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
    }

    @Test
    public void queryBinary() throws InterruptedException {
        RLSchema schema = new RLSchema();
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
        test.getStream().filterBinary(
            (rec) -> {
                return true;
            },
            (rec) -> count[0]++ >= MAX,
            (r, e) -> {
                if (e == RLTable.FIN)
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
        RLSchema schema = new RLSchema();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");

//        Thread.sleep(20000);
        System.out.println("start");

        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 1000000;
        int count[] = {0};
        test.getStream().filterUntil(
            (rec) -> false,
            (rec) -> count[0]++ >= MAX,
            (r) -> {
                if (r.isSnapshotDone())
                    latch.countDown();
            }
        );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
        Thread.sleep(100000);
    }

    @Test
    public void testBasics() throws InterruptedException {
        RLSchema schema = new RLSchema();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
        TestRec newRec = new TestRec(null, test);
        Future<String> res[] = new Future[10];
        CountDownLatch latch = new CountDownLatch(1);
        for ( int i = 0; i < 10; i++ ) {
            newRec.setX(i);
            res[i] = test.$addGetId(newRec);
        }
        Actors.yield(res).then( (r, e) -> {
            for (int i = 0; i < r.length; i++) {
                Future future = r[i];
                System.out.println("key: '" + future.getResult() + "'");
            }
        }).then((r,e) ->
            test.$get("test:a").then( (r1,e1) -> System.out.println(r1) ).then( (r2,e2)-> latch.countDown() )
        );

        latch.await();
    }

    @Test
    public void testRepeatedRead() throws InterruptedException {
        RLSchema schema = new RLSchema();
        schema.createTable( "test", TestRec.class );
        RLTable<TestRec> test = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        test.$get("test:a")
            .then( (r1,e1) -> System.out.println(r1) )
            .then( (r2,e2) -> latch.countDown() );

        latch.await();

    }

}
