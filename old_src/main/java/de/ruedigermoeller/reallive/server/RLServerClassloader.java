package de.ruedigermoeller.reallive.server;

import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCReceiveContext;
import de.ruedigermoeller.fastcast.remoting.FCSendContext;
import de.ruedigermoeller.fastcast.remoting.FastCast;
import de.ruedigermoeller.heapoff.structs.unsafeimpl.FSTStructFactory;
import de.ruedigermoeller.reallive.client.RealLiveClassRessolvingService;
import de.ruedigermoeller.reallive.client.RealLiveClientTopicService;
import de.ruedigermoeller.serialization.util.FSTUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 12/6/13.
 *
 * assumes classloading is going on receiving thread always
 */
public class RLServerClassloader extends ClassLoader {

    RealLiveClassRessolvingService remoteClients;
    ConcurrentHashMap<String,Class> definedClasses = new ConcurrentHashMap<>();

    public RLServerClassloader(ClassLoader parent, RealLiveClassRessolvingService clients) {
        super(parent);
        remoteClients = clients;
    }

    public Class loadClass(final String name) throws ClassNotFoundException {
        if ( name.endsWith("_Struct") )
            Thread.dumpStack();
        final AtomicReference<Class> res = new AtomicReference<>();
        try { return super.loadClass(name); } catch ( Exception ex ) {
            System.out.println("start resolving foreign "+name+" "+Thread.currentThread().getName());
            synchronized (definedClasses) {
                if ( definedClasses.get(name) != null ) {
                    System.out.println("local lookup sucessful");
                    return definedClasses.get(name);
                }
            }
            final CountDownLatch latch = new CountDownLatch(1);
            FCSendContext.get().setReceiver(FCReceiveContext.get().getSender());
            System.out.println("start resolving remote " + name + " from " + FCReceiveContext.get().getSender());
            remoteClients.getClass(name,new FCFutureResultHandler<byte[]>() {
                @Override
                public void resultReceived(byte[] clzBytes, String sender) {
                    synchronized (this) {
                        try {
                            System.out.println("class load response in "+Thread.currentThread().getName()+" cl:"+name);
                            //Class aClass = FSTUtil.unsafe.defineClass(name, clzBytes, 0, clzBytes.length);
                            Class aClass = defineClass(name, clzBytes, 0, clzBytes.length);
                            FSTStructFactory.getInstance().registerRawClass(name,clzBytes);
                            synchronized (definedClasses) {
                                definedClasses.put(name,aClass);
                            }
                            FastCast.getSerializationConfig().getClassRegistry().registerClazzFromOtherLoader(aClass);
                            System.out.println("defined remote class "+name);
                            res.set(aClass);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        done();
                        latch.countDown();
                    }
                }
            });
            try {
                System.out.println("locking thread "+Thread.currentThread().getName());
//                Thread.dumpStack();
                latch.await();
                System.out.println("<unlocked> "+Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return res.get();
    }
}
