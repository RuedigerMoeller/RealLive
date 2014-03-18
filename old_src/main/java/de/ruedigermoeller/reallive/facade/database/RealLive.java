package de.ruedigermoeller.reallive.facade.database;

import de.ruedigermoeller.reallive.facade.collection.*;
import de.ruedigermoeller.reallive.impl.RLStructRow;

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
 * Time: 19:34
 * To change this template use File | Settings | File Templates.
 */
public interface RealLive {

    public void createTable(RLTableConfiguration configuration); // FIXME: errormsg
    public void dropTable( int tableId ); // FIXME: errormsg
    public RLTableConfiguration getTableConfiguration( int tableId );
    public RLTableConfiguration[] getTableConfigurations();

    public <T extends RLRow> RLCollectionMutator<T> getTableMutator( int tableId );
    public <T extends RLRow> RLCollectionMutator<T> getTableMutator(Class<T> rowClazz);
    public <T extends RLRow> RLChangeSource<T> getTable( int tableId );
    public <T extends RLRow> RLChangeSource<T> getTable(Class<T> rowClazz);
    public <T extends RLRow> RLChangeSource<T> getTable(String name);
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror( int tableId, RLRowMatcher<T> filter );
    public <T extends RLRow> RLMirrorCollection<T> getFilteredMirror( Class<T> rowClazz , RLRowMatcher<T> filter );


}
