package org.hotwheel.rpc1x.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.hotwheel.rpc1x.core.ClientConfig;
import org.hotwheel.rpc1x.core.RpcClient;
import org.hotwheel.rpc1x.core.RpcFuture;
import org.hotwheel.rpc1x.protocol.http.NettyHttpRequest;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponse;
import org.hotwheel.rpc1x.util.NettyHttpRequestUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyHttpClient extends RpcClient {
    private static final Logger LOG = Logger.getLogger(RpcClient.class.getSimpleName());

    public NettyHttpClient(ClientConfig clientConfig) {
        super(clientConfig);
    }

    public RpcFuture<NettyHttpResponse> doPost(NettyHttpRequest request) throws Exception {

        HttpRequest httpRequest = NettyHttpRequestUtil.create(request, HttpMethod.POST);
        InetSocketAddress route = new InetSocketAddress(request.getUri().getHost(), request.getUri().getPort());

        return sendRequest(route, httpRequest);
    }

    public RpcFuture<NettyHttpResponse> doGet(NettyHttpRequest request) throws Exception {
        HttpRequest httpRequest = NettyHttpRequestUtil.create(request, HttpMethod.GET);
        InetSocketAddress route = new InetSocketAddress(request.getUri().getHost(), request.getUri().getPort());
        return sendRequest(route, httpRequest);
    }

    /**
     * send http request to server specified by the route. The channel used to
     * send the request is obtained according to the follow rules
     * <p>
     * 1. poll the first valid channel from pool without waiting. If no valid
     * channel exists, then go to step 2.
     * 2. create a new channel and return. If failed to create a new channel, then go to step 3.
     * Note: the new channel created in this step will be returned to the pool
     * 3. poll the first valid channel from pool within specified waiting time. If no valid
     * channel exists and the value of forbidForceConnect is false, then throw <code>TimeoutException</code>.
     * Otherwise,go to step 4.
     * 4. create a new channel and return. Note: the new channel created in this step will not be returned to the pool.
     * </p>
     *
     * @param route   target server
     * @param request {@link HttpRequest}
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws IOException
     * @throws Exception
     */
    public RpcFuture<NettyHttpResponse> sendRequest(InetSocketAddress route, final HttpRequest request)
            throws InterruptedException,
            IOException {
        final RpcFuture<NettyHttpResponse> responseFuture = new RpcFuture<NettyHttpResponse>();
        if (sendRequestUsePooledChannel(route, request, responseFuture, false)) {
            return responseFuture;
        }

        if (sendRequestUseNewChannel(route, request, responseFuture, pool.isForbidForceConnect())) {
            return responseFuture;
        }

        if (sendRequestUsePooledChannel(route, request, responseFuture, true)) {
            return responseFuture;
        }

        throw new IOException("send request failed");
    }

    private boolean sendRequestUsePooledChannel(InetSocketAddress route, final HttpRequest request,
                                                RpcFuture<NettyHttpResponse> responseFuture,
                                                boolean isWaiting) throws InterruptedException {
        LinkedBlockingQueue<Channel> poolChannels = pool.getPoolChannels(pool.getKey(route));
        Channel channel = poolChannels.poll();

        while (null != channel && !channel.isActive()) {
            channel = poolChannels.poll();
        }

        if (null == channel || !channel.isActive()) {
            if (!isWaiting) {
                return false;
            }
            channel = poolChannels.poll(pool.getConnectTimeOutInMilliSecondes(), TimeUnit.MILLISECONDS);
            if (null == channel || !channel.isActive()) {
                LOG.log(Level.WARNING, "obtain channel from pool timeout");
                return false;
            }
        }

        LOG.log(Level.INFO, channel + " reuse");
        //NettyHttpResponseFutureUtil.attributeResponse(channel, responseFuture);
        responseFuture.setChannel(channel);
        channel.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        return true;
    }

    private boolean sendRequestUseNewChannel(final InetSocketAddress route,
                                             final HttpRequest request,
                                             final RpcFuture<NettyHttpResponse> responseFuture,
                                             boolean forceConnect) {
        ChannelFuture future = pool.createChannelFuture(route, forceConnect);
        if (null != future) {
            RpcFuture.attributeResponse(future.channel(), responseFuture);
            RpcFuture.attributeRoute(future.channel(), route);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {

                        future.channel().closeFuture().addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                LOG.log(Level.SEVERE, future.channel() + " closed, exception: " + future.cause());
                                pool.removeChannel(future.channel(), future.cause());
                            }
                        });
                        future.channel().writeAndFlush(request).addListener(CLOSE_ON_FAILURE);
                    } else {
                        LOG.log(Level.SEVERE, future.channel() + " connect failed, exception: " + future.cause());
                        RpcFuture.cancel(future.channel(), future.cause());
                        if (!RpcFuture.getForceConnect(future.channel())) {
                            pool.releaseCreatePerRoute(future.channel());
                        }
                    }
                }
            });
            return true;
        }
        return false;
    }
}
