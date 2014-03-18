package de.ruedigermoeller.reallive.client.wsgate;

import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.structtypes.StructString;
import de.ruedigermoeller.reallive.client.RealLiveClient;
import de.ruedigermoeller.reallive.client.SystemSchema;
import de.ruedigermoeller.reallive.client.wsgate.protocol.Gen;
import de.ruedigermoeller.reallive.client.wsgate.protocol.Scheme;
import de.ruedigermoeller.reallive.client.wsgate.protocol.TableAttribute;
import de.ruedigermoeller.reallive.client.wsgate.protocol.TableMetaData;
import de.ruedigermoeller.reallive.facade.collection.annotations.Description;
import de.ruedigermoeller.reallive.facade.collection.annotations.DisplayName;
import de.ruedigermoeller.reallive.facade.collection.RLCollectionMutator;
import de.ruedigermoeller.reallive.facade.database.RLTableConfiguration;
import de.ruedigermoeller.reallive.impl.RLStructRow;
import de.ruedigermoeller.serialization.FSTClazzInfo;
import de.ruedigermoeller.serialization.FSTConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;

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
 * Date: 17.12.13
 * Time: 19:26
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketGate {

    final int port;
    final RealLiveClient client;
    WebSocketServerHandshaker handshaker;
    String sessionClass;

    public WebSocketGate(int port) {
        this.port = port;
        Gen.registerClasses();
        client = new RealLiveClient("wsgate");
        try {
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final RLCollectionMutator<SystemSchema.User> tableMutator = client.getTableMutator(SystemSchema.User.class);
        final ArrayList<SystemSchema.User> added = new ArrayList<>();
        for ( int i = 0; i < 100; i++ ) {
            SystemSchema.User user = tableMutator.allocateInstanceForAdd();
            user.getUid().setString("POK"+i);
            user.getFullname().setString("bla x "+i*2);
            tableMutator.add(user);
            added.add(user);
        }
        new Thread("RandomUSerMutator") {
            public void run() {
                while( true ) {
                    switch ((int) (Math.random()*3)) {
                        case 0: {
                            SystemSchema.User updated = added.get((int) (Math.random()*added.size()));
                            SystemSchema.User modified = tableMutator.getInstanceForUpdate(updated);
                            modified.getFullname().setString(Math.random()>.5?"BLA":"HUCKA");
                            tableMutator.update(modified);
                        } break;
                        case 1: {
                            SystemSchema.User updated = added.get((int) (Math.random()*added.size()));
                            SystemSchema.User modified = tableMutator.getInstanceForUpdate(updated);
                            modified.setCreated(System.currentTimeMillis());
                            tableMutator.update(modified);
//                            System.out.println("mutate "+modified);
                        } break;
                        case 2: {
                            SystemSchema.User updated = added.get((int) (Math.random()*added.size()));
                            SystemSchema.User modified = tableMutator.getInstanceForUpdate(updated);
                            modified.setRights((long) (Math.random()*10));
                            tableMutator.update(modified);
//                            System.out.println("mutate "+modified);
                        } break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }

    public WebSocketServerHandshaker getHandshaker() {
        return handshaker;
    }

    public RealLiveClient getClient() {
        return client;
    }

    public void run() {
        // param(1) just example to show the thread model with 2 connected clients while live coding
//        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // param(1) just example to show the thread model with 2 connected clients while live coding
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);
        try {

            // additional thread pool for blocking handler
//            final EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(8);

            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpObjectAggregator(65536),
                                    new HttpResponseEncoder(),
                                    new WebSocketServerProtocolHandler("/websocket"),
                                    new WebSocketGateChannelHandler(WebSocketGate.this)); // normal example without another thread pool

                            // register blocking or long lasting handler to additional thread pool
//                            ch.pipeline().addLast(executorGroup, new JSUGWebSocketHandler(channels));
                        }
                    });

            final Channel channel;
            channel = bootstrap.bind(port).sync().channel();

            System.out.println("server started on port: " + port);
            channel.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();

        } finally {
            workerGroup.shutdown();
        }
    }


    AttributeKey<WSGSession> session = AttributeKey.valueOf("RLSESSION");

    public WSGSession createEmptySession() {
        if (sessionClass != null ) {
            try {
                return (WSGSession) Class.forName(sessionClass).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return new WSGSession(this);
    }

    public int getPort() {
        return port;
    }

    public String getSessionClass() {
        return sessionClass;
    }

    public void setSessionClass(String sessionClass) {
        this.sessionClass = sessionClass;
    }

    public static void main(String arg[]) {
        new WebSocketGate(8089).run();
    }

    public Scheme getMetaData() {
        RLTableConfiguration[] tableConfigurations = client.getTableConfigurations();
        TableMetaData res[] = new TableMetaData[tableConfigurations.length];
        for (int i = 0; i < tableConfigurations.length; i++) {
            RLTableConfiguration tableConfiguration = tableConfigurations[i];
            TableMetaData data = new TableMetaData();
            data.setTableId(tableConfiguration.getTableId());
            data.setTableName(tableConfiguration.getTableName());
            data.setDescription(tableConfiguration.getDescription());
            data.setClassName(tableConfiguration.getRowClass().getName());

            FSTClazzInfo clazzInfo = FSTConfiguration.getDefaultConfiguration().getClazzInfo(tableConfiguration.getRowClass());
            FSTClazzInfo.FSTFieldInfo[] fieldInfo = clazzInfo.getFieldInfoFiltered( Object.class, FSTStruct.class, RLStructRow.class );
            TableAttribute atts[] = new TableAttribute[fieldInfo.length];
            Object sampleInstance = null;
            try {
                sampleInstance = clazzInfo.getClazz().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < fieldInfo.length; j++) {
                FSTClazzInfo.FSTFieldInfo fstFieldInfo = fieldInfo[j];
                Object initAttribute = null;
                if ( sampleInstance != null ) {
                    try {
                        initAttribute = fstFieldInfo.getObjectValue(sampleInstance);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                TableAttribute attribute = new TableAttribute();
                attribute.setName(fstFieldInfo.getField().getName());
                Class attrType = fstFieldInfo.getType();
                attribute.setType(mapAttrType(initAttribute, attrType));

                if ( attrType == StructString.class ) {
                    attribute.setMaxLen(((StructString)initAttribute).charsLen());
                }

                DisplayName displayName = (DisplayName) fstFieldInfo.getField().getAnnotation(DisplayName.class);
                if ( displayName != null ) {
                    attribute.setDisplayName(displayName.value());
                } else {
                    attribute.setDisplayName(fstFieldInfo.getField().getName());
                }

                Description desc = (Description) fstFieldInfo.getField().getAnnotation(Description.class);
                if ( desc != null )
                    attribute.setDescription(desc.value());

                atts[j] = attribute;
            }
            data.setAttributes(atts);
            res[i] = data;
        }
        return new Scheme( res );
    }

    protected String mapAttrType(Object emptyInstance, Class type) {
        if ( type == StructString.class ) {
            return "String";
        }
        return type.getSimpleName();
    }
}
