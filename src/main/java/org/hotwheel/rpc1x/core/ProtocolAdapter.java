package org.hotwheel.rpc1x.core;

import java.nio.charset.Charset;

/**
 * 协议适配
 *
 * @author wangfeng
 * @date 2019-12-25
 */
public class ProtocolAdapter {
    private String name;
    protected static final Charset DEFAUT_CHARSET = Charset.forName("GBK");

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
