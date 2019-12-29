package org.hotwheel.rpc1x.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponseBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RPC Future
 *
 * @author wangfeng
 * @date 2019-12-27
 */
public class RpcFuture<T extends RpcResponse>/* implements Future<T>*/ {
    public static final AttributeKey<Object> DEFAULT_ATTRIBUTE = AttributeKey.valueOf("nettyResponse");
    public static final AttributeKey<Object> ROUTE_ATTRIBUTE = AttributeKey.valueOf("route");
    public static final AttributeKey<Object> FORCE_CONNECT_ATTRIBUTE = AttributeKey.valueOf("forceConnect");

    protected final AtomicBoolean isProcessed = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Channel channel;
    private volatile boolean isDone = false;
    private volatile boolean isCancel = false;
    private volatile ResponseBuilder<T> responseBuilder;

    public boolean cancel(Throwable cause) {
        if (isProcessed.getAndSet(true)) {
            return false;
        }
        responseBuilder = new NettyHttpResponseBuilder();
        responseBuilder.setSuccess(false);
        responseBuilder.setCause(cause);
        isCancel = true;
        latch.countDown();
        return true;
    }

    public ResponseBuilder<T> getResponseBuilder() {
        return responseBuilder;
    }

    public void setResponseBuilder(ResponseBuilder<T> responseBuilder) {
        this.responseBuilder = responseBuilder;
    }

    public boolean done() {
        if (isProcessed.getAndSet(true)) {
            return false;
        }
        isDone = true;
        latch.countDown();
        return true;
    }

    public boolean isCancelled() {
        return isCancel;
    }

    public boolean isDone() {
        return isDone;
    }

    /**
     * Getter method for property <tt>channel</tt>.
     *
     * @return property value of channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Setter method for property <tt>channel</tt>.
     *
     * @param channel value to be assigned to property channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return responseBuilder.build();
    }

    public T get(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return responseBuilder.build();
    }

    public static InetSocketAddress getRoute(Channel channel) {
        return (InetSocketAddress) channel.attr(RpcFuture.ROUTE_ATTRIBUTE).get();
    }

    public static boolean getForceConnect(Channel channel) {
        Object forceConnect = channel.attr(RpcFuture.FORCE_CONNECT_ATTRIBUTE).get();
        if (null == forceConnect) {
            return false;
        }
        return true;
    }


    public static void attributeResponse(Channel channel, RpcFuture responseFuture) {
        channel.attr(RpcFuture.DEFAULT_ATTRIBUTE).set(responseFuture);
        responseFuture.setChannel(channel);
    }

    public static RpcFuture getResponse(Channel channel) {
        return (RpcFuture) channel.attr(RpcFuture.DEFAULT_ATTRIBUTE).get();
    }

    public static void attributeRoute(Channel channel, InetSocketAddress route) {
        channel.attr(RpcFuture.ROUTE_ATTRIBUTE).set(route);
    }

    public static void attributeForceConnect(Channel channel, boolean forceConnect) {
        if (forceConnect) {
            channel.attr(RpcFuture.FORCE_CONNECT_ATTRIBUTE).set(true);
        }
    }

    public static void setPendingResponse(Channel channel, HttpResponse pendingResponse) {
        RpcFuture responseFuture = getResponse(channel);
        NettyHttpResponseBuilder responseBuilder = new NettyHttpResponseBuilder();
        responseBuilder.setSuccess(true);
        responseBuilder.setPendingResponse(pendingResponse);
        responseFuture.setResponseBuilder(responseBuilder);
    }

    public static boolean headerContainConnectionClose(Channel channel) {
        RpcFuture responseFuture = getResponse(channel);
        return HttpHeaders.Values.CLOSE.equalsIgnoreCase(responseFuture.getResponseBuilder()
                .getPendingResponse().headers().get(HttpHeaders.Names.CONNECTION));
    }

    public static void setPendingContent(Channel channel, HttpContent httpContent) {
        RpcFuture responseFuture = getResponse(channel);
        ResponseBuilder responseBuilder = responseFuture.getResponseBuilder();
        responseBuilder.addContent(httpContent.content().retain());
    }

    public static boolean done(Channel channel) {
        RpcFuture responseFuture = getResponse(channel);
        if (null != responseFuture) {
            return responseFuture.done();
        }

        return true;
    }

    public static boolean cancel(Channel channel, Throwable cause) {
        RpcFuture responseFuture = getResponse(channel);
        return responseFuture.cancel(cause);
    }
}
