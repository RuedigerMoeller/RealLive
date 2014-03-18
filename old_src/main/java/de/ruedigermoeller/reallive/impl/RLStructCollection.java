package de.ruedigermoeller.reallive.impl;

import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;
import de.ruedigermoeller.fastcast.remoting.FCReceiveContext;
import de.ruedigermoeller.fastcast.remoting.FCSendContext;
import de.ruedigermoeller.heapoff.bytez.Bytez;
import de.ruedigermoeller.heapoff.bytez.malloc.MallocBytezAllocator;
import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.FSTStructAllocator;
import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.util.fibers.FiberPool;
import de.ruedigermoeller.reallive.util.fibers.FiberRunnable;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * versionAtomic 2.1 of the License, or (at your option) any later versionAtomic.
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
 * Date: 29.10.13
 * Time: 20:32
 * To change this template use File | Settings | File Templates.
 */

/**
 * this class is not mt aware, all synchronization must come from outside
 * @param <T>
 */
public class RLStructCollection<T extends RLRow> implements RLCollection<T>,RLMirrorCollection<T> {

    public static final int FIBER_ITERATIONS = 2000;
    FSTStructAllocator allocator;
    RLChangeTarget<T> changeSource;
    RLChangeTarget<T> listener;

    final int startSize = 1000000;
    Long2IntOpenHashMap rowBufMap = new Long2IntOpenHashMap(startSize);
    Bytez[] buffsMap = new Bytez[startSize]; // points to buffer segment of n'th struct
    int indexMapBufIndex[] = new int[startSize]; // points to offset in buffer of associated segment
    long indexMapKeyIndex[] = new long[startSize]; // contains key of n'th entry
    IntStack freeList = new IntStack(startSize); // free slots
    BitSet usedSlots = new BitSet(startSize);


    int indexMapIndex = 0;

    int colId;
    int removed = 0;
    int added = 0;

    private T template;
    private AtomicLong curId = new AtomicLong(1);
    private AtomicLong versionAtomic = new AtomicLong(0);
    private RLSubscription sub;

    final int chunkSizeBytes = 16 * 1024 * 1024;

    public RLStructCollection( int collectionId, T template ) {
        colId = collectionId;
        allocator = new FSTStructAllocator(chunkSizeBytes, new MallocBytezAllocator());
//        allocator = new FSTStructAllocator(chunkSizeBytes);
        this.template = (T) allocator.toStruct((FSTStruct)template);
    }

    public RLChangeTarget<T> getListener() {
        return listener;
    }

    public void setListener(RLChangeTarget<T> listener) {
        this.listener = listener;
    }

    // blocks listener slot
    public RLFiberedFilterChangeSource<T> getFilteredChangeSource() {
        if ( changeSource == null ) {
            changeSource = new RLFiberedFilterChangeSource<T>(this);
        }
        return (RLFiberedFilterChangeSource<T>) changeSource;
    }

    // blocks listener slot
    public RLChangeSource<T> getChangeSource() {
        if ( changeSource == null ) {
            changeSource = new RLFiberedFilterChangeSource<T>(this);
        }
        return (RLChangeSource<T>) changeSource;
    }

    @Override
    public long getVersionAtomic() {
        return versionAtomic.get();
    }

    @Override
    public long createVersion() {
        return versionAtomic.incrementAndGet();
    }

