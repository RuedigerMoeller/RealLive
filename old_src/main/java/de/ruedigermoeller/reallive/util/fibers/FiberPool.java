package de.ruedigermoeller.reallive.util.fibers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * User: ruedi
 * Date: 18.11.13
 * Time: 23:54
 * To change this template use File | Settings | File Templates.
 */
public class FiberPool extends Thread implements Executor {

    ConcurrentLinkedQueue<FiberRunnable> fibers = new ConcurrentLinkedQueue<>();
    FiberRunnable currentFiber = null;
    Semaphore elems = new Semaphore(0);
    AtomicReference<Runnable> prioJob = new AtomicReference<>();

    public FiberPool(String name) {
        super(name);
    }

    public int getFiberSize() {
        return fibers.size();
    }

    public void execute( Runnable torun ) {
        while(!prioJob.compareAndSet(null,torun))
            ;
        elems.release();
    }

    public void addFiber(FiberRunnable fiber) {
        fibers.add(fiber);
        elems.release();
    }

    public void finished() {
        elems.acquireUninterruptibly();
        fibers.remove(currentFiber);
        currentFiber = null;
    }

    public void run() {
        while (true) {
            try {
                handlePrioJob();
            } catch (Throwable th) {
                th.printStackTrace();
            }
            while (fibers.size() > 0 ) {
                for (Iterator<FiberRunnable> iterator = fibers.iterator(); iterator.hasNext(); ) {
                    try {
                        handlePrioJob();
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    currentFiber = iterator.next();
                    if ( currentFiber != null ) {
                        try {
                            currentFiber.tick(this);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                }
            }
            elems.acquireUninterruptibly();
            elems.release();
        }
    }

    private void handlePrioJob() {
        if ( prioJob.get()!=null ) {
            try {
                prioJob.get().run();
            } finally {
                elems.acquireUninterruptibly();
                prioJob.set(null);
            }
        }
    }

    public static void main(String arg[] ) throws InterruptedException {
        FiberPool fiber = new FiberPool("pok");
        fiber.start();

        while( true ) {
            switch ((int) (Math.random()*1000)) {
                default:
                    fiber.execute(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("high prio -----------------------------------------------------------");
                        }
                    });
                    break;
                case 0:
                    fiber.addFiber(new FiberRunnable() {
                        int count = 5;
                        @Override
                        public void tick(FiberPool p) {
                            count--;
                            System.out.println(System.identityHashCode(this)+" count "+count);
                            if ( count < 0 ) {
                                System.out.println("######################finisgh "+p.getFiberSize());
                                p.finished();
                            }
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    });
            }
            Thread.sleep(1);
        }
    }

}
