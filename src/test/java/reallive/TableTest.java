package reallive;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.impl.InMemSchema;
import org.nustaq.impl.TableImpl;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.model.Record;
import org.nustaq.model.RecordChange;
import org.nustaq.model.Schema;
import org.nustaq.model.Table;

import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 21.06.14.
 */
public class TableTest {

    @Test
    public void machVollSync() throws InterruptedException {
        InMemSchema schema = new InMemSchema();
        TableImpl<TestRec> test = new TableImpl<>();
        test.$init("test",schema);
        TestRec newRec = new TestRec(null, schema);
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
    public void machVoll() throws InterruptedException {
        InMemSchema schema = new InMemSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> test = schema.getTable("test");
        TestRec newRec = new TestRec(null, schema);
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
        int MAX = 5*1000000;
        for ( int i = 0; i < MAX; i++ ) {
            newRec.setX(i);
            int finalI = i;
            test.$add(newRec).then( (r,e) -> {
                if ( (finalI%1000) == 0 ) {
                    System.out.println("adding .. "+finalI );
                }
                if ( finalI == MAX-1 )
                    latch.countDown();
            });
        }
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

    @Test
    public void update() throws InterruptedException {
        InMemSchema schema = new InMemSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> test = schema.getTable("test");


        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 1000000;
        int count[] = {0};
        test.$filter(
            (rec) -> true,
            (rec) -> count[0]++ >= MAX,
            (r,e) -> {
                if ( e != Table.FIN ) {
//                    if ( r.getX() != 18 )
//                        System.out.println("POK");
                    TestRec org = new TestRec();
                    r.copyTo(org);
                    r._setOriginalRecord(org);
                    r.setX(30);
                    test.$update(r.computeDiff());
                } else {
                    latch.countDown();
                }
            }
        );
        latch.await();
        long dur = System.currentTimeMillis() - tim;
        System.out.println("need "+ dur +" for "+count[0]+" recs. "+(count[0]/dur)+" per ms ");
        Thread.sleep(100000);
    }

    @Test
    public void query() throws InterruptedException {
        InMemSchema schema = new InMemSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> test = schema.getTable("test");

        while( true )
            oneQLoop(test);
//        Thread.sleep(100000);
    }

    private void oneQLoop(Table<TestRec> test) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long tim = System.currentTimeMillis();
//        int MAX = 1*1000000;
        int MAX = 10000;
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
    public void queryNoMatch() throws InterruptedException {
        InMemSchema schema = new InMemSchema();
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
        InMemSchema schema = new InMemSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> test = schema.getTable("test");
        TestRec newRec = new TestRec(null, schema);
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
        InMemSchema schema = new InMemSchema();
        schema.createTable( "test", TestRec.class );
        Table<TestRec> test = schema.getTable("test");
        CountDownLatch latch = new CountDownLatch(1);

        test.$get("test:a")
            .then( (r1,e1) -> System.out.println(r1) )
            .then( (r2,e2) -> latch.countDown() );

        latch.await();

    }

}
