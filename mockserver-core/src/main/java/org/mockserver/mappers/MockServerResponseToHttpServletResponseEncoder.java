package org.mockserver.mappers;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.mockserver.codec.BodyDecoderEncoder;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.*;
import org.mockserver.serialization.Base64Converter;
import org.mockserver.streams.IOStreamUtils;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @author jamesdbloom
 */
public class MockServerResponseToHttpServletResponseEncoder {

    private final Base64Converter base64Converter = new Base64Converter();
    private final IOStreamUtils ioStreamUtils;
    private final BodyDecoderEncoder bodyDecoderEncoder;

    public MockServerResponseToHttpServletResponseEncoder(MockServerLogger mockServerLogger) {
        ioStreamUtils = new IOStreamUtils(mockServerLogger);
        bodyDecoderEncoder = new BodyDecoderEncoder(mockServerLogger);
    }

    public void mapMockServerResponseToHttpServletResponse(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        setStatusCode(httpResponse, httpServletResponse);
        setHeaders(httpResponse, httpServletResponse);
        setCookies(httpResponse, httpServletResponse);
        setBody(httpResponse, httpServletResponse);
    }

    @SuppressWarnings("deprecation")
    private void setStatusCode(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        int statusCode = httpResponse.getStatusCode() != null ? httpResponse.getStatusCode() : 200;
        if (httpResponse.getReasonPhrase() != null) {
            httpServletResponse.setStatus(statusCode, httpResponse.getReasonPhrase());
        } else {
            httpServletResponse.setStatus(statusCode);
        }
    }

    private void setHeaders(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getHeaderList() != null) {
            for (Header header : httpResponse.getHeaderList()) {
                String headerName = header.getName().getValue();
                if (!headerName.equalsIgnoreCase(CONTENT_LENGTH.toString())
                    && !headerName.equalsIgnoreCase(TRANSFER_ENCODING.toString())
                    && !headerName.equalsIgnoreCase(HOST.toString())
                    && !headerName.equalsIgnoreCase(ACCEPT_ENCODING.toString())
                    && !headerName.equalsIgnoreCase(CONNECTION.toString())) {
                    for (NottableString value : header.getValues()) {
                        httpServletResponse.addHeader(headerName, value.getValue());
                    }
                }
            }
        }
        addContentTypeHeader(httpResponse, httpServletResponse);
    }

    private void setCookies(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getCookieList() != null) {
            for (Cookie cookie : httpResponse.getCookieList()) {
                if (httpResponse.cookieHeadeDoesNotAlreadyExists(cookie)) {
                    httpServletResponse.addHeader(SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(new DefaultCookie(cookie.getName().getValue(), cookie.getValue().getValue())));
                }
            }
        }
    }

    private void setBody(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        bodyDecoderEncoder.bodyToServletResponse(httpServletResponse, httpResponse.getBody(), httpResponse.getFirstHeader(CONTENT_TYPE.toString()));
    }

    private void addContentTypeHeader(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpServletResponse.getContentType() == null
            && httpResponse.getBody() != null
            && httpResponse.getBody().getContentType() != null) {
            httpServletResponse.addHeader(CONTENT_TYPE.toString(), httpResponse.getBody().getContentType());
        }
    }
}
