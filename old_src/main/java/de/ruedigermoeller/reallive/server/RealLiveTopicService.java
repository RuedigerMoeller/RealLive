package de.ruedigermoeller.reallive.server;

import de.ruedigermoeller.fastcast.remoting.*;
import de.ruedigermoeller.fastcast.util.FCLog;
import de.ruedigermoeller.heapoff.bytez.Bytez;
import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.heapoff.structs.unsafeimpl.FSTStructFactory;
import de.ruedigermoeller.reallive.client.RealLiveClassRessolvingService;
import de.ruedigermoeller.reallive.client.RealLiveClient;
import de.ruedigermoeller.reallive.client.RealLiveGlobalTopicService;
import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.facade.database.RealLive;
import de.ruedigermoeller.reallive.impl.InProcessRealLive;
import de.ruedigermoeller.reallive.impl.RLExtendedCollectionMutator;
import de.ruedigermoeller.reallive.client.RealLiveClientTopicService;
import de.ruedigermoeller.serialization.util.FSTUtil;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

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
 * Date: 07.11.13
 * Time: 19:04
 * To change this template use File | Settings | File Templates.
 */
@PerSenderThread(false)
public class RealLiveTopicService extends FCTopicService {

    RealLive db;
    RealLiveClientTopicService clients;
    RealLiveGlobalTopicService globals;
    int nodeMaskId = 0; //unshifted node num
    RLServerClassloader loader;

    public RealLiveTopicService() { // for proxy
    }

    public RealLiveTopicService(int nodeMaskId) {
        this.nodeMaskId = nodeMaskId;
    }

    public RealLive getDb() {
        return db;
    }

