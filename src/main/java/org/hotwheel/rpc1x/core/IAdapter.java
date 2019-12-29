package org.hotwheel.rpc1x.core;

import org.hotwheel.rpc1x.codec.CodecAdapter;

/**
 * 适配器接口
 *
 * @author wangfeng
 * @version 0.1.0
 * @date 2019-12-29
 */
public interface IAdapter {
    void setRequestCodec(CodecAdapter requestCodec);

    void setResponseCodec(CodecAdapter responseCodec);
}
