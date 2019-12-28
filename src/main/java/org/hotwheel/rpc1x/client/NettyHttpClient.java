package org.hotwheel.rpc1x.client;

import org.hotwheel.rpc1x.core.ClientConfig;
import org.hotwheel.rpc1x.core.RpcClient;
import org.hotwheel.rpc1x.core.RpcResponseFuture;
import org.hotwheel.rpc1x.protocol.http.NettyHttpRequest;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponse;
import org.hotwheel.rpc1x.util.NettyHttpRequestUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;

public class NettyHttpClient extends RpcClient {

    public NettyHttpClient(ClientConfig clientConfig) {
        super(clientConfig);
    }

    public RpcResponseFuture<NettyHttpResponse> doPost(NettyHttpRequest request) throws Exception {

        HttpRequest httpRequest = NettyHttpRequestUtil.create(request, HttpMethod.POST);
        InetSocketAddress route = new InetSocketAddress(request.getUri().getHost(), request.getUri().getPort());

        return sendRequest(route, httpRequest);
    }

    public RpcResponseFuture<NettyHttpResponse> doGet(NettyHttpRequest request) throws Exception {
        HttpRequest httpRequest = NettyHttpRequestUtil.create(request, HttpMethod.GET);
        InetSocketAddress route = new InetSocketAddress(request.getUri().getHost(), request
                .getUri().getPort());
        return sendRequest(route, httpRequest);
    }

}
