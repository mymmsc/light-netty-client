package org.hotwheel.rpc1x.core;

import io.netty.channel.Channel;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponseBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wangfeng
 * @date 2019-12-27
 */
public class RpcResponseFuture<T extends RpcResponse>/* implements Future<T>*/ {
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

        responseBuilder = new ResponseBuilder<>();
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
}
