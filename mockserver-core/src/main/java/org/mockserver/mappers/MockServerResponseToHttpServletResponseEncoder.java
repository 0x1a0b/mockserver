package org.mockserver.mappers;

import com.google.common.base.Strings;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ServerCookieEncoder;
import org.mockserver.client.serialization.Base64Converter;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.mockserver.streams.IOStreamUtils;

import javax.servlet.http.HttpServletResponse;

import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

/**
 * @author jamesdbloom
 */
public class MockServerResponseToHttpServletResponseEncoder {

    public void mapMockServerResponseToHttpServletResponse(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        setStatusCode(httpResponse, httpServletResponse);
        setHeaders(httpResponse, httpServletResponse);
        setCookies(httpResponse, httpServletResponse);
        setBody(httpResponse, httpServletResponse);
    }

    private void setStatusCode(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getStatusCode() != null) {
            httpServletResponse.setStatus(httpResponse.getStatusCode());
        }
    }

    private void setHeaders(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getHeaders() != null) {
            for (Header header : httpResponse.getHeaders()) {
                String headerName = header.getName();
                if (!headerName.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH)
                        && !headerName.equalsIgnoreCase(HttpHeaders.Names.TRANSFER_ENCODING)
                        && !headerName.equalsIgnoreCase(HttpHeaders.Names.HOST)
                        && !headerName.equalsIgnoreCase(HttpHeaders.Names.ACCEPT_ENCODING)
                        && !headerName.equalsIgnoreCase(HttpHeaders.Names.CONNECTION)) {
                    for (String value : header.getValues()) {
                        httpServletResponse.addHeader(headerName, value);
                    }
                }
            }
        }
        addContentTypeHeader(httpResponse, httpServletResponse);
    }

    private void setCookies(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getCookies() != null) {
            for (Cookie cookie : httpResponse.getCookies()) {
                httpServletResponse.addHeader(SET_COOKIE, ServerCookieEncoder.encode(new DefaultCookie(cookie.getName(), cookie.getValue())));
            }
        }
    }

    private void setBody(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getBodyAsString() != null) {
            if (httpResponse.getBody() instanceof BinaryBody) {
                IOStreamUtils.writeToOutputStream(Base64Converter.base64StringToBytes(httpResponse.getBodyAsString()), httpServletResponse);
            } else {
                Charset bodyCharset = httpResponse.getBody().getCharset(ContentTypeMapper.determineCharsetFromResponseContentType(httpResponse));
                IOStreamUtils.writeToOutputStream(httpResponse.getBodyAsString().getBytes(bodyCharset), httpServletResponse);
            }
        }
    }

    private void addContentTypeHeader(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getBody() != null && Strings.isNullOrEmpty(httpServletResponse.getHeader(HttpHeaders.Names.CONTENT_TYPE))) {
            Charset bodyCharset = httpResponse.getBody().getCharset(null);
            String bodyContentType = httpResponse.getBody().getContentType();
            if (bodyCharset != null) {
                httpServletResponse.addHeader(HttpHeaders.Names.CONTENT_TYPE, bodyContentType + "; charset=" + bodyCharset.name().toLowerCase());
            } else if (bodyContentType != null) {
                httpServletResponse.addHeader(HttpHeaders.Names.CONTENT_TYPE, bodyContentType);
            }
        }
    }
}
