package org.mockserver.proxy.filters;

import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.collections.CircularMultiMap;
import org.mockserver.matchers.HttpRequestMatcher;
import org.mockserver.matchers.MatcherBuilder;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.Verification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.mockserver.model.HttpResponse.notFoundResponse;

/**
 * @author jamesdbloom
 */
public class LogFilter implements ProxyResponseFilter {

    private final CircularMultiMap<HttpRequest, HttpResponse> requestResponseLog = new CircularMultiMap<HttpRequest, HttpResponse>(100, 100);
    private final MatcherBuilder matcherBuilder = new MatcherBuilder();
    private Logger requestLogger = LoggerFactory.getLogger("REQUEST");

    public synchronized HttpResponse onResponse(HttpRequest httpRequest, HttpResponse httpResponse) {
        if (httpRequest != null && httpResponse != null) {
            requestResponseLog.put(httpRequest, httpResponse);
        } else if (httpRequest != null) {
            requestResponseLog.put(httpRequest, notFoundResponse());
        }
        return httpResponse;
    }

    public synchronized List<HttpResponse> httpResponses(HttpRequest httpRequest) {
        List<HttpResponse> httpResponses = new ArrayList<HttpResponse>();
        HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(httpRequest);
        for (HttpRequest loggedHttpRequest : requestResponseLog.keySet()) {
            if (httpRequestMatcher.matches(loggedHttpRequest)) {
                httpResponses.addAll(requestResponseLog.getAll(loggedHttpRequest));
            }
        }
        return httpResponses;
    }

    public synchronized List<HttpRequest> httpRequests(HttpRequest httpRequest) {
        List<HttpRequest> httpRequests = new ArrayList<HttpRequest>();
        HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(httpRequest);
        for (HttpRequest loggedHttpRequest : requestResponseLog.keySet()) {
            if (httpRequestMatcher.matches(loggedHttpRequest)) {
                httpRequests.add(loggedHttpRequest);
            }
        }
        return httpRequests;
    }

    public synchronized void reset() {
        synchronized (this.requestResponseLog) {
            requestResponseLog.clear();
        }
    }

    public synchronized void clear(HttpRequest httpRequest) {
        if (httpRequest != null) {
            HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(httpRequest);
            for (HttpRequest key : new LinkedHashSet<HttpRequest>(requestResponseLog.keySet())) {
                if (httpRequestMatcher.matches(key)) {
                    synchronized (this.requestResponseLog) {
                        requestResponseLog.removeAll(key);
                    }
                }
            }
        } else {
            reset();
        }
    }

    public synchronized void dumpToLog(HttpRequest httpRequest, boolean asJava) {
        ExpectationSerializer expectationSerializer = new ExpectationSerializer();
        if (httpRequest != null) {
            HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(httpRequest);
            for (Map.Entry<HttpRequest, HttpResponse> entry : requestResponseLog.entrySet()) {
                if (httpRequestMatcher.matches(entry.getKey())) {
                    if (asJava) {
                        requestLogger.warn(expectationSerializer.serializeAsJava(new Expectation(entry.getKey(), Times.once()).thenRespond(entry.getValue())));
                    } else {
                        requestLogger.warn(expectationSerializer.serialize(new Expectation(entry.getKey(), Times.once()).thenRespond(entry.getValue())));
                    }
                }
            }
        } else {
            for (Map.Entry<HttpRequest, HttpResponse> entry : requestResponseLog.entrySet()) {
                if (asJava) {
                    requestLogger.warn(expectationSerializer.serializeAsJava(new Expectation(entry.getKey(), Times.once()).thenRespond(entry.getValue())));
                } else {
                    requestLogger.warn(expectationSerializer.serialize(new Expectation(entry.getKey(), Times.once()).thenRespond(entry.getValue())));
                }
            }
        }
    }

    public synchronized Expectation[] retrieve(HttpRequest httpRequest) {
        List<Expectation> expectations = new ArrayList<Expectation>();
        if (httpRequest != null) {
            HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(httpRequest);
            for (HttpRequest key : requestResponseLog.keySet()) {
                for (HttpResponse value : requestResponseLog.getAll(key)) {
                    if (httpRequestMatcher.matches(key)) {
                        expectations.add(new Expectation(key, Times.once()).thenRespond(value));
                    }
                }
            }
        } else {
            for (HttpRequest key : requestResponseLog.keySet()) {
                for (HttpResponse value : requestResponseLog.getAll(key)) {
                    expectations.add(new Expectation(key, Times.once()).thenRespond(value));
                }
            }
        }
        return expectations.toArray(new Expectation[expectations.size()]);
    }

    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer();

    public synchronized String verify(Verification verification) {
        if (verification != null) {
            List<HttpRequest> requests = new ArrayList<HttpRequest>();
            if (verification.getHttpRequest() != null) {
                HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(verification.getHttpRequest());
                for (HttpRequest httpRequest : requestResponseLog.keySet()) {
                    if (httpRequestMatcher.matches(httpRequest)) {
                        requests.add(httpRequest);
                    }
                }
            }

            HttpRequest[] httpRequests = requestResponseLog.keySet().toArray(new HttpRequest[requests.size()]);
            if (requests.isEmpty()) {
                return "expected:<" + httpRequestSerializer.serialize(verification.getHttpRequest()) + "> but was:<" + (httpRequests.length == 1 ? httpRequestSerializer.serialize(httpRequests[0]) : httpRequestSerializer.serialize(httpRequests)) + ">";
            }
            if (verification.getTimes().isExact()) {
                if (requests.size() != verification.getTimes().getCount()) {
                    return "expected:<" + httpRequestSerializer.serialize(verification.getHttpRequest()) + "> but was:<" + (httpRequests.length == 1 ? httpRequestSerializer.serialize(httpRequests[0]) : httpRequestSerializer.serialize(httpRequests)) + ">";
                }
            } else {
                if (requests.size() < verification.getTimes().getCount()) {
                    return "expected:<" + httpRequestSerializer.serialize(verification.getHttpRequest()) + "> but was:<" + (httpRequests.length == 1 ? httpRequestSerializer.serialize(httpRequests[0]) : httpRequestSerializer.serialize(httpRequests)) + ">";
                }
            }
        }

        return "";
    }
}
