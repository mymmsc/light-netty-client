package org.hotwheel.rpc1x.protocol.http;

import org.hotwheel.rpc1x.core.ResponseBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyHttpResponseBuilder extends ResponseBuilder<NettyHttpResponse> {

    private volatile HttpResponse pendingResponse;

    private volatile List<ByteBuf> pendingContents;

    private volatile NettyHttpResponse content;

    private AtomicBoolean isBuild = new AtomicBoolean(false);

    public NettyHttpResponse build() {
        if (isBuild.getAndSet(true)) {
            return content;
        }
        NettyHttpResponse response = new NettyHttpResponse();
        content = response;

        if (isSuccess()) {
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



    /**
     * Getter method for property <tt>pendingContents</tt>.
     *
     * @return property value of pendingContents
     */
    public List<ByteBuf> getPendingContents() {
        return pendingContents;
    }

    /**
     * Setter method for property <tt>pendingContents</tt>.
     *
     * @param pendingContents value to be assigned to property pendingContents
     */
    public void setPendingContents(List<ByteBuf> pendingContents) {
        this.pendingContents = pendingContents;
    }


}
