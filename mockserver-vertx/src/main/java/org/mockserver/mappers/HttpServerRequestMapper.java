package org.mockserver.mappers;

import org.apache.commons.lang3.CharEncoding;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jamesdbloom
 */
public class HttpServerRequestMapper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpRequest createHttpRequest(HttpServerRequest httpServerRequest, byte[] bodyBytes) {
        HttpRequest httpRequest = new HttpRequest();
        setMethod(httpRequest, httpServerRequest);
        setPath(httpRequest, httpServerRequest);
        setBody(httpRequest, bodyBytes);
        setHeaders(httpRequest, httpServerRequest);
        setCookies(httpRequest, httpServerRequest);
        setParameters(httpRequest, httpServerRequest);
        return httpRequest;
    }

    private void setMethod(HttpRequest httpRequest, HttpServerRequest httpServletRequest) {
        httpRequest.withMethod(httpServletRequest.method());
    }

    private void setPath(HttpRequest httpRequest, HttpServerRequest httpServerRequest) {
        httpRequest.withPath(httpServerRequest.path());
    }

    private void setBody(HttpRequest httpRequest, byte[] bodyBytes) {
        httpRequest.withBody(new String(bodyBytes, Charset.forName(CharEncoding.UTF_8)));
    }

    private void setHeaders(HttpRequest httpRequest, HttpServerRequest httpServerRequest) {
        List<Header> mappedHeaders = new ArrayList<Header>();
        MultiMap headers = httpServerRequest.headers();
        for (String headerName : headers.names()) {
            mappedHeaders.add(new Header(headerName, new ArrayList<String>(headers.getAll(headerName))));
        }
        httpRequest.withHeaders(mappedHeaders);
    }

    private void setCookies(HttpRequest httpRequest, HttpServerRequest httpServerRequest) {
        List<Cookie> mappedCookies = new ArrayList<Cookie>();
        MultiMap headers = httpServerRequest.headers();
        for (String headerName : headers.names()) {
            if (headerName.equals("Cookie") || headerName.equals("Set-Cookie")) {
                for (String cookieHeader : headers.getAll(headerName)) {
                    for (HttpCookie httpCookie : HttpCookie.parse(cookieHeader)) {
                        mappedCookies.add(new Cookie(httpCookie.getName(), httpCookie.getValue()));
                    }
                }
            }
        }
        httpRequest.withCookies(mappedCookies);
    }

    private void setParameters(HttpRequest httpRequest, HttpServerRequest httpServerRequest) {
        MultiMap parameters = httpServerRequest.params();
        List<Parameter> mappedParameters = new ArrayList<Parameter>();
        for (String parameterName : parameters.names()) {
            mappedParameters.add(new Parameter(parameterName, parameters.getAll(parameterName)));
        }
        httpRequest.withParameters(mappedParameters);
        httpRequest.withParameters(mappedParameters);
    }
}
