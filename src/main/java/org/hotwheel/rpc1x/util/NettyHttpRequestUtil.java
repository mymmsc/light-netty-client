package org.hotwheel.rpc1x.util;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.hotwheel.rpc1x.protocol.http.NettyHttpRequest;

import java.util.Map.Entry;

public class NettyHttpRequestUtil {

    public static HttpRequest create(NettyHttpRequest request, HttpMethod httpMethod) {
        HttpRequest httpRequest = null;
        if (HttpMethod.POST == httpMethod) {
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, request
                    .getUri().getRawPath(), request.getContent().retain());

            httpRequest.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                    request.getContent().readableBytes());
        } else {
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, request
                    .getUri().getRawPath());
        }
        for (Entry<String, Object> entry : request.getHeaders().entrySet()) {
            httpRequest.headers().set(entry.getKey(), entry.getValue());
        }
        httpRequest.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        httpRequest.headers().set(HttpHeaders.Names.HOST, request.getUri().getHost());

        return httpRequest;
    }
}