    /**
     * override to do init and stuff
     */
    @Override
    public void init() {
        db = new InProcessRealLive(nodeMaskId);
        try {
            clients = (RealLiveClientTopicService) getRemoting().startSending("rlclient",RealLiveClientTopicService.class);
            RealLiveClassRessolvingService rlclientclasses = (RealLiveClassRessolvingService) getRemoting().startSending("rlclientclasses", RealLiveClassRessolvingService.class);
            loader = new RLServerClassloader(getClass().getClassLoader(), rlclientclasses);
            FSTStructFactory.getInstance().setParentLoader(loader);
            globals = (RealLiveGlobalTopicService) getRemoting().startSending("rlglobal",RealLiveGlobalTopicService.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNodeMaskId() {
        return nodeMaskId;
    }

    public void setNodeMaskId(int nodeMaskId) {
        this.nodeMaskId = nodeMaskId;
    }

    public RealLiveClientTopicService getClients() {
        return clients;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Remote DB Stuff ...
    //

    @RemoteMethod(1)
    public void createTable( RLTableConfiguration configuration ) {
        if ( ((InProcessRealLive) db).getCol(configuration.getTableId()) != null ) {
            FCLog.log("table already exists: "+configuration );
            return;
        }
        FCLog.log("creating table "+configuration+" row size "+((FSTStruct)configuration.getTemplate()).getByteSize());
        db.createTable(configuration);
        final RLCollection col = ((InProcessRealLive) db).getCol(configuration.getTableId());
        col.setListener(new RLChangeTarget() {
            @Override
            public void added(long version, RLRow added) {
//                System.out.println("send $add "+version);
                globals.receiveGlobalAdd(version,added); // FIXME: could reduce to change against default
            }

            @Override
            public void preUpdate(long version, FSTStructChange change, RLRow oldRow) {
//                System.out.println("send upd "+version);
                globals.receiveGlobalUpdate(version, change, oldRow);
            }

            @Override
            public void updated(long version, FSTStructChange change, RLRow updatedRow) {
//                globals.receiveGlobalUpdate(version,change, updatedRow); no pre/post update global
            }

            @Override
            public void removed(long version, RLRow removed) {
//                System.out.println("send rem "+version);
                globals.receiveGlobalRemove(version,removed);
            }

            @Override
            public void queryFinished(Object error) {
                // not sent global
            }

        });
    }

    @RemoteMethod(2)
    public void dropTable( int tableId ) {
        db.dropTable(tableId);
    }

//    public RLTableConfiguration getTableConfiguration( int tableId );

    @RemoteMethod(3)
    public void getTableConfigurations( FCFutureResultHandler<RLTableConfiguration[]> res ) {
        System.out.println("sending table configs .. to "+FCReceiveContext.get().getSender());
        res.sendResult(db.getTableConfigurations());
    }

    //
    // ... Remote DB stuff
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Remote ChangeSource stuff ...
    //

    //    public int getTableId();
    @RemoteMethod(4)
    public void select( final int subsId, int tableId, RLRowMatcher matcher ) {
        final String receiver = FCReceiveContext.get().getSender();
        final RLChangeSource<RLRow> table = db.getTable(tableId);
        final long version = table.createVersion();

        table.select(matcher, new RLChangeTargetAdapter<RLRow>() {
            @Override
            public void added(long version, RLRow added) {
                FCSendContext.get().setReceiver(receiver);
                clients.receiveSelectAdd( subsId, version, added );
            }

            @Override
            public void queryFinished(Object error) {
                FCSendContext.get().setReceiver(receiver);
                clients.receiveSelectQueryFinished( subsId, version, error);
            }
        });
    }

//    public void subscribe( int tableId, RLRowMatcher matcher, FCFutureResultHandler res );

    //
    // ... Remote ChangeSource stuff
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Remote Mutator stuff ...
    //

//    public T allocateInstanceForAdd();
//    public T getInstanceForUpdate(T current);



    // flowHeader
    final int ADD = 6;
    final int REM = 7;
    final int UPD = 8;

    @Override
    protected int readAndFilter(int methodId, Bytez bytez, int offset) {
        switch (methodId) {
            case ADD:
            case REM:
            case UPD:
                long rowId = bytez.getLong(offset);
                if ( !shouldProcessId(rowId) )
                    return -1;
                return 8;
        }
        return 0;
    }

    private boolean shouldProcessId(long rowId) {
        int id = RealLiveClient.extractNodeMask(rowId);
        return id == nodeMaskId;
    }

    @RemoteMethod(ADD)
    public void add(int tableId, long rowId, FSTStructChange change) {
//        if ( ! shouldProcessId(rowId) ) {
//            return;
//        }
        RLCollectionMutator<RLRow> tableMutator = db.getTableMutator(tableId);
        RLRow rlRow = tableMutator.allocateInstanceForAdd();
        change.applySnapshot((FSTStruct) rlRow);
        rlRow.setId(rowId);
        tableMutator.add(rlRow);
    }

    @RemoteMethod(REM)
    public void remove(int tableId, long id) {
//        if ( ! shouldProcessId(id) ) {
//            return;
//        }
        RLCollectionMutator<RLRow> tableMutator = db.getTableMutator(tableId);
        tableMutator.remove(id);
    }

    @RemoteMethod(UPD)
    public void update(int tableId, long rowId, FSTStructChange change) {
//        if ( ! shouldProcessId(rowId) ) {
//            return;
//        }
        RLCollectionMutator<RLRow> tableMutator = db.getTableMutator(tableId);
        ((RLExtendedCollectionMutator)tableMutator).update(rowId, change);
    }

    @RemoteMethod(9)
    public void sync(int collection, FCFutureResultHandler res) {
        RLCollectionMutator<RLRow> tableMutator = db.getTableMutator(collection);
        ((RLExtendedCollectionMutator)tableMutator).syncEx(collection,res);
//        res.sendResult(Boolean.TRUE);
    }

    //
    // ... Remote Mutator stuff
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////


    @RemoteMethod(10)
    public void registerRemoteClass( final String clzName, final FCFutureResultHandler ok ) {
        final String sender = FCReceiveContext.get().getSender();
//        new Thread("[class registering thread ]") {
//            public void run() {
                FCReceiveContext.get().setSender(sender);
                Class aClass = null;
                try {
                    loader.loadClass(clzName);
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                ok.sendResult(Boolean.TRUE);
//            }
//        }.start();
    }

    @RemoteMethod(11)
    public void registerRemoteStruct( String clzName, int clzId, final FCFutureResultHandler ok ) {
        final String sender = FCReceiveContext.get().getSender();
            Class aClass = null;
            try {
                aClass = loader.loadClass(clzName);
                if ( FSTStruct.class.isAssignableFrom(aClass) ) {
                    Class proxyClass = null;// define struct
                    try {
                        Class structClass = FSTStructFactory.getInstance().getClazz(clzId);
                        if ( structClass != null && ! structClass.equals(aClass) )
                            throw new RuntimeException("Colliding client class ids, cluster data inconsistent. Restart");
                        FSTStructFactory.getInstance().registerClzId(aClass, clzId);
                        System.out.println("******* register struct ************* "+clzId+" "+aClass.getName());

                        proxyClass = FSTStructFactory.getInstance().getProxyClass(aClass);
                        FastCast.getSerializationConfig().getClassRegistry().registerClazzFromOtherLoader(proxyClass);
                        System.out.println("defined "+proxyClass.getName()+" loader "+proxyClass.getClassLoader());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        ok.sendResult(Boolean.TRUE);
    }

//    public <T extends RLRow> RLCollectionMutator<T> getTableMutator( int tableId );
//    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(Class<T> rowClazz);
//    public <T extends RLRow> RLChangeSource<T> getTable( int tableId );
//    public <T extends RLRow> RLChangeSource<T> getTable(Class<T> rowClazz);
//    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror( int tableId, RLRowMatcher<T> filter );
//    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror( Class<T> rowClazz , RLRowMatcher<T> filter );

}
