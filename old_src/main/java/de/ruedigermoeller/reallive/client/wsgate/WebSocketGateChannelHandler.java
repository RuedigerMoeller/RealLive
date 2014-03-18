package de.ruedigermoeller.reallive.client.wsgate;

import de.ruedigermoeller.reallive.client.wsgate.protocol.AuthReq;
import de.ruedigermoeller.reallive.client.wsgate.protocol.ErrorMsg;
import de.ruedigermoeller.serialization.dson.Dson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
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
* Date: 23.12.13
* Time: 01:09
* To change this template use File | Settings | File Templates.
*/
public class WebSocketGateChannelHandler extends ChannelInboundHandlerAdapter {

    private WebSocketGate webSocketGate;

    public WebSocketGateChannelHandler(WebSocketGate webSocketGate) {
        this.webSocketGate = webSocketGate;
    }

    /**
     * Gets called after the {@link io.netty.channel.ChannelHandler} was added to the actual context and it's ready to handle events.
     *
     * @param ctx
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler added "+ctx);
        ctx.attr(webSocketGate.session).set(webSocketGate.createEmptySession());
        super.handlerAdded(ctx);
    }

    /**
     * Gets called after the {@link io.netty.channel.ChannelHandler} was removed from the actual context and it doesn't handle events
     * anymore.
     *
     * @param ctx
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Handler removed "+ctx);
        ctx.attr(webSocketGate.session).set(null);
        super.handlerRemoved(ctx);
    }

    /**
     * Calls {@link io.netty.channel.ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link io.netty.channel.ChannelInboundHandler} in the {@link io.netty.channel.ChannelPipeline}.
     * <p/>
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Check for closing frame
        if (msg instanceof CloseWebSocketFrame) {
            webSocketGate.getHandshaker().close(ctx.channel(), (CloseWebSocketFrame) msg);
            return;
        }
        if (msg instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(((PingWebSocketFrame) msg).content()));
            return;
        }
        if (!(msg instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", msg.getClass()
                    .getName()));
        }
        if ( msg instanceof TextWebSocketFrame) {
            WSGSession wsgs = ctx.attr(webSocketGate.session).get();
            TextWebSocketFrame wf = (TextWebSocketFrame) msg;
            BasicDsonMsg dsonMsg = null;
            try {
                System.out.println(wf.text());
                dsonMsg = (BasicDsonMsg) Dson.getInstance().readObject(wf.text());
                dsonMsg.setCtx(ctx);
            } catch ( Exception ex ) {
                ctx.writeAndFlush(
                        new TextWebSocketFrame(
                            new ErrorMsg( 1, "failed to parse request:" + ex.getMessage(), 0, 0 ).toDson()
                        )
                );
                ex.printStackTrace();
                return;
            }

            if ( dsonMsg instanceof AuthReq == false && ! wsgs.isAuthenticated() ) {
                ctx.writeAndFlush(
                    new TextWebSocketFrame(
                        new ErrorMsg( 2, "not authenticated", 0, dsonMsg.getReqId() ).toDson()
                    )
                );
            } else {
                wsgs.processRequest(dsonMsg);
//                    ChannelFuture channelFuture = ctx.writeAndFlush(new TextWebSocketFrame("Hello from java"));
//                    channelFuture.addListener(new GenericFutureListener<ChannelFuture>() {
//                        @Override
//                        public void operationComplete(ChannelFuture future) throws Exception {
//                            System.out.println("send success");
//                        }
//                    });
            }
        } else
            System.out.println("read "+msg);
        super.channelRead(ctx, msg);
    }

    /**
     * Gets called if a {@link Throwable} was thrown.
     *
     * @param ctx
     * @param cause
     * @deprecated Will be removed in the future and only {@link io.netty.channel.ChannelInboundHandler} will receive
     * exceptionCaught events. For {@link io.netty.channel.ChannelOutboundHandler} the {@link io.netty.channel.ChannelPromise}
     * must be failed.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Handler exceptionCaught " + cause);
    }
}
