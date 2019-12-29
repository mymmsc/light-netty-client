package org.hotwheel.rpc1x.codec;

import io.netty.channel.ChannelHandler;

/**
 * 编解码器
 *
 * @author wangfeng
 * @date 2019-12-29
 */
public interface Codec {
    void codec(String name, ChannelHandler handler);

    public interface Encoder extends Codec {

    }

    public interface Decoder extends Codec {

    }
}
