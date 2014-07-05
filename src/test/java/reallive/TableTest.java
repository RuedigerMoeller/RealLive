package reallive;

import org.junit.Test;
import org.nustaq.impl.RLSchema;
import org.nustaq.impl.TableImpl;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.model.Table;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.storage.TestRec;

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
        TableImpl<TestRec> test = new TableImpl<>();
        test.$init("test",schema, TestRec.class);
        TestRec newRec = new TestRec(null, test);
        long tim = System.currentTimeMillis();
        int MAX = 5*1000000;
        for ( int i = 0; i < MAX; i++ ) {
            newRec.setX(i);
            int finalI = i;
            test.$add(newRec);
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
        Table<TestRec> table = schema.getTable("test1");
        TestRec forAdd = table.createRecordForAdd();
        table.$add(forAdd).then((key,e) -> {

            TestRec forUpdate = table.createRecordForUpdate(key, false);
            forUpdate.setAnother("sodifjsodifjsodifjsodifjsodifjsodijfsodifjsoidfjsoidfjsoidfjsofijdsodijfsodfijsoijfd");
            forUpdate.$apply();

            forUpdate.setAnother("sod");
            forUpdate.$apply().then( (key1,e1) -> {
                System.out.println("org " + key + " applied " + key1);
                table.$filter(null, null, (r, e2) -> {
                    if (e2 == Table.FIN) {
                        testLatch.countDown();
                    } else if ( e2 != null ) {
                        ((Throwable)e2).printStackTrace();
                    } else
                        System.out.println(r);
                });
            });
        });
        testLatch.await();
    }

    @Test
    public void machVoll() throws InterruptedException {
        RLSchema schema = new RLSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> table = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("record size:"+ FSTConfiguration.getDefaultConfiguration().asByteArray(table.createRecordForAdd()).length);


        long tim = System.currentTimeMillis();
        int MAX = 1000000;
//        int MAX = 200000;
        for ( int i = 0; i < MAX; i++ ) {
            TestRec newRec = table.createRecordForAdd();
            while ( ((TableImpl)table).__mailbox.size() > 10000 ) {
                LockSupport.parkNanos(1);
            }
            newRec.setX(i);
            int finalI = i;
            table.$add(newRec).then((r, e) -> {
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
        Table<TestRec> test = schema.getTable("test");
//        while( true )
            mutateOnce(test);
    }

    private void mutateOnce(Table<TestRec> table) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
        int MAX = 100000;
//        int MAX = 1000;
        int count[] = {0};
        table.$filter(
            (rec) -> true,
            (rec) -> count[0]++ >= MAX,
            (r, e) -> {
                if (e != Table.FIN) {
                    if ( r == null ) {
                        if ( e instanceof Throwable )
                            ((Throwable) e).printStackTrace();
                        System.out.println("error");
                    }
                    table.prepareRecordForUpdate(r);
                    r.setName(longString.substring((int) (longString.length() * Math.random())));
//                    r.setName(longString.substring(0,(int) (20 * Math.random())));
                    r.$apply();

                } else {
                    latch.countDown();
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
        Table<TestRec> test = schema.getTable("test");

//        while( true )
            oneQLoop(test);
//        Thread.sleep(100000);
    }

    private void oneQLoop(Table<TestRec> test) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 100000;
        int count[] = {0};
        test.$filter(
            (rec) -> true,
            (rec) -> count[0]++ >= MAX,
            (r,e) -> {
                if ( e == Table.FIN )
                    latch.countDown();
                if ( e instanceof Exception ) {
                    System.out.println("count "+count[0]);
                    ((Exception) e).printStackTrace();
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
        Table<TestRec> test = schema.getTable("test");

//        while( true )
            oneQBinaryLoop(test);
//        Thread.sleep(100000);
    }

    private void oneQBinaryLoop(Table<TestRec> test) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 100000;
        int count[] = {0};
        test.$filterBinary(
            (rec) -> {
                return true;
            },
            (rec) -> count[0]++ >= MAX,
            (r,e) -> {
                if ( e == Table.FIN )
                    latch.countDown();
                if ( e instanceof Exception ) {
                    System.out.println("count "+count[0]);
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
        Table<TestRec> test = schema.getTable("test");

//        Thread.sleep(20000);
        System.out.println("start");

        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 1000000;
        int count[] = {0};
        test.$filter(
            (rec) -> false,
            (rec) -> count[0]++ >= MAX,
            (r,e) -> {
                if ( e == Table.FIN )
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
        Table<TestRec> test = schema.getTable("test");
        TestRec newRec = new TestRec(null, test);
        Future<String> res[] = new Future[10];
        CountDownLatch latch = new CountDownLatch(1);
        for ( int i = 0; i < 10; i++ ) {
            newRec.setX(i);
            res[i] = test.$add(newRec);
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
        Table<TestRec> test = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        test.$get("test:a")
            .then( (r1,e1) -> System.out.println(r1) )
            .then( (r2,e2) -> latch.countDown() );

        latch.await();

    }

}
