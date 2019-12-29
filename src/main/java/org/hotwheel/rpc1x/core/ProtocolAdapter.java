package org.hotwheel.rpc1x.core;

import org.hotwheel.rpc1x.codec.CodecAdapter;

import java.nio.charset.Charset;

/**
 * 协议适配
 *
 * @author wangfeng
 * @date 2019-12-25
 */
public class ProtocolAdapter implements IAdapter {
    private String name;
    protected static final Charset DEFAUT_CHARSET = Charset.forName("GBK");
    private Charset charset;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public void setRequestCodec(CodecAdapter requestCodec) {

    }

    @Override
    public void setResponseCodec(CodecAdapter responseCodec) {

    }
}
