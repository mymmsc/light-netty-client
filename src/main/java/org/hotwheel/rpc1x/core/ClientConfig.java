package org.hotwheel.rpc1x.core;

import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.hotwheel.rpc1x.pool.RpcChannelInitializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置构建
 *
 * @author wangfeng
 * @date 2019-12-27
 */
public final class ClientConfig {
    @SuppressWarnings(CompileOptions.UNCHECKED)
    private Map<ChannelOption, Object> options = new HashMap<ChannelOption, Object>();

    // max idle time for a channel before close
    private int maxIdleTimeInMilliSecondes;

    // max time wait for a channel return from pool
    private int connectTimeOutInMilliSecondes;

    /**
     * value is false indicates that when there is not any channel in pool and no new
     * channel allowed to be create based on maxPerRoute, a new channel will be forced
     * to create.Otherwise, a <code>TimeoutException</code> will be thrown
     * value is false.
     */
    private boolean forbidForceConnect = false;

    private RpcChannelInitializer rpcChannelInitializer;

    // max number of channels allow to be created per route
    private Map<String, Integer> maxPerRoute;

    private EventLoopGroup customGroup;
    private ProtocolAdapter adapter;

    public ClientConfig() {
    }

    public ClientConfig maxPerRoute(Map<String, Integer> maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
        return this;
    }

    public ClientConfig connectTimeOutInMilliSecondes(int connectTimeOutInMilliSecondes) {
        this.connectTimeOutInMilliSecondes = connectTimeOutInMilliSecondes;
        return this;
    }

    @SuppressWarnings(CompileOptions.UNCHECKED)
    public ClientConfig option(ChannelOption key, Object value) {
        options.put(key, value);
        return this;
    }

    public ClientConfig maxIdleTimeInMilliSecondes(int maxIdleTimeInMilliSecondes) {
        this.maxIdleTimeInMilliSecondes = maxIdleTimeInMilliSecondes;
        return this;
    }

    public ClientConfig customGroup(EventLoopGroup customGroup) {
        this.customGroup = customGroup;
        return this;
    }

    public ClientConfig forbidForceConnect(boolean forbidForceConnect) {
        this.forbidForceConnect = forbidForceConnect;
        return this;
    }

    @SuppressWarnings(CompileOptions.UNCHECKED)
    public Map<ChannelOption, Object> getOptions() {
        return options;
    }

    public int getMaxIdleTimeInMilliSecondes() {
        return maxIdleTimeInMilliSecondes;
    }

    public ClientConfig additionalChannelInitializer(RpcChannelInitializer rpcChannelInitializer) {
        this.rpcChannelInitializer = rpcChannelInitializer;
        return this;
    }

    public RpcChannelInitializer getRpcChannelInitializer() {
        return rpcChannelInitializer;
    }

    public Map<String, Integer> getMaxPerRoute() {
        return maxPerRoute;
    }

    public int getConnectTimeOutInMilliSecondes() {
        return connectTimeOutInMilliSecondes;
    }

    public EventLoopGroup getGroup() {
        return this.customGroup;
    }

    public boolean getForbidForceConnect() {
        return this.forbidForceConnect;
    }

    public void setAdapter(ProtocolAdapter adapter) {
        this.adapter = adapter;
    }

    public ProtocolAdapter getAdapter() {
        return adapter;
    }
}
