package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.heapoff.structs.structtypes.StructString;
import de.ruedigermoeller.reallive.facade.collection.annotations.DisplayName;
import de.ruedigermoeller.reallive.facade.collection.TableName;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.impl.RLStructRow;
import de.ruedigermoeller.reallive.server.RealLiveTopicService;

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
 * Date: 25.12.13
 * Time: 02:09
 * To change this template use File | Settings | File Templates.
 */
public class SystemSchema {

    public static void create(RealLiveTopicService db) {
        db.createTable( new RLTableConfiguration(1,new User()));
        db.createTable( new RLTableConfiguration(2,new Session() ));
        db.createTable( new RLTableConfiguration(3,new Table()));
    }

    @TableName("sys.Table")
    public static class Table extends RLStructRow {
        protected StructString name = new StructString(100);
        protected StructString clz = new StructString(100);
        protected int tableId;
        protected int numElements = 0;

        public StructString getName() {
            return name;
        }

        public int getTableId() {
            return tableId;
        }

        public void setTableId(int tableId) {
            this.tableId = tableId;
        }

        public StructString getClz() {
            return clz;
        }

        public int getNumElements() {
            return numElements;
        }
    }

    @TableName("sys.User")
    public static class User extends RLStructRow {

        @DisplayName("UserID")
        protected StructString uid = new StructString(12);
        @DisplayName("Name")
        protected StructString fullname = new StructString(32);
        protected StructString password = new StructString(10);
        protected long created;
        protected long rights;
        protected int userData;

        public User() {
        }

        public StructString getUid() {
            return uid;
        }

        public StructString getFullname() {
            return fullname;
        }

        public StructString getPassword() {
            return password;
        }

        public long getCreated() {
            return created;
        }

        public void setCreated(long created) {
            this.created = created;
        }

        public long getRights() {
            return rights;
        }

        public void setRights(long rights) {
            this.rights = rights;
        }

        public int getUserData() {
            return userData;
        }

        public void setUserData(int userData) {
            this.userData = userData;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", collectionId=" + collectionId +
                    ", uid=" + uid +
                    ", fullname=" + fullname +
                    ", password=" + password +
                    ", created=" + created +
                    ", rights=" + rights +
                    ", userData=" + userData +
                    '}';
        }
    }

    @TableName("sys.Session")
    public static class Session extends RLStructRow {

        protected StructString userId = new StructString(12);
        protected long sessionCreation;

        public StructString getUserId() {
            return userId;
        }

        public void setUserId(StructString userId) {
            this.userId = userId;
        }

        public long getSessionCreation() {
            return sessionCreation;
        }

        public void setSessionCreation(long sessionCreation) {
            this.sessionCreation = sessionCreation;
        }
    }

}
