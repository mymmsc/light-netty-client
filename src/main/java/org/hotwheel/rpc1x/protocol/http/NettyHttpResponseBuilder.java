package org.hotwheel.rpc1x.protocol.http;

import org.hotwheel.rpc1x.core.ResponseBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponse;
import org.hotwheel.rpc1x.core.RpcResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyHttpResponseBuilder<T> extends ResponseBuilder<NettyHttpResponse> {

    //private volatile HttpResponse pendingResponse;

    //private volatile List<ByteBuf> pendingContents;

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
