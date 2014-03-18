package de.ruedigermoeller.reallive.impl;

import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.reallive.facade.collection.RLRow;

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
 * Date: 28.10.13
 * Time: 21:08
 * To change this template use File | Settings | File Templates.
 */
public class RLStructRow extends FSTStruct implements RLRow {

    protected long id;
    protected int collectionId;
    protected long version; // set version !

    public void setId( long val ) {
        id = val;
    }

    public long getId() {
        return id;
    }

    public int getTableId() {
        return collectionId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public void setCollectionId(int id) {
        collectionId = id;
    }

    @Override
    public String toString() {
        return "RLStructRow{" +
                "id=" + id +
                ", collectionId=" + collectionId +
                '}';
    }
}
