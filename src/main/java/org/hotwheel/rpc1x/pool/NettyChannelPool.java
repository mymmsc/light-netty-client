package org.hotwheel.rpc1x.pool;

import org.hotwheel.rpc1x.core.RpcResponseFuture;
import org.hotwheel.rpc1x.handler.AdditionalChannelInitializer;
import org.hotwheel.rpc1x.handler.HttpChannelPoolHandler;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponse;
import org.hotwheel.rpc1x.core.CompileOptions;
import org.hotwheel.rpc1x.util.NettyHttpResponseFutureUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyChannelPool {
    private static final Logger logger = Logger.getLogger(NettyChannelPool.class.getName());

    // channel pools per route
    private ConcurrentMap<String, LinkedBlockingQueue<Channel>> routeToPoolChannels;

    // max number of channels allow to be created per route
    private ConcurrentMap<String, Semaphore> maxPerRoute;

    // max time wait for a channel return from pool
    private int connectTimeOutInMilliSecondes;

    // max idle time for a channel before close
    private int maxIdleTimeInMilliSecondes;

    private AdditionalChannelInitializer additionalChannelInitializer;

    /**
     * value is false indicates that when there is not any channel in pool and no new
     * channel allowed to be create based on maxPerRoute, a new channel will be forced
     * to create.Otherwise, a <code>TimeoutException</code> will be thrown
     */
    public boolean forbidForceConnect;

    // default max number of channels allow to be created per route
    private final static int DEFAULT_MAX_PER_ROUTE = 200;

    private EventLoopGroup group;

    private final Bootstrap clientBootstrap;

    private static final String COLON = ":";

    /**
     * Create a new instance of ChannelPool
     *
     * @param maxPerRoute                   max number of channels per route allowed in pool
     * @param connectTimeOutInMilliSecondes max time wait for a channel return from pool
     * @param maxIdleTimeInMilliSecondes    max idle time for a channel before close
     * @param forbidForceConnect            value is false indicates that when there is not any channel in pool and no new
     *                                      channel allowed to be create based on maxPerRoute, a new channel will be forced
     *                                      to create.Otherwise, a <code>TimeoutException</code> will be thrown. The default
     *                                      value is false.
     * @param additionalChannelInitializer  user-defined initializer
     * @param options                       user-defined options
     * @param customGroup                   user defined {@link EventLoopGroup}
     */
    @SuppressWarnings(CompileOptions.UNCHECKED)
    public NettyChannelPool(Map<String, Integer> maxPerRoute, int connectTimeOutInMilliSecondes,
                            int maxIdleTimeInMilliSecondes, boolean forbidForceConnect,
                            AdditionalChannelInitializer additionalChannelInitializer,
                            Map<ChannelOption, Object> options, EventLoopGroup customGroup) {

        this.additionalChannelInitializer = additionalChannelInitializer;
        this.maxIdleTimeInMilliSecondes = maxIdleTimeInMilliSecondes;
        this.connectTimeOutInMilliSecondes = connectTimeOutInMilliSecondes;
        this.maxPerRoute = new ConcurrentHashMap<String, Semaphore>();
        this.routeToPoolChannels = new ConcurrentHashMap<String, LinkedBlockingQueue<Channel>>();
        this.group = null == customGroup ? new NioEventLoopGroup() : customGroup;
        this.forbidForceConnect = forbidForceConnect;

        this.clientBootstrap = new Bootstrap();
        clientBootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast("log", new LoggingHandler(LogLevel.INFO));

                        ch.pipeline().addLast(HttpClientCodec.class.getSimpleName(), new HttpClientCodec());
                        if (null != additionalChannelInitializer) {
                            additionalChannelInitializer.initChannel(ch);
                        }

                        ch.pipeline().addLast(HttpObjectAggregator.class.getSimpleName(),
                                new HttpObjectAggregator(1048576));

                        ch.pipeline().addLast(
                                IdleStateHandler.class.getSimpleName(),
                                new IdleStateHandler(0, 0, maxIdleTimeInMilliSecondes,
                                        TimeUnit.MILLISECONDS));

                        ch.pipeline().addLast(HttpChannelPoolHandler.class.getSimpleName(),
                                new HttpChannelPoolHandler(NettyChannelPool.this));
                    }

                });
        if (null != options) {
            for (Entry<ChannelOption, Object> entry : options.entrySet()) {
                clientBootstrap.option(entry.getKey(), entry.getValue());
            }
        }

        if (null != maxPerRoute) {
            for (Entry<String, Integer> entry : maxPerRoute.entrySet()) {
                this.maxPerRoute.put(entry.getKey(), new Semaphore(entry.getValue()));
            }
        }

    }

    /**
     * return the specified channel to pool
     *
     * @param channel
     */
    public void returnChannel(Channel channel) {
        if (NettyHttpResponseFutureUtil.getForceConnect(channel)) {
            return;
        }
        InetSocketAddress route = (InetSocketAddress) channel.remoteAddress();
        String key = getKey(route);
        LinkedBlockingQueue<Channel> poolChannels = routeToPoolChannels.get(key);

        if (null != channel && channel.isActive()) {
            if (poolChannels.offer(channel)) {
                logger.log(Level.INFO, channel + "returned");
            }
        }
    }

    /**
     * close all channels in the pool and shut down the eventLoopGroup
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {
        ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        for (LinkedBlockingQueue<Channel> queue : routeToPoolChannels.values()) {
            for (Channel channel : queue) {
                removeChannel(channel, null);
                channelGroup.add(channel);
            }
        }
        channelGroup.close().sync();
        group.shutdownGracefully();
    }

    /**
     * remove the specified channel from the pool,cancel the responseFuture
     * and release semaphore for the route
     *
     * @param channel
     */
    public void removeChannel(Channel channel, Throwable cause) {

        InetSocketAddress route = (InetSocketAddress) channel.remoteAddress();
        String key = getKey(route);

        NettyHttpResponseFutureUtil.cancel(channel, cause);

        if (!NettyHttpResponseFutureUtil.getForceConnect(channel)) {
            LinkedBlockingQueue<Channel> poolChannels = routeToPoolChannels.get(key);
            if (poolChannels.remove(channel)) {
                logger.log(Level.INFO, channel + " removed");
            }
            getAllowCreatePerRoute(key).release();
        }
    }

    public void releaseCreatePerRoute(Channel channel) {
        InetSocketAddress route = NettyHttpResponseFutureUtil.getRoute(channel);
        getAllowCreatePerRoute(getKey(route)).release();
    }

    private Semaphore getAllowCreatePerRoute(String key) {
        Semaphore allowCreate = maxPerRoute.get(key);
        if (null == allowCreate) {
            Semaphore newAllowCreate = new Semaphore(DEFAULT_MAX_PER_ROUTE);
            allowCreate = maxPerRoute.putIfAbsent(key, newAllowCreate);
            if (null == allowCreate) {
                allowCreate = newAllowCreate;
            }
        }

        return allowCreate;
    }

    public LinkedBlockingQueue<Channel> getPoolChannels(String route) {
        LinkedBlockingQueue<Channel> oldPoolChannels = routeToPoolChannels.get(route);
        if (null == oldPoolChannels) {
            LinkedBlockingQueue<Channel> newPoolChannels = new LinkedBlockingQueue<Channel>();
            oldPoolChannels = routeToPoolChannels.putIfAbsent(route, newPoolChannels);
            if (null == oldPoolChannels) {
                oldPoolChannels = newPoolChannels;
            }
        }
        return oldPoolChannels;
    }

    public String getKey(InetSocketAddress route) {
        return route.getHostName() + COLON + route.getPort();
    }

    public ChannelFuture createChannelFuture(InetSocketAddress route, boolean forceConnect) {
        String key = getKey(route);

        Semaphore allowCreate = getAllowCreatePerRoute(key);
        if (allowCreate.tryAcquire()) {
            try {
                ChannelFuture connectFuture = clientBootstrap.connect(route.getHostName(), route.getPort());
                return connectFuture;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "connect failed", e);
                allowCreate.release();
            }
        }
        if (forceConnect) {
            ChannelFuture connectFuture = clientBootstrap.connect(route.getHostName(), route.getPort());
            if (null != connectFuture) {
                NettyHttpResponseFutureUtil.attributeForceConnect(connectFuture.channel(), forceConnect);
            }
            return connectFuture;
        }
        return null;
    }

    public int getConnectTimeOutInMilliSecondes() {
        return connectTimeOutInMilliSecondes;
    }

    public void setConnectTimeOutInMilliSecondes(int connectTimeOutInMilliSecondes) {
        this.connectTimeOutInMilliSecondes = connectTimeOutInMilliSecondes;
    }

    public boolean isForbidForceConnect() {
        return forbidForceConnect;
    }

    public void setForbidForceConnect(boolean forbidForceConnect) {
        this.forbidForceConnect = forbidForceConnect;
    }
}
