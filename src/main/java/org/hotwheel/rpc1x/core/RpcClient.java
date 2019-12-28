package org.hotwheel.rpc1x.core;

import org.hotwheel.rpc1x.pool.NettyChannelPool;

import java.util.logging.Logger;

/**
 * RPC Client
 *
 * @author wangfeng
 * @date 2019-12-28
 */
public class RpcClient {
    private static final Logger LOG = Logger.getLogger(RpcClient.class.getSimpleName());
    private ClientConfig clientConfig;
    protected NettyChannelPool pool;

    public RpcClient(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.pool = new NettyChannelPool(clientConfig.getMaxPerRoute(), clientConfig
                .getConnectTimeOutInMilliSecondes(), clientConfig.getMaxIdleTimeInMilliSecondes(),
                clientConfig.getForbidForceConnect(), clientConfig.getAdditionalChannelInitializer(),
                clientConfig.getOptions(), clientConfig.getGroup());
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void close() throws InterruptedException {
        pool.close();
    }
}
