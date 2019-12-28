package org.hotwheel.rpc1x.handler;

import io.netty.channel.Channel;

public interface AdditionalChannelInitializer {

    void initChannel(Channel ch) throws Exception;
}
