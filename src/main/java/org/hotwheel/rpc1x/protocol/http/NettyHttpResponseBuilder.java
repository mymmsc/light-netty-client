package org.hotwheel.rpc1x.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponse;
import org.hotwheel.rpc1x.core.ResponseBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyHttpResponseBuilder extends ResponseBuilder<NettyHttpResponse> {
    private volatile NettyHttpResponse content;
    private AtomicBoolean isBuild = new AtomicBoolean(false);

    @Override
    public NettyHttpResponse build() {
        if (isBuild.getAndSet(true)) {
            return content;
        }
        NettyHttpResponse response = new NettyHttpResponse();
        content = response;
        if (isSuccess()) {
            HttpResponse pendingResponse = getPendingResponse();
            List<ByteBuf> pendingContents = getContents();
            response.setSuccess(true);
            response.setVersion(pendingResponse.getProtocolVersion());
            response.setStatus(pendingResponse.getStatus());
            response.setHeaders(pendingResponse.headers());
            response.setContents(pendingContents);
        } else {
            response.setCause(getCause());
        }
        return content;
    }

}
