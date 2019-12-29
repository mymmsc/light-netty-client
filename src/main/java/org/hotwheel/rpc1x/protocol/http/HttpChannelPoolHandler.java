package org.hotwheel.rpc1x.protocol.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateEvent;
import org.hotwheel.rpc1x.core.RpcResponseFuture;
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
            setPendingResponse(ctx.channel(), headers);
        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            RpcResponseFuture.setPendingContent(ctx.channel(), httpContent.content().retain());
            if (httpContent instanceof LastHttpContent) {
                boolean connectionClose = headerContainConnectionClose(ctx.channel());
                RpcResponseFuture.done(ctx.channel());
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

    private void setPendingResponse(Channel channel, HttpResponse pendingResponse) {
        RpcResponseFuture responseFuture = RpcResponseFuture.getResponse(channel);
        NettyHttpResponseBuilder responseBuilder = new NettyHttpResponseBuilder();
        responseBuilder.setSuccess(true);
        responseBuilder.setPendingResponse(pendingResponse);
        responseFuture.setResponseBuilder(responseBuilder);
    }

    private boolean headerContainConnectionClose(Channel channel) {
        RpcResponseFuture responseFuture = RpcResponseFuture.getResponse(channel);
        return HttpHeaders.Values.CLOSE.equalsIgnoreCase(responseFuture.getResponseBuilder()
                .getPendingResponse().headers().get(HttpHeaders.Names.CONNECTION));
    }
}
