package org.hotwheel.rpc1x.protocol.http;

import org.hotwheel.rpc1x.core.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class NettyHttpRequest extends RpcRequest {
    private URI uri;
    private Map<String, Object> headers;
    private ByteBuf content;

    public NettyHttpRequest uri(String uri) {
        this.uri = URI.create(uri);
        return this;
    }

    public NettyHttpRequest uri(URI uri) {
        if (null == uri) {
            throw new NullPointerException("uri");
        }
        this.uri = uri;
        return this;
    }

    public NettyHttpRequest header(String key, Object value) {
        if (null == this.headers) {
            this.headers = new HashMap<String, Object>();
        }
        headers.put(key, value);
        return this;
    }

    public NettyHttpRequest headers(Map<String, Object> headers) {
        if (null == headers) {
            throw new NullPointerException("headers");
        }

        if (null == this.headers) {
            this.headers = new HashMap<String, Object>();
        }

        this.headers.putAll(headers);
        return this;
    }

    public NettyHttpRequest content(ByteBuf content) {
        if (null == content) {
            throw new NullPointerException("content");
        }

        this.content = content;
        return this;
    }

    public NettyHttpRequest content(byte[] content) {
        if (null == content) {
            throw new NullPointerException("content");
        }
        this.content = Unpooled.copiedBuffer(content);
        return this;
    }

    public NettyHttpRequest content(String content, Charset charset) {
        if (null == content) {
            throw new NullPointerException("content");
        }
        charset = null == charset ? DEFAUT_CHARSET : charset;
        this.content = Unpooled.copiedBuffer(content, charset);
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public ByteBuf getContent() {
        return content;
    }
}
