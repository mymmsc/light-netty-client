/*
 * Copyright 2014 The LightNettyClient Project
 *
 * The Light netty client Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.hotwheel.rpc1x.client;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import org.hotwheel.rpc1x.core.ClientConfig;
import org.hotwheel.rpc1x.core.ResponseBuilder;
import org.hotwheel.rpc1x.core.RpcFuture;
import org.hotwheel.rpc1x.pool.RpcChannelInitializer;
import org.hotwheel.rpc1x.protocol.http.HttpChannelPoolHandler;
import org.hotwheel.rpc1x.pool.RpcChannelPool;
import org.hotwheel.rpc1x.protocol.http.HttpAdapter;
import org.hotwheel.rpc1x.protocol.http.NettyHttpRequest;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;


import org.junit.Before;
import org.junit.Test;

public class NettyHttpClientTest {
    private ClientConfig config;

    @Before
    public void dobefor() {
        Map<String, Integer> maxPerRoute = new HashMap<String, Integer>();
        maxPerRoute.put("www.baidu.com:80", 100);

        HttpAdapter httpAdapter = new HttpAdapter();
        config = new ClientConfig();
        config.setAdapter(httpAdapter);
        config.maxIdleTimeInMilliSecondes(200 * 1000);
        config.maxPerRoute(maxPerRoute);
        config.connectTimeOutInMilliSecondes(30 * 1000);
    }

    @Test
    public void testGet() throws Exception {
        final String url = "http://www.baidu.com:80";
        ResponseBuilder<NettyHttpResponse> responseBuilder = new ResponseBuilder<NettyHttpResponse>() {
            @Override
            public NettyHttpResponse build() {
                return new NettyHttpResponse();
            }
        };

        RpcChannelInitializer initializer = new RpcChannelInitializer() {
            @Override
            public void init(ChannelPipeline pipeline, RpcChannelPool rpcChannelPool) throws Exception {
                pipeline.addLast("codec", new HttpClientCodec());
                pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
                pipeline.addLast("handler", new HttpChannelPoolHandler(rpcChannelPool));
            }
        };
        config.additionalChannelInitializer(initializer);
        final NettyHttpClient client = new NettyHttpClient(config);

        final NettyHttpRequest request = new NettyHttpRequest();
        request.header(HttpHeaders.Names.CONTENT_TYPE, "text/json; charset=GBK").uri(url);

        RpcFuture<NettyHttpResponse> responseFuture = client.doGet(request);

        NettyHttpResponse response = responseFuture.get();
        client.close();

        print(response);
    }

    private void print(NettyHttpResponse response) {
        System.out.println("STATUS: " + response.getStatus());
        System.out.println("VERSION: " + response.getVersion());
        System.out.println();

        if (!response.getHeaders().isEmpty()) {
            for (String name : response.getHeaders().names()) {
                for (String value : response.getHeaders().getAll(name)) {
                    System.out.println("HEADER: " + name + " = " + value);
                }
            }
        }
        System.out.println("CHUNKED CONTENT :");
        for (ByteBuf buf : response.getContents()) {
            System.out.print(buf.toString(CharsetUtil.UTF_8));
        }
    }
}
