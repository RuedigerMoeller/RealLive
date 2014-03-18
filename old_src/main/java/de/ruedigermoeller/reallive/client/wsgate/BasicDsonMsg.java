package de.ruedigermoeller.reallive.client.wsgate;

import de.ruedigermoeller.serialization.dson.Dson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

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
 * Date: 20.12.13
 * Time: 21:31
 * To change this template use File | Settings | File Templates.
 */
public class BasicDsonMsg {
    int reqId;
    int respToId;
    transient ChannelHandlerContext ctx;

    public String toDson() {
        return Dson.getInstance().writeObject(this);
    }

    public void reply( BasicDsonMsg response ) {
        response.setRespToId(reqId);
        ctx.writeAndFlush( new TextWebSocketFrame( response.toDson() ) );
    }

    public int getReqId() {
        return reqId;
    }

    public void setReqId(int reqId) {
        this.reqId = reqId;
    }

    public int getRespToId() {
        return respToId;
    }

    public void setRespToId(int respToId) {
        this.respToId = respToId;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

}
