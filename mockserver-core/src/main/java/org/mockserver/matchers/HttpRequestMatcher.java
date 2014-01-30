package org.mockserver.matchers;

import org.mockserver.client.serialization.ObjectMapperFactory;
import org.mockserver.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jamesdbloom
 */
public class HttpRequestMatcher extends EqualsHashCodeToString implements Matcher<HttpRequest> {

    private HttpRequest httpRequest;
    private RegexStringMatcher methodMatcher = null;
    private RegexStringMatcher urlMatcher = null;
    private RegexStringMatcher pathMatcher = null;
    private MapMatcher queryStringParameterMatcher = null;
    private BodyMatcher bodyMatcher = null;
    private MapMatcher headerMatcher = null;
    private MapMatcher cookieMatcher = null;

    public HttpRequestMatcher withHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
        return this;
    }

    public HttpRequestMatcher withMethod(String method) {
        this.methodMatcher = new RegexStringMatcher(method);
        return this;
    }

    public HttpRequestMatcher withURL(String url) {
        this.urlMatcher = new RegexStringMatcher(url);
        return this;
    }

    public HttpRequestMatcher withPath(String path) {
        this.pathMatcher = new RegexStringMatcher(path);
        return this;
    }

    public HttpRequestMatcher withQueryStringParameters(Parameter... parameters) {
        this.queryStringParameterMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(parameters));
        return this;
    }

    public HttpRequestMatcher withQueryStringParameters(List<Parameter> parameters) {
        this.queryStringParameterMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(parameters));
        return this;
    }

    public HttpRequestMatcher withBody(Body body) {
        if (body != null) {
            switch (body.getType()) {
                case EXACT:
                    this.bodyMatcher = new ExactStringMatcher(((StringBody) body).getValue());
                    break;
                case REGEX:
                    this.bodyMatcher = new RegexStringMatcher(((StringBody) body).getValue());
                    break;
                case PARAMETERS:
                    this.bodyMatcher = new ParameterStringMatcher(((ParameterBody) body).getParameters());
                    break;
                case XPATH:
                    this.bodyMatcher = new XPathStringMatcher(((StringBody) body).getValue());
                    break;
            }
        }
        return this;
    }

    public HttpRequestMatcher withHeaders(Header... headers) {
        this.headerMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(headers));
        return this;
    }

    public HttpRequestMatcher withHeaders(List<Header> headers) {
        this.headerMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(headers));
        return this;
    }

    public HttpRequestMatcher withCookies(Cookie... cookies) {
        this.cookieMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(cookies));
        return this;
    }

    public HttpRequestMatcher withCookies(List<Cookie> cookies) {
        this.cookieMatcher = new MapMatcher(KeyToMultiValue.toMultiMap(cookies));
        return this;
    }

    public boolean matches(HttpRequest httpRequest) {
        if (httpRequest != null) {
            boolean methodMatches = matches(methodMatcher, httpRequest.getMethod());
            boolean urlMatches = matches(urlMatcher, httpRequest.getURL());
            boolean pathMatches = matches(pathMatcher, httpRequest.getPath());
            boolean queryStringParametersMatches = matches(queryStringParameterMatcher, (httpRequest.getQueryStringParameters() != null ? new ArrayList<KeyToMultiValue>(httpRequest.getQueryStringParameters()) : null));
            boolean bodyMatches = matches(bodyMatcher, (httpRequest.getBody() != null ? httpRequest.getBody().toString() : ""));
            boolean headersMatch = matches(headerMatcher, (httpRequest.getHeaders() != null ? new ArrayList<KeyToMultiValue>(httpRequest.getHeaders()) : null));
            boolean cookiesMatch = matches(cookieMatcher, (httpRequest.getCookies() != null ? new ArrayList<KeyToMultiValue>(httpRequest.getCookies()) : null));
            boolean result = methodMatches && urlMatches && pathMatches && queryStringParametersMatches && bodyMatches && headersMatch && cookiesMatch;
            if (!result && logger.isDebugEnabled()) {
                logger.debug("\n\nMatcher:\n\n" +
                        "[" + this + "]\n\n" +
                        "did not match request:\n\n" +
                        "[" + httpRequest + "]\n\n" +
                        "because:\n\n" +
                        "methodMatches = " + methodMatches + "\n" +
                        "urlMatches = " + urlMatches + "\n" +
                        "pathMatches = " + pathMatches + "\n" +
                        "queryStringParametersMatch = " + queryStringParametersMatches + "\n" +
                        "bodyMatches = " + bodyMatches + "\n" +
                        "headersMatch = " + headersMatch + "\n" +
                        "cookiesMatch = " + cookiesMatch);
            }
            return result;
        } else {
            return false;
        }
    }

    private <T> boolean matches(Matcher<T> matcher, T t) {
        boolean result = false;

        if (matcher == null) {
            result = true;
        } else if (matcher.matches(t)) {
            result = true;
        }

        return result;
    }

    @Override
    public String toString() {
        try {
            return ObjectMapperFactory
                    .createObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(httpRequest);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
