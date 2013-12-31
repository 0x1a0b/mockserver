package org.mockserver.proxy.filters;

import org.mockserver.collections.CircularMultiMap;
import org.mockserver.matchers.HttpRequestMatcher;
import org.mockserver.matchers.MatcherBuilder;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

/**
 * @author jamesdbloom
 */
public class Filters {

    private final CircularMultiMap<HttpRequestMatcher, ProxyResponseFilter> responseFilters = new CircularMultiMap<>(100, 100);
    private final CircularMultiMap<HttpRequestMatcher, ProxyRequestFilter> requestFilters = new CircularMultiMap<>(100, 100);

    public Filters withFilter(HttpRequest httpRequest, ProxyRequestFilter filter) {
        requestFilters.put(MatcherBuilder.transformsToMatcher(httpRequest), filter);
        return this;
    }

    public Filters withFilter(HttpRequest httpRequest, ProxyResponseFilter filter) {
        responseFilters.put(MatcherBuilder.transformsToMatcher(httpRequest), filter);
        return this;
    }

    public HttpRequest applyFilters(HttpRequest httpRequest) {
        for (HttpRequestMatcher httpRequestMatcher : requestFilters.keySet()) {
            if (httpRequestMatcher.matches(httpRequest)) {
                for (ProxyRequestFilter proxyRequestFilter : requestFilters.getAll(httpRequestMatcher)) {
                    if (httpRequest != null) {
                        httpRequest = proxyRequestFilter.onRequest(httpRequest);
                    }
                }
            }
        }
        return httpRequest;
    }

    public HttpResponse applyFilters(HttpRequest httpRequest, HttpResponse httpResponse) {
        for (HttpRequestMatcher httpRequestMatcher : responseFilters.keySet()) {
            if (httpRequestMatcher.matches(httpRequest)) {
                for (ProxyResponseFilter proxyFilter : responseFilters.getAll(httpRequestMatcher)) {
                    httpResponse = proxyFilter.onResponse(httpRequest, httpResponse);
                    if (httpResponse == null) throw new IllegalStateException(proxyFilter.getClass().getName() + " returned a null HttpResponse, Filters are not allowed to return a null HttpResponse object, a Filter can only return null for an HttpRequest which will prevent the request being sent.");
                }
            }
        }
        return httpResponse;
    }
}
