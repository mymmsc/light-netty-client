package org.hotwheel.rpc1x.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponse;
import org.hotwheel.rpc1x.protocol.http.NettyHttpRequest;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponse;
import org.hotwheel.rpc1x.protocol.http.NettyHttpResponseBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangfeng
 * @date 2019-12-28
 */
public class ResponseBuilder<T extends RpcResponse> implements RpcBuilder<T>{
    private volatile Throwable cause;
    private volatile boolean success = false;
    private volatile List<ByteBuf> pendingContents;
    private volatile HttpResponse pendingResponse;

    @Override
    public T build() {
        return null;
    }

/*
    public ResponseBuilder<NettyHttpResponse> getResponseBuilder() {
        return responseBuilder;
    }

    public void setResponseBuilder(NettyHttpResponseBuilder responseBuilder) {
        this.responseBuilder = responseBuilder;
    }
*/
    /**
     * Getter method for property <tt>cause</tt>.
     *
     * @return property value of cause
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Setter method for property <tt>cause</tt>.
     *
     * @param cause value to be assigned to property cause
     */
    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    /**
     * Getter method for property <tt>success</tt>.
     *
     * @return property value of success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Setter method for property <tt>success</tt>.
     *
     * @param success value to be assigned to property success
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Getter method for property <tt>pendingResponse</tt>.
     *
     * @return property value of pendingResponse
     */
    public HttpResponse getPendingResponse() {
        return pendingResponse;
    }

    /**
     * Setter method for property <tt>pendingResponse</tt>.
     *
     * @param pendingResponse value to be assigned to property pendingResponse
     */
    public void setPendingResponse(HttpResponse pendingResponse) {
        this.pendingResponse = pendingResponse;
    }

    public void addContent(ByteBuf byteBuf) {
        if (null == pendingContents) {
            pendingContents = new ArrayList<ByteBuf>();
        }
        pendingContents.add(byteBuf);
    }

    /**
     * @return the contents
     */
    public List<ByteBuf> getContents() {
        return pendingContents;
    }
}
