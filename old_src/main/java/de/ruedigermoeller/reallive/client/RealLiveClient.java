package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.fastcast.remoting.*;
import de.ruedigermoeller.fastcast.service.FCMembership;
import de.ruedigermoeller.fastcast.util.FCLog;
import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.heapoff.structs.unsafeimpl.FSTStructFactory;
import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.facade.database.RealLive;
import de.ruedigermoeller.reallive.impl.RLTableRegistry;
import de.ruedigermoeller.reallive.query.RLQueryHelper;
import de.ruedigermoeller.reallive.server.RLNodePingInfo;
import de.ruedigermoeller.reallive.server.RealLiveNode;
import de.ruedigermoeller.reallive.server.RealLiveNodeInternals;
import de.ruedigermoeller.reallive.server.RealLiveTopicService;
import de.ruedigermoeller.serialization.FSTClazzInfoRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 08.11.13
 * Time: 18:26
 * To change this template use File | Settings | File Templates.
 */
public class RealLiveClient extends RLTableRegistry implements RealLive {

    public static int changesPerSync = 5000;
    public static int maxOpenSyncs = 10;

    RealLiveTopicService proxy;
    RealLiveClientTopicService clientService;
    RealLiveGlobalTopicService globalService;
    RealLiveClassRessolvingService classResolver;
    RealLiveNodeInternals rlinternals;
    FCRemoting remoting;

    AtomicInteger localIds = new AtomicInteger(1);

    ConcurrentHashMap<String,Integer> dnAddres2MyId = new ConcurrentHashMap<>();
    FCMembership memberShip;

    ArrayList<RLNodePingInfo> currentAddTarget = null;
    volatile int currentAddIndex = 0;

    String nodeId = "rlclient";

    public RealLiveClient() {
    }

    public RealLiveClient(String nodeId) {
        this.nodeId = nodeId;
    }

