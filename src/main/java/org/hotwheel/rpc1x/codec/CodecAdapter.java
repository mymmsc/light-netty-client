package org.hotwheel.rpc1x.codec;

/**
 * 编解码器适配
 *
 * @author wangfeng
 * @version 0.1.0
 * @date 2019-12-29
 */
public interface CodecAdapter {
    void setEncoder(Codec.Encoder encoder);

    void setDecoder(Codec.Decoder decoder);
}

