package org.hotwheel.rpc1x.handler;

import io.netty.channel.ChannelPipeline;
import org.hotwheel.rpc1x.pool.RpcChannelPool;

public interface RpcChannelInitializer {

    void init(ChannelPipeline pipeline, RpcChannelPool rpcChannelPool) throws Exception;
    //void attributeForceConnect(Channel channel, boolean forceConnect);
}
