package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.fastcast.remoting.*;
import de.ruedigermoeller.fastcast.util.FCLog;
import de.ruedigermoeller.reallive.facade.collection.RLChangeTarget;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Time: 16:21
 * To change this template use File | Settings | File Templates.
 */
@PerSenderThread(false)
public class RealLiveClientTopicService extends FCTopicService {

    ConcurrentHashMap<Integer,RunningSelect> runningSelects = new ConcurrentHashMap<>();

    public void registerSelect(int selId, int expectedQFs, RLChangeTarget rec, SubscriptionEntry entry) {
        runningSelects.put(selId,new RunningSelect(rec,expectedQFs,entry));
    }

    /**
     * override to do init and stuff
     */
    @Override
    public void init() {
    }

    @RemoteMethod(1)
    public void receiveSelectAdd(int subsId, long version, RLRow added) {
        final RunningSelect target = runningSelects.get(subsId);
        if (target==null) {
            FCLog.get().warn("no listener subscriber "+subsId+" for row "+added);
            return;
        }
        target.receiveSelectAdd(version, added);
    }

    @RemoteMethod(2)
    public void receiveSelectQueryFinished(int subsId, long version, Object error) {
        final RunningSelect target = runningSelects.get(subsId);
        if (target==null) {
            FCLog.get().warn("no listener for " + subsId + " for query finished");
            return;
        }
        target.receiveSelectQueryFinished(FCReceiveContext.get().getSender(), subsId, version, error);
    }

    public class RunningSelect {
        long started = System.currentTimeMillis();
        RLChangeTarget receiver;
        ConcurrentHashMap<String, Object> senders = new ConcurrentHashMap<>(29); // maps to boolean.true or Long version of queryFin
        AtomicInteger queryFinished = new AtomicInteger(0);
        volatile int expectedQFs;
        SubscriptionEntry entry;
        Object someError = null;

        public RunningSelect(RLChangeTarget receiver, int expectedQFs, SubscriptionEntry entry) {
            this.receiver = receiver;
            this.expectedQFs = expectedQFs;
            this.entry = entry;
        }

        public void receiveSelectAdd(long version, RLRow added) {
            String sender = FCReceiveContext.get().getSender();
            synchronized (this) {
                senders.put(sender,Boolean.TRUE);
                receiver.added(version,added);
            }
        }

        public void receiveSelectQueryFinished(String sender0, int subsId, long version, Object error) {
            if ( error != null )
                someError = error;
            int qfCount = queryFinished.incrementAndGet();
            senders.put(sender0,version);
            synchronized (this) {
                int size = senders.size();
                if ( size > expectedQFs ) {
                    FCLog.get().warn("expected "+expectedQFs+" query finished, but got "+size);
                    expectedQFs = size;
                }
                if ( qfCount == expectedQFs ) {
                    runningSelects.remove(subsId);
                    receiver.queryFinished(someError);
                    if ( entry != null ) {
                        for (Iterator<String> iterator = senders.keySet().iterator(); iterator.hasNext(); ) {
                            String sender = iterator.next();
                            Object obj = senders.get(sender);
                            // fixme: blocks incoming broadcasts on this set
                            if ( obj instanceof Long)
                                entry.replay(sender, (Long)obj);
                            else
                                entry.replay(sender, -1l);
                        }
                    }
                }
            }
        }


    }
}