    public RLCollectionMutator<T> getMutator(final Executor pool) {
        return new RLExtendedCollectionMutator<T>() {
            @Override
            public T allocateInstanceForAdd() {
                return (T) ((FSTStruct)getTemplate()).createCopy();
            }

            @Override
            public T getInstanceForUpdate(T current) {
                T t = (T) ((FSTStruct)current).createCopy();
                ((FSTStruct)t).startChangeTracking();
                return t;
            }

            @Override
            public void add(final T t) {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        added(-1, t);
                    }
                });
            }

            @Override
            public void remove(final long id) {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        T row = get(id);
                        if ( row != null )
                            removed(-1, row);
                    }
                });
            }

            @Override
            public void update(final T updated) {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        FSTStruct structUpdated = (FSTStruct) updated;
                        if ( ! structUpdated.isChangeTracking() ) {
                            throw new RuntimeException("invalid row for update need to use getInstanceForUpdate() or enable changetracking before modifying the row");
                        }
                        FSTStructChange fstStructChange = structUpdated.finishChangeTracking();
                        updated(updated.getId(),fstStructChange,updated);
                    }
                });
            }

            public void syncEx( int colId, final FCFutureResultHandler res ) {
                final String sender = FCReceiveContext.get().getSender();
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        FCSendContext.get().setReceiver(sender);
                        res.sendResult(Boolean.TRUE);
                    }
                });
            }

            @Override
            public void sync() {
                throw new RuntimeException("call is for remoting client side");
            }

            @Override
            public void update(final long rowID, final FSTStructChange change) {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        updated(rowID,change,null);
                    }
                });
            }
        };
    }

    @Override
    public int getCollectionId() {
        return colId;
    }

    @Override
    public T get(long l) {
        checkThread();
        if ( !rowBufMap.containsKey(l) ) {
            return null;
        }
        int index = rowBufMap.get(l);
        return (T) allocator.getVolatileStructPointer(buffsMap[index],indexMapBufIndex[index]);
    }

    /**
     * allocates a copy of given struct o
     * @param version
     * @param o
     */
    @Override
    public void added(long version, T o) {
        System.out.println("olen:"+((FSTStruct)o).getByteSize()+" "+((FSTStruct)template).getByteSize());
        if ( ((FSTStruct)o).getByteSize() != ((FSTStruct)template).getByteSize() )
            throw new RuntimeException("FATAL ERROR");
        version = versionAtomic.incrementAndGet();
        o.setCollectionId(colId);
        if ( get(o.getId()) != null ) {
            throw new RuntimeException("Row with "+o.getId()+" already exists "+o.getClass().getSimpleName() );
        }
        int idx = findNextFreeSlot();
        if ( idx < 0 ) {
            added++;
            FSTStruct newAdded = allocator.newStruct((FSTStruct) o);
            indexMapIndex = addEntry(indexMapIndex, o.getId(), (RLRow)newAdded);
        } else {
            removed--;
            Bytez buff = tryFindBuffer(idx);
            if ( buff != null ) {
                ((FSTStruct) o).getBytes(buff,indexMapBufIndex[idx]);
                ((FSTStruct) o).baseOn(buff,indexMapBufIndex[idx]);
//                    FSTStruct struct = (FSTStruct) o;
//                    System.arraycopy(struct.getBase(), struct.getOffset(), buff, indexMapBufIndex[idx], struct.getByteSize());
//                    struct.baseOn(buff,indexMapBufIndex[idx]);
            }
            addEntry(idx,o.getId(),o);
        }
        if ( changeSource != null )
            changeSource.added(version, o);
        if ( listener != null )
            listener.added(version, o);
    }

    Bytez tryFindBuffer(int index) {
        return buffsMap[index];
    }

    int findNextFreeSlot() {
        if ( freeList.getSize() > 0 ) {
            int res = freeList.pop();
            return res;
        }
        return -1;
    }

    Thread lastThread;
    void checkThread() {
        if ( lastThread == null ) {
            lastThread = Thread.currentThread();
            return;
        }
        if ( Thread.currentThread() != lastThread ) {
            System.out.println("Invalid Multithreading "+lastThread.getName()+" current:"+Thread.currentThread().getName());
            Thread.dumpStack();
        }
    }

    int addEntry(int index, long key, RLRow row) {
        usedSlots.set(index,true);
        checkThread();
        rowBufMap.put(key,index);
        buffsMap[index] = ((FSTStruct)row).___bytes;
        usedSlots.set(index, true);
        indexMapBufIndex[index] = (int) ((FSTStruct)row).getOffset();
        indexMapKeyIndex[index] = key;
        index++;
        if ( index == buffsMap.length ) {
            Bytez newBMap[] = new Bytez[buffsMap.length*2];
            System.arraycopy(buffsMap,0,newBMap,0,buffsMap.length);

            int newIMap[] = new int[buffsMap.length*2];
            System.arraycopy(indexMapBufIndex,0,newIMap,0,buffsMap.length);

            long newKMap[] = new long[buffsMap.length*2];
            System.arraycopy(indexMapKeyIndex,0,newKMap,0,buffsMap.length);

            BitSet newSet = new BitSet(buffsMap.length*2);
            newSet.or(usedSlots);
            usedSlots = newSet;
            freeList.resize(buffsMap.length * 2);
            indexMapBufIndex = newIMap;
            indexMapKeyIndex = newKMap;
            buffsMap = newBMap;
        }
        return index;
    }

    ArrayList<StableVersionRowIterator> runningIterations = new ArrayList();

    void predeliver(int index) {
        for (int i = 0; i < runningIterations.size(); i++) {
            StableVersionRowIterator stableVersionRowIterator = runningIterations.get(i); // FIXME: concurrency related NPE
            stableVersionRowIterator.predeliver(index);
        }
    }

    @Override
    public <T extends RLRow> FiberRunnable iterate(final RLRowVisitor<T> iter) {
        StableVersionRowIterator iterator = new StableVersionRowIterator(iter, getVersionAtomic());
        runningIterations.add(iterator);
        System.out.println("start iteration count " + runningIterations.size());
        return iterator;
    }

    @Override
    public void preUpdate(long version, FSTStructChange change, T oldRow) {
        // no filtering done by this class
    }

    @Override
    public void updated(long rowId, FSTStructChange change, T updatedRow) {
        T target;
        long vers = versionAtomic.incrementAndGet();
        target = get(rowId);
        if ( target == null )
            return;
        predeliver(rowBufMap.get(rowId));
        if ( changeSource != null )
            changeSource.preUpdate(vers, change, target);
        if ( listener != null )
            listener.preUpdate(vers, change, target);

        change.applySnapshot((FSTStruct) target);

        if ( changeSource != null )
            changeSource.updated(vers, change, target);
        if ( listener != null )
            listener.updated(rowId, change, target);
    }

    @Override
    public void removed(long version, T row) {
        checkThread();
        version = versionAtomic.incrementAndGet();
        long id = row.getId();
        if (rowBufMap.containsKey(id) ) {
            int index = rowBufMap.get(id);
            predeliver(index);
            rowBufMap.remove(id);
            if ( changeSource != null ) {
                changeSource.removed(version, (T) allocator.getVolatileStructPointer(buffsMap[index], indexMapBufIndex[index]));
            }
            if ( listener != null ) {
                listener.removed(version, (T) allocator.getVolatileStructPointer(buffsMap[index], indexMapBufIndex[index]));
            }
            //indexMapBufIndex[index] = -1; wg. reuse
            //buffsMap[index] = null;
            usedSlots.set(index, false);
            freeList.push(index);
            removed++;
        }
    }

    @Override
    public void queryFinished(Object error) {
        // FIXME: listeners should not received query finished until this is received !
    }

    public int size() {
        return indexMapIndex-removed;
    }

    public T getTemplate() {
        return template;
    }

    public RLSubscription getSub() {
        return sub;
    }

    public void setSub(RLSubscription sub) {
        this.sub = sub;
    }

    @Override
    public void unsubscribe() {
        sub.unsubscribe();
        sub = null;
    }

    //FIXME: concurrent adds should be ignored !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    class StableVersionRowIterator<T extends RLRow> implements RLIterationContext, FiberRunnable {
        final HashSet<Integer> predelivered = new HashSet<>(); // contains index
        final RLRowVisitor<T> iter;
        final int maxIdx;
        volatile boolean terminated;
        final long version;

        StableVersionRowIterator(RLRowVisitor<T> iter, long vers) {
            this.iter = iter;
            maxIdx = indexMapIndex;
            version = vers;
        }

        public void predeliver(int index) {
            if ( predelivered.contains(index) )
                return;
            processObject( index );
            predelivered.add(index);
        }

        private void terminate(Object error) {
            terminated = true;
            runningIterations.remove(this);
            System.out.println("removed iteration, size "+runningIterations.size());
            iter.terminated(error);
        }

        int processObject(int i) { // always in lock
            final Bytez buff;
            final int offset;
            buff = buffsMap[i];
            offset = indexMapBufIndex[i];
            FSTStruct volatileStructPointer = allocator.getVolatileStructPointer(buff, offset);
            return iter.processObject(indexMapKeyIndex[i], (T) volatileStructPointer, this);
        }

        @Override
        public long getCurrentVersion() {
            return version;
        }

        int index = 0;
        @Override
        public void tick(FiberPool p) {
            Object error = null;
            for (int i = index; i < maxIdx; i++) {

                if ( predelivered.contains(i) )
                    continue;
                if (usedSlots.get(i) ) {
                    try {
                        int deliverd = processObject(i);
                        if (deliverd > 0) {
                            index=i+1;
                            return;
                        }
                    } catch (Throwable th) {
                        terminated = true;
                        error = th;
                        break;
                    }
                }
                if ( terminated ) {
                    break;
                }
                if (i==index+ FIBER_ITERATIONS){
                    index = i;
                    return;
                }
                index++;
            }
            if (index>=maxIdx-1||terminated) {
                terminate(error);
                p.finished();
            }
        }
    }


}