    public void connect() throws Exception {
        remoting = FastCast.getRemoting();
        clientService = new RealLiveClientTopicService();
        globalService = new RealLiveGlobalTopicService();
        classResolver = new RealLiveClassRessolvingService();

        remoting.joinCluster( RealLiveNode.getClusterConfig(),nodeId, null );

        rlinternals = (RealLiveNodeInternals)remoting.startSending("rlinternal", RealLiveNodeInternals.class);

        rlinternals.assignClientId(new FCFutureResultHandler<Integer>() {
            @Override
            public void resultReceived(Integer obj, String sender) {
                dnAddres2MyId.put(sender,obj);
                FCLog.log("assign client id for " + sender + " " + obj);
            }
        });
        memberShip = remoting.getMemberShipLocal();
        Thread.sleep(memberShip.getHeartbeatInterval() * 2);

        ArrayList<RLNodePingInfo> activeNodes = memberShip.getActiveNodes(RLNodePingInfo.class);
        if ( activeNodes.size() == 0 ) {
            System.out.println("no data nodes found");
            System.exit(-1);
        }
        updateAddTarget(activeNodes);

        remoting.startReceiving("rlclientclasses", classResolver);
        remoting.startReceiving("rlclient", clientService);
        remoting.startReceiving("rlglobal", globalService);
        proxy = (RealLiveTopicService) remoting.startSending("rlserver", RealLiveTopicService.class);

        // retarget add node each X seconds
        new Thread( "data node watch") {
            public void run() {
                while(true) {
                    try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
                    updateAddTarget(memberShip.getActiveNodes(RLNodePingInfo.class));
                }
            }
        }.start();

        final CountDownLatch latch = new CountDownLatch(1);
        proxy.getTableConfigurations(new FCFutureResultHandler<RLTableConfiguration[]>() {
            @Override
            public void resultReceived(RLTableConfiguration[] obj, String sender) {
                done();
                for (int i = 0; i < obj.length; i++) {
                    RLTableConfiguration rlTableConfiguration = obj[i];
                    System.out.println("register table "+rlTableConfiguration);
                    registerTable(rlTableConfiguration);
                }
                latch.countDown();
                System.out.println("schema initialized");
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        if ( latch.getCount() > 0 ) {
            System.out.println("FATAL no response for schema request. Exiting");
            System.exit(-1);
        }
    }

    private void updateAddTarget(ArrayList<RLNodePingInfo> activeNodes) {
        if ( activeNodes == null || activeNodes.size() == 0) {
            FCLog.get().cluster("no active data nodes found, ignore for now ..");
            return;
        }
        Collections.sort(activeNodes, new Comparator<RLNodePingInfo>() {
            @Override
            public int compare(RLNodePingInfo o1, RLNodePingInfo o2) {
                return (int) (o2.getFreeMem()-o1.getFreeMem());
            }
        });
        for (int i = 0; i < activeNodes.size(); i++) {
            RLNodePingInfo rlNodePingInfo = activeNodes.get(i);
            boolean isKnown = dnAddres2MyId.containsKey(rlNodePingInfo.getSender());
            if (!isKnown) {
                FCSendContext.get().setReceiver(rlNodePingInfo.getSender());
                rlinternals.assignClientId(new FCFutureResultHandler<Integer>() {
                    @Override
                    public void resultReceived(Integer obj, String sender) {
                        dnAddres2MyId.put(sender,obj);
                        FCLog.log("assign client id for new discovered "+sender+" "+obj);
                    }
                });
            }
        }
        currentAddIndex = 0;
        currentAddTarget = activeNodes;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // impl of reallive interface
    //

    protected void registerTable(RLTableConfiguration configuration) {
        super.createTable(configuration);
    }

    /**
     * create a table in the remote nodes
     * @param configuration
     */
    @Override
    public void createTable(final RLTableConfiguration configuration) {
        super.createTable(configuration);
        final Class rowClass = configuration.getRowClass();

        System.out.println("sending create table "+rowClass);

        ensureClassesRemotelyLoaded(rowClass);

        System.out.println("await creation of table "+rowClass+" "+configuration.getTableId());
        proxy.createTable(configuration);
        System.out.println("created table "+rowClass+" "+configuration.getTableId());
    }

    HashSet<String> alreadyEnsured = new HashSet<>();
    HashSet<Class> alreadyEnsuredTopLevel = new HashSet<>();
    private void ensureClassesRemotelyLoaded(Class rowClass) {
        if ( alreadyEnsuredTopLevel.contains(rowClass) ) {
            return;
        }
        alreadyEnsuredTopLevel.add(rowClass);

        ArrayList<String> tmparr = new ArrayList<>();
        FSTClazzInfoRegistry.addAllReferencedClasses(rowClass, tmparr);

        final ArrayList<String> arr = new ArrayList<>();
        for (int i = 0; i < tmparr.size(); i++) {
            String cl = tmparr.get(i);
            if ( cl.startsWith("java.") ||
                 cl.startsWith("de.ruedigermoeller.") ||
                 cl.startsWith("javassist.") ||
                 cl.startsWith("sun.")
                )
                continue;
            if ( ! alreadyEnsured.contains(cl) ) {
                arr.add(cl);
            }
            alreadyEnsured.add(cl);
        }

        for (int i = 0; i < arr.size(); i++) {
            final String clz = arr.get(i);
            final CountDownLatch latch = new CountDownLatch(1);
            proxy.registerRemoteClass(clz, new FCFutureResultHandler() {
                @Override
                public void resultReceived(Object obj, String sender) {
                    System.out.println("finished register, sent "+clz);
                    latch.countDown();
                    done();
                }

                @Override
                public void timeoutReached() {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            final String clz = arr.get(i);
            try {
                final Class c = Class.forName(clz);
                if (FSTStruct.class.isAssignableFrom(c)) {
                    final CountDownLatch latch = new CountDownLatch(1);
                    FSTStructFactory.getInstance().registerClz(c);
                    proxy.registerRemoteStruct(clz, FSTStructFactory.getInstance().getClzId(c), new FCFutureResultHandler() {
                        @Override
                        public void resultReceived(Object obj, String sender) {
                            System.out.println("finished struct register, sent " + clz);
                            registerStructClazz(c);
                            latch.countDown();
                            done();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    void registerStructClazz(Class rowClass) {
        try {
            Class proxyClass = FSTStructFactory.getInstance().getProxyClass(rowClass);// define struct
            FastCast.getSerializationConfig().getClassRegistry().registerClazzFromOtherLoader(proxyClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dropTable(int tableId) {
        super.dropTable(tableId);
        proxy.dropTable(tableId);
    }

    @Override
    public RLTableConfiguration getTableConfiguration(int tableId) {
        return super.getTableConfiguration(tableId);
    }

    @Override
    public RLTableConfiguration[] getTableConfigurations() {
        return super.getTableConfigurations();
    }

    @Override
    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(final int tableId) {
        return new RLCollectionMutator<T>() {

            int changeCounter = 0;
            Semaphore pendingSyncs = new Semaphore(maxOpenSyncs*getNumDN());

            @Override
            public T allocateInstanceForAdd() {
                FSTStruct fstStruct = ((FSTStruct) getTableConfiguration(tableId).getTemplate()).createCopy().startChangeTracking();
                ((RLRow)fstStruct).setId(constructAddId());
                return (T) fstStruct;
            }

            @Override
            public T getInstanceForUpdate(T current) {
                T t = (T) ((FSTStruct)current).createCopy();
                ((FSTStruct)t).startChangeTracking();
                return t;
            }

            private void checkSync() {
                changeCounter++;
                if ( changeCounter > changesPerSync ) {
//                    System.out.println("pending permits "+pendingSyncs.availablePermits());
                    changeCounter = 0;
                    pendingSyncs.acquireUninterruptibly(getNumDN());
                    proxy.sync(tableId, new FCFutureResultHandler() {
                        int relCount = getNumDN();
                        long tim = System.currentTimeMillis();
                        @Override
                        public void resultReceived(Object obj, String sender) {
                            relCount--;
                            pendingSyncs.release();
                            if ( relCount == getNumDN() ) {
                                done();
                            }
                        }

                        @Override
                        public void timeoutReached() {
                            if ( relCount > 0 ) {
                                pendingSyncs.release(relCount);
                                System.out.println("--------------------- timout ! ----------------------- "+relCount+" "+(System.currentTimeMillis()-tim));
                            }
                        }
                    });
                }
            }

            @Override
            public void add(T t) {
                if ( ! ((FSTStruct)t).isChangeTracking() ) {
                    throw new RuntimeException("must allocate using allocateInstanceForAdd()");
                }
                if ( t.getId() != 0 ) {
                    throw new RuntimeException("id is assigned by system. Access the generated unqique row id after calling add.");
                }
                checkSync();
                long id = getUniqueId();
                FSTStructChange change = ((FSTStruct) t).finishChangeTracking();
                t.setId(id);
                FCSendContext.get().setLongFlowHeader(id);
                proxy.add(tableId,id, change);
            }

            @Override
            public void remove(long id) {
                checkSync();
                FCSendContext.get().setLongFlowHeader(id);
                proxy.remove(tableId,id);
            }

            @Override
            public void update(T updated) {
                checkSync();
                if ( ! ((FSTStruct)updated).isChangeTracking() ) {
                    throw new RuntimeException("must allocate using allocateInstanceForAdd()");
                }
                FCSendContext.get().setLongFlowHeader(updated.getId());
                proxy.update(tableId,updated.getId(),((FSTStruct) updated).finishChangeTracking());
            }

            @Override
            public void sync() {
                final CountDownLatch latch = new CountDownLatch(getNumDN());
                proxy.sync(tableId,new FCFutureResultHandler() {
                    @Override
                    public void resultReceived(Object obj, String sender) {
                        latch.countDown();
                        if ( latch.getCount() == 0 )
                            done();
                    }

                    @Override
                    public void timeoutReached() {
                        System.out.println("sync timout reached");
                        while(latch.getCount()>0)
                            latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    AtomicLong creationCount = new AtomicLong(0);
    private <T extends RLRow> long getUniqueId() {
        ArrayList<RLNodePingInfo> targets = currentAddTarget;
        int index = currentAddIndex / 3;
        if ( index >= targets.size() ) {
            index = 0; currentAddIndex = 0;
        }
        RLNodePingInfo addTarget = targets.get(index);
        String sender = addTarget.getSender();
        int myIdToCurrentDN = dnAddres2MyId.get(sender);
        currentAddIndex += currentAddIndex == 0 ? 1 : (currentAddIndex == 1) ? 2 : 3;
        return computeRowID(addTarget.getNodeMask(),myIdToCurrentDN,creationCount.incrementAndGet());
    }

    final static int nodeMaskBits = 8; // 128 nodes
    final static int myIdBits = nodeMaskBits+16; // 65k client connects
    public static int extractNodeMask(long id) {
        return (int) (id>>>(64-nodeMaskBits));
    }

    public static long computeRowID(long nodeMask, long myIdToCurrentDN, long l)
    {
        long l1 = (nodeMask << (64 - nodeMaskBits)) | (myIdToCurrentDN << (64 - myIdBits) | l);
        return l1;
    }

    protected long constructAddId() {
        return 0;
    }

    @Override
    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(Class<T> rowClazz) {
        Object val = clazzUniqueMap.get(rowClazz);
        if ( val instanceof Integer ) {
            return getTableMutator(((Integer) val).intValue());
        }
        return null;
    }

    int getNumDN() {
        return memberShip.getActiveNodes(RLNodePingInfo.class).size();
    }

    @Override
    public <T extends RLRow> RLChangeSource<T> getTable(final int tableId) {
        return new RLChangeSource<T>() {
            @Override
            public int getCollectionId() {
                return tableId;
            }

            @Override
            public void select(final RLRowMatcher<T> matcher, RLChangeTarget<T> listener) {
                System.out.println("select for "+getNumDN()+" data nodes. Running pre select "+clientService.runningSelects.size()+" matcher "+matcher.getClass().getName());
                final ArrayList arrayList = RLQueryHelper.nullOuterThis(matcher);
                final int subsId = localIds.incrementAndGet();
                clientService.registerSelect(subsId, getNumDN(), listener, null);

                ensureClassesRemotelyLoaded(matcher.getClass());

                proxy.select(subsId, tableId, matcher);
                RLQueryHelper.restoreOuterThis(arrayList, matcher);
            }

            @Override
            public RLSubscription subscribe(final RLRowMatcher<T> matcher, final RLChangeTarget<T> listener) {
                final ArrayList arrayList = RLQueryHelper.nullOuterThis(matcher);
                final int subsId = localIds.incrementAndGet();
                SubscriptionEntry subscriptionEntry = globalService.registerSubscribe(subsId, tableId, listener, matcher);
                final int selId = localIds.incrementAndGet();
                System.out.println("subscribe for " + getNumDN() + " data nodes "+matcher.getClass().getName());
                clientService.registerSelect(selId, getNumDN(), new RLChangeTarget<T>() {
                        @Override
                        public void added(long version, T added) {
                            listener.added(version, added);
                        }

                        @Override
                        public void preUpdate(long version, FSTStructChange change, T oldRow) {
                            listener.preUpdate(version, change, oldRow);
                        }

                        @Override
                        public void updated(long rowId, FSTStructChange change, T updatedRow) {
                            listener.updated(rowId, change, updatedRow);
                        }

                        @Override
                        public void removed(long version, T removed) {
                            listener.removed(version, removed);
                        }

                        @Override
                        public void queryFinished(Object error) {
                            listener.queryFinished(error);
                        }

                    },
                    subscriptionEntry
                );

                ensureClassesRemotelyLoaded(matcher.getClass());
                proxy.select(selId, tableId, matcher);
                RLQueryHelper.restoreOuterThis(arrayList, matcher);
                return new RLSubscription() {
                    @Override
                    public void unsubscribe() {
                        globalService.unregisterSubscriber(subsId);
                    }
                };
            }

            @Override
            public long createVersion() {
                return -1;
            }

            @Override
            public long getVersion() {
                return -1;
            }

        };
    }


    @Override
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror(int tableId, RLRowMatcher<T> filter) {
        return null;
    }

    @Override
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror(Class<T> rowClazz, RLRowMatcher<T> filter) {
        return null;
    }

    //
    // impl of reallive interface
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void main( String arg[] ) throws Exception {
//        RealLiveClient client = new RealLiveClient();
//        client.connect();
        computeRowID(3,4,99);
        System.out.println(Long.toBinaryString(computeRowID(3, 4, 0)));
    }


}
