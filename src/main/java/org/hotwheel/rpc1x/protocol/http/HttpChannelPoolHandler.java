package org.hotwheel.rpc1x.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateEvent;
import org.hotwheel.rpc1x.core.RpcFuture;
import org.hotwheel.rpc1x.pool.RpcChannelPool;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpChannelPoolHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger logger = Logger.getLogger(HttpChannelPoolHandler.class.getName());

    private RpcChannelPool channelPool;

    /**
     * @param channelPool
     */
    public HttpChannelPoolHandler(RpcChannelPool channelPool) {
        super();
        this.channelPool = channelPool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse headers = (HttpResponse) msg;
            RpcFuture.setPendingResponse(ctx.channel(), headers);
        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            RpcFuture.setPendingContent(ctx.channel(), httpContent);
            if (httpContent instanceof LastHttpContent) {
                boolean connectionClose = RpcFuture.headerContainConnectionClose(ctx.channel());
                RpcFuture.done(ctx.channel());
                //the maxKeepAliveRequests config will cause server close the channel, and return 'Connection: close' in headers                
                if (!connectionClose) {
                    channelPool.returnChannel(ctx.channel());
                }
            }
        }
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#userEventTriggered(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            logger.log(Level.WARNING, "remove idle channel: " + ctx.channel());
            ctx.channel().close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * @param channelPool the pool to set
     */
    public void setChannelPool(RpcChannelPool channelPool) {
        this.channelPool = channelPool;
    }
}
