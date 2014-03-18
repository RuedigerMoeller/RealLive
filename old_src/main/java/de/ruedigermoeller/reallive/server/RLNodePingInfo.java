package de.ruedigermoeller.reallive.server;

import de.ruedigermoeller.serialization.annotations.Serialize;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.Serializable;

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
 * Date: 21.11.13
 * Time: 20:39
 * To change this template use File | Settings | File Templates.
 */
public class RLNodePingInfo implements Serializable {

    Int2IntOpenHashMap tableId2NumRows = new Int2IntOpenHashMap(100);
    int nodeMask;
    long freeMem = 0;
    String sender;

    public RLNodePingInfo(int nodeMask, long freeMem, String sender) {
        this.nodeMask = nodeMask;
        this.freeMem = freeMem;
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void putRowNum( int tableId, int rowCount ) {
        tableId2NumRows.put(tableId,rowCount);
    }

    public int getRowNum( int tableId ) {
        if ( ! tableId2NumRows.containsKey(tableId) ) {
            return 0;
        }
        return tableId2NumRows.get(tableId);
    }

    public long getNodeMask() {
        return nodeMask;
    }

    public void setNodeMask(int nodeMask) {
        this.nodeMask = nodeMask;
    }

    public long getFreeMem() {
        return freeMem;
    }

    public void setFreeMem(int freeMem) {
        this.freeMem = freeMem;
    }

    @Override
    public String toString() {
        return "RLNodePingInfo{" +
                "tableId2NumRows=" + tableId2NumRows +
                ", nodeMask=" + nodeMask +
                ", freeMem=" + freeMem/1024/1024 + " MB"+
                ", sender='" + sender + '\'' +
                '}';
    }
}
