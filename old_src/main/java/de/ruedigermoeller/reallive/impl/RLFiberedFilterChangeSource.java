package de.ruedigermoeller.reallive.impl;

import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.util.fibers.FiberPool;
import de.ruedigermoeller.reallive.util.fibers.FiberRunnable;

import java.util.ArrayList;

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
 * Date: 02.11.13
 * Time: 14:20
 * To change this template use File | Settings | File Templates.
 */

/**
 * In-Process only
 * @param <T>
 */
public class RLFiberedFilterChangeSource<T extends RLRow> implements RLChangeSource<T>, RLChangeTarget<T> {

    class ListenerEntry<T extends RLRow> implements RLSubscription {
        ListenerEntry(RLRowMatcher<T> matcher, RLChangeTarget<T> listener) {
            this.matcher = matcher;
            this.listener = listener;
        }

        RLRowMatcher<T> matcher;
        RLChangeTarget<T> listener;
        boolean preMatched;

        @Override
        public void unsubscribe() {
            listenerList.remove(this);
        }
    }

    ArrayList<ListenerEntry<T>> listenerList = new ArrayList<>();
    RLCollection collection;

    public RLFiberedFilterChangeSource(RLCollection coll) {
        collection = coll;
    }

    @Override
    public int getCollectionId() {
        return collection.getCollectionId();
    }

    @Override
    public void select(final RLRowMatcher<T> matcher, final RLChangeTarget<T> listener) {
        throw new RuntimeException("not implemeted");
    }

    public FiberRunnable selectFibered(final RLRowMatcher<T> matcher, final RLChangeTarget<T> listener) {
        return collection.iterate(new RLRowVisitor<T>() {
            @Override
            public int processObject(long id, T o, RLIterationContext context) {
                if (matcher.matches(o)) {
                    listener.added(context.getCurrentVersion(), o);
                    return 1;
                }
                return 0;
            }

            @Override
            public void terminated(Object error) {
                listener.queryFinished(error);
            }
        });
    }

    @Override
    public RLSubscription subscribe(RLRowMatcher<T> matcher, RLChangeTarget<T> listener) {
//        ListenerEntry<T> en = new ListenerEntry<>(matcher, listener);
//        select(matcher,listener);
//        listenerList.$add(en);
//        return en;
        throw new RuntimeException("not implemented");
    }

    @Override
    public long createVersion() {
        return collection.createVersion();
    }

    public long getVersion() {
        return collection.getVersionAtomic();
    }

    @Override
    public void added(long version, T added) {
        for (int i = 0; i < listenerList.size(); i++) {
            ListenerEntry<T> tListenerEntry = listenerList.get(i);
            if ( tListenerEntry.matcher.matches(added) ) {
                tListenerEntry.listener.added(version, added);
            }
        }
    }

    @Override
    public void preUpdate(long version, FSTStructChange change, T oldRow) {
        for (int i = 0; i < listenerList.size(); i++) {
            ListenerEntry<T> tListenerEntry = listenerList.get(i);
            tListenerEntry.listener.preUpdate(version, change, oldRow);
            tListenerEntry.preMatched = tListenerEntry.matcher.matches(oldRow);
        }
    }

    @Override
    public void updated(long version, FSTStructChange change, T updatedRow) {
        for (int i = 0; i < listenerList.size(); i++) {
            ListenerEntry<T> tListenerEntry = listenerList.get(i);
            boolean matched = tListenerEntry.matcher.matches(updatedRow);
            if ( tListenerEntry.preMatched ) {
                if ( matched ) {
                    tListenerEntry.listener.updated(version, change, updatedRow);
                } else {
                    tListenerEntry.listener.removed(version, updatedRow);
                }
            } else {
                if ( matched ) {
                    tListenerEntry.listener.added(version, updatedRow);
                }
            }
        }
    }

    @Override
    public void removed(long version, T removedPreUpdate) {
        for (int i = 0; i < listenerList.size(); i++) {
            ListenerEntry<T> tListenerEntry = listenerList.get(i);
            boolean matched = tListenerEntry.matcher.matches(removedPreUpdate);
            if ( matched ) {
                tListenerEntry.listener.removed(version, removedPreUpdate);
            }
        }
    }

    @Override
    public void queryFinished(Object error) {
        for (int i = 0; i < listenerList.size(); i++) {
            ListenerEntry<T> tListenerEntry = listenerList.get(i);
            tListenerEntry.listener.queryFinished(error);
        }
    }

}
