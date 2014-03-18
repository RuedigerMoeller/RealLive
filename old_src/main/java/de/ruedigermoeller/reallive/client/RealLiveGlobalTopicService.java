package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.fastcast.remoting.FCReceiveContext;
import de.ruedigermoeller.fastcast.remoting.FCTopicService;
import de.ruedigermoeller.fastcast.remoting.PerSenderThread;
import de.ruedigermoeller.fastcast.remoting.RemoteMethod;
import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.reallive.facade.collection.RLChangeTarget;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import de.ruedigermoeller.reallive.facade.collection.RLRowMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
 * Date: 10.11.13
 * Time: 03:50
 * To change this template use File | Settings | File Templates.
 */
@PerSenderThread(false)
public class RealLiveGlobalTopicService extends FCTopicService {

    ConcurrentHashMap<Integer,SubscriptionEntry> subsMap = new ConcurrentHashMap<>();
    HashMap<Integer,List<SubscriptionEntry>> tableId2Subs = new HashMap<>();

    int debugAdd = 0;
    @RemoteMethod(1)
    public void receiveGlobalAdd(long version, RLRow row ) {
        debugAdd++;
//        if ( debugAdd % 10000 == 0 ) {
//            System.out.println("global add msg "+debugAdd);
//        }
        List<SubscriptionEntry> li = tableId2Subs.get(row.getTableId());
        if ( li != null ) {
            GlobalMessage msg = null;
            for (int i = 0; i < li.size(); i++) {
                SubscriptionEntry subscriptionEntry = li.get(i);
                synchronized (subscriptionEntry) {
                    if ( subscriptionEntry.isRecording() ) {
                        if ( msg == null )
                            msg = new GlobalMessage(GlobalMsgType.ADD, version, row, null );
                        subscriptionEntry.record(FCReceiveContext.get().getSender(), msg);
                    } else {
                        subscriptionEntry.dispatchAdd(FCReceiveContext.get().getSender(), version, row);
                    }
                }
            }
        }
    }

    @RemoteMethod(2)
    public void receiveGlobalRemove(long version, RLRow row ) {
        List<SubscriptionEntry> li = tableId2Subs.get(row.getTableId());
        if ( li != null ) {
            GlobalMessage msg = null;
            for (int i = 0; i < li.size(); i++) {
                SubscriptionEntry subscriptionEntry = li.get(i);
                synchronized (subscriptionEntry) {
                    if ( subscriptionEntry.isRecording() ) {
                        if ( msg == null )
                            msg = new GlobalMessage(GlobalMsgType.REM, version, row, null );
                        subscriptionEntry.record(FCReceiveContext.get().getSender(), msg);
                    } else {
                        subscriptionEntry.dispatchRem(FCReceiveContext.get().getSender(), version, row);
                    }
                }
            }
        }
    }

    @RemoteMethod(4)
    public void receiveGlobalUpdate(long version, FSTStructChange update, RLRow rowToUpdate ) {
        int tableId = rowToUpdate.getTableId();
        List<SubscriptionEntry> li = tableId2Subs.get(tableId);
        if ( li != null ) {
            GlobalMessage msg = null;
            for (int i = 0; i < li.size(); i++) {
                SubscriptionEntry subscriptionEntry = li.get(i);
                synchronized (subscriptionEntry) {
                    if ( subscriptionEntry.isRecording() ) {
                        if ( msg == null )
                            msg = new GlobalMessage(GlobalMsgType.PREUPD, version, rowToUpdate, null );
                        subscriptionEntry.record(FCReceiveContext.get().getSender(),msg);
                    } else {
                        subscriptionEntry.dispatchPreUpd(FCReceiveContext.get().getSender(), version, rowToUpdate);
                    }
                }
            }
            msg = null;
            rowToUpdate = (RLRow) ((FSTStruct)rowToUpdate).createCopy();
            update.applySnapshot((FSTStruct) rowToUpdate);
            for (int i = 0; i < li.size(); i++) {
                SubscriptionEntry subscriptionEntry = li.get(i);
                synchronized (subscriptionEntry) {
                    if ( subscriptionEntry.isRecording() ) {
                        if ( msg == null )
                            msg = new GlobalMessage(GlobalMsgType.UPD, version, rowToUpdate, update);
                        subscriptionEntry.record(FCReceiveContext.get().getSender(),msg);
                    } else {
                        subscriptionEntry.dispatchUpd(FCReceiveContext.get().getSender(),version, rowToUpdate, update);
                    }
                }
            }
        }
    }

    public <T extends RLRow> SubscriptionEntry registerSubscribe(int subsId, int tableId, RLChangeTarget<T> listener, RLRowMatcher<T> filter) {
        SubscriptionEntry subsEntry = new SubscriptionEntry(subsId, tableId, listener, filter);
        subsMap.put(subsId, subsEntry);
        synchronized (tableId2Subs ) {
            List<SubscriptionEntry> subscriptionEntries = tableId2Subs.get(tableId);
            if ( subscriptionEntries == null ) {
                subscriptionEntries = new ArrayList<>();
                tableId2Subs.put(tableId,subscriptionEntries);
            }
            subscriptionEntries.add(subsEntry);
        }
        return subsEntry;
    }

    public void unregisterSubscriber(int subsId) {
        subsMap.remove(subsId);
    }
}
