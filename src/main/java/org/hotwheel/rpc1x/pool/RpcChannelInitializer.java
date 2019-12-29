package org.hotwheel.rpc1x.pool;

import io.netty.channel.ChannelPipeline;

public interface RpcChannelInitializer {

    void init(ChannelPipeline pipeline, RpcChannelPool rpcChannelPool) throws Exception;
    //void attributeForceConnect(Channel channel, boolean forceConnect);
}
