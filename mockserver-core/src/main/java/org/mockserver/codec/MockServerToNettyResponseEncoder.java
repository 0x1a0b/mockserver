package org.mockserver.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mockserver.model.ConnectionOptions.isFalseOrNull;

/**
 * @author jamesdbloom
 */
public class MockServerToNettyResponseEncoder extends MessageToMessageEncoder<HttpResponse> {

    private final MockServerLogger mockServerLogger;
    private final BodyDecoderEncoder bodyDecoderEncoder;

    public MockServerToNettyResponseEncoder(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        this.bodyDecoderEncoder = new BodyDecoderEncoder(mockServerLogger);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpResponse response, List<Object> out) {
        out.add(encode(response));
    }

    public DefaultFullHttpResponse encode(HttpResponse httpResponse) {
        try {
            ByteBuf body = getBody(httpResponse);
            DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                getStatus(httpResponse),
                body
            );
            setHeaders(httpResponse, defaultFullHttpResponse, body);
            setCookies(httpResponse, defaultFullHttpResponse);
            return defaultFullHttpResponse;
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(LogEntry.LogMessageType.EXCEPTION)
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception encoding response{}")
                    .setArguments(httpResponse)
                    .setThrowable(throwable)
            );
            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, getStatus(httpResponse));
        }
    }

    private HttpResponseStatus getStatus(HttpResponse httpResponse) {
        int statusCode = httpResponse.getStatusCode() != null ? httpResponse.getStatusCode() : 200;
        if (!StringUtils.isEmpty(httpResponse.getReasonPhrase())) {
            return new HttpResponseStatus(statusCode, httpResponse.getReasonPhrase());
        } else {
            return HttpResponseStatus.valueOf(statusCode);
        }
    }

    private ByteBuf getBody(HttpResponse httpResponse) {
        return bodyDecoderEncoder.bodyToByteBuf(httpResponse.getBody(), httpResponse.getFirstHeader(CONTENT_TYPE.toString()));
    }

    private void setHeaders(HttpResponse httpResponse, DefaultFullHttpResponse response, ByteBuf body) {
        if (httpResponse.getHeaderMultimap() != null) {
            httpResponse.getHeaderMultimap().forEach((key, value) -> response.headers().add(key.getValue(), value.getValue()));
        }

        // Content-Type
        if (isBlank(httpResponse.getFirstHeader(CONTENT_TYPE.toString()))) {
            if (httpResponse.getBody() != null
                && httpResponse.getBody().getContentType() != null) {
                response.headers().set(CONTENT_TYPE, httpResponse.getBody().getContentType());
            }
        }

        // Content-Length
        if (isBlank(httpResponse.getFirstHeader(CONTENT_LENGTH.toString()))) {
            ConnectionOptions connectionOptions = httpResponse.getConnectionOptions();
            boolean overrideContentLength = connectionOptions != null && connectionOptions.getContentLengthHeaderOverride() != null;
            boolean addContentLength = connectionOptions == null || isFalseOrNull(connectionOptions.getSuppressContentLengthHeader());
            if (overrideContentLength) {
                response.headers().set(CONTENT_LENGTH, connectionOptions.getContentLengthHeaderOverride());
            } else if (addContentLength) {
                response.headers().set(CONTENT_LENGTH, body.readableBytes());
            }
        }
    }

    private void setCookies(HttpResponse httpResponse, DefaultFullHttpResponse response) {
        if (httpResponse.getCookieMap() != null) {
            for (Map.Entry<NottableString, NottableString> cookie : httpResponse.getCookieMap().entrySet()) {
                if (httpResponse.cookieHeadeDoesNotAlreadyExists(cookie.getKey().getValue(), cookie.getValue().getValue())) {
                    response.headers().add(SET_COOKIE, ServerCookieEncoder.LAX.encode(new DefaultCookie(cookie.getKey().getValue(), cookie.getValue().getValue())));
                }
            }
        }
    }
}
