package org.hotwheel.rpc1x.extension;

import org.hotwheel.rpc1x.core.ProtocolAdapter;
import org.hotwheel.rpc1x.protocol.http.HttpAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议适配器加载器
 *
 * @author wangfeng
 * @version 0.1.0
 * @date 2019-12-29
 */
public class AdapterLoader {
    private static Map<String, ProtocolAdapter> mapAdapters = new ConcurrentHashMap<>();

    static {
        HttpAdapter httpAdapter = new HttpAdapter();
        mapAdapters.put(httpAdapter.getName(), httpAdapter);
    }

    public static ProtocolAdapter getAdapter(final String name) {
        ProtocolAdapter adapter = mapAdapters.get(name);
        return adapter;
    }
}
