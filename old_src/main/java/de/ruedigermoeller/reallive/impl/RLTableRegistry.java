package de.ruedigermoeller.reallive.impl;

import de.ruedigermoeller.reallive.facade.collection.RLChangeSource;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.facade.database.RealLive;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.HashMap;

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
 * Time: 19:12
 * To change this template use File | Settings | File Templates.
 */
public abstract class RLTableRegistry implements RealLive {

    protected Int2ObjectOpenHashMap<RLTableConfiguration> tables= new Int2ObjectOpenHashMap<>(97);
    protected HashMap<Class,Object> clazzUniqueMap = new HashMap<>();
    protected HashMap<String,RLTableConfiguration> stringUniqueMap = new HashMap<>();

    public void createTable(RLTableConfiguration configuration) {
        tables.put(configuration.getTableId(), configuration);
        stringUniqueMap.put(configuration.getTableName(),configuration);
        Object aId = clazzUniqueMap.get(configuration.getRowClass());
        if ( aId == null ) {
            clazzUniqueMap.put(configuration.getRowClass(), configuration.getTableId());
        } else {
            clazzUniqueMap.put(configuration.getRowClass(), Boolean.FALSE);
        }
    }

    public void dropTable(int tableId) {
        RLTableConfiguration conf = tables.get(tableId);
        if ( conf != null ) {
            tables.remove(tableId);
        }
    }

    public RLTableConfiguration getTableConfiguration(int tableId) {
        return tables.get(tableId);
    }

    public RLTableConfiguration[] getTableConfigurations() {
        RLTableConfiguration conf[] = new RLTableConfiguration[tables.size()];
        int count = 0;
        for ( RLTableConfiguration c : tables.values() ) {
            conf[count++] = c;
        }
        return conf;
    }

    @Override
    public <T extends RLRow> RLChangeSource<T> getTable(Class<T> rowClazz) {
        Object val = clazzUniqueMap.get(rowClazz);
        if ( val instanceof Integer ) {
            return getTable(((Integer) val).intValue());
        }
        return null;
    }

    @Override
    public <T extends RLRow> RLChangeSource<T> getTable(String name) {
        return getTable(stringUniqueMap.get(name).getTableId());
    }
}
