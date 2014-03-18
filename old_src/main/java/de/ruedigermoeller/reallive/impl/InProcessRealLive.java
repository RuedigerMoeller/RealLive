package de.ruedigermoeller.reallive.impl;

import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.facade.database.RealLive;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
 * Date: 03.11.13
 * Time: 20:32
 * To change this template use File | Settings | File Templates.
 */
public class InProcessRealLive extends RLTableRegistry implements RealLive {

    int nodeId = 0;
    TableThreadPool perTableThreadPool = new TableThreadPool();

    Int2ObjectOpenHashMap<RLStructCollection> cols = new Int2ObjectOpenHashMap<>(97);

    public InProcessRealLive(int nodeId) {
        this.nodeId = nodeId;
    }

    public void createTable(RLTableConfiguration configuration) {
        super.createTable(configuration);
        RLStructCollection col = new RLStructCollection(configuration.getTableId(), configuration.getTemplate());
        cols.put(configuration.getTableId(),col);
    }

    public void dropTable(int tableId) {
        RLTableConfiguration conf = tables.get(tableId);
        if ( conf != null ) {
            cols.remove(tableId);
        }
    }


    @Override
    public <T extends RLRow> RLChangeSource<T> getTable(final int tableId) {
        final RLStructCollection rlStructCollection = cols.get(tableId);
        if ( rlStructCollection != null ) {
            final RLFiberedFilterChangeSource changeSource = rlStructCollection.getFilteredChangeSource();
            return new RLChangeSource<T>() {

                @Override
                public int getCollectionId() {
                    return tableId;
                }

                @Override
                public void select(final RLRowMatcher<T> matcher, final RLChangeTarget<T> listener) {
                    perTableThreadPool.getExecutor(tableId).addFiber(
                            changeSource.selectFibered(matcher, listener)
                    );
                }

                @Override
                public RLSubscription subscribe(final RLRowMatcher<T> matcher, final RLChangeTarget<T> listener) {
                    throw new RuntimeException("not implemented. No in process subscription supported");
                    // needs adaption for fibers
//                    final AtomicReference<RLSubscription> res = new AtomicReference<>();
//                    perTableThreadPool.execute(tableId, new Runnable() {
//                        @Override
//                        public void run() {
//                            res.set(changeSource.subscribe(matcher, listener));
//                        }
//                    });
//                    while(res.get()==null)
//                        ;
//                    return res.get();
                }

                @Override
                public long createVersion() {
                    return rlStructCollection.createVersion();
                }

                @Override
                public long getVersion() {
                    return rlStructCollection.getVersionAtomic();
                }

            };
        }
        return null;
    }

    public <T extends RLRow> RLCollection getCol(Class<T> rowClazz) {
        Object val = clazzUniqueMap.get(rowClazz);
        if ( val instanceof Integer ) {
            return getCol(((Integer) val).intValue());
        }
        return null;
    }

    public <T extends RLRow> RLCollection getCol(int tableId) {
        return cols.get(tableId);
    }


    @Override
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror(int tableId, RLRowMatcher<T> filter) {
        final RLStructCollection<T> rlStructCollection = cols.get(tableId);
        if (rlStructCollection==null) {
            return null;
        }
        return new RLStructCollection<T>(tableId,rlStructCollection.getTemplate());
    }

    @Override
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror(Class<T> rowClazz, RLRowMatcher<T> filter) {
        final RLStructCollection<T> rlStructCollection = (RLStructCollection<T>) getCol(rowClazz);
        if (rlStructCollection==null) {
            return null;
        }
        final RLStructCollection<T> mirror = new RLStructCollection<T>(rlStructCollection.getCollectionId(), rlStructCollection.getTemplate());
        rlStructCollection.getChangeSource().subscribe(filter,mirror);
        return mirror;
    }

    @Override
    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(int tableId) {
        return cols.get(tableId).getMutator(perTableThreadPool.getExecutor(tableId));
    }

    @Override
    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(Class<T> rowClazz) {
        Object val = clazzUniqueMap.get(rowClazz);
        if ( val instanceof Integer ) {
            RLStructCollection col = cols.get(((Integer) val).intValue());
            if ( col == null ) {
                return null;
            }
            return col.getMutator(perTableThreadPool.getExecutor(col.getCollectionId()));
        }
        return null;
    }
}
