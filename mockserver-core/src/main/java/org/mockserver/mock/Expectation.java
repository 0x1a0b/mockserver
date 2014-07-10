package org.mockserver.mock;

import org.mockserver.matchers.HttpRequestMatcher;
import org.mockserver.matchers.MatcherBuilder;
import org.mockserver.matchers.Times;
import org.mockserver.model.*;

/**
 * @author jamesdbloom
 */
public class Expectation extends EqualsHashCodeToString {

    private final HttpRequest httpRequest;
    private final Times times;
    private final HttpRequestMatcher httpRequestMatcher;
    private HttpResponse httpResponse;
    private HttpForward httpForward;

    public Expectation(HttpRequest httpRequest, Times times) {
        this.httpRequest = httpRequest;
        this.times = times;
        this.httpRequestMatcher = new MatcherBuilder().transformsToMatcher(this.httpRequest);
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpResponse getHttpResponse(boolean applyDelay) {
        if (httpResponse != null) {
            if (applyDelay) {
                return httpResponse.applyDelay();
            } else {
                return httpResponse;
            }
        } else {
            return null;
        }
    }

    public HttpForward getHttpForward() {
        return httpForward;
    }

    public Action getAction(boolean applyDelay) {
        if (httpResponse != null) {
            return getHttpResponse(applyDelay);
        } else {
            return getHttpForward();
        }
    }

    public Times getTimes() {
        return times;
    }

    public Expectation thenRespond(HttpResponse httpResponse) {
        if (httpResponse != null) {
            if (httpForward != null)
                throw new IllegalArgumentException("It is not possible to set a response once a forward has been set");
            this.httpResponse = httpResponse;
        }
        return this;
    }

    public Expectation thenForward(HttpForward httpForward) {
        if (httpForward != null) {
            if (httpResponse != null)
                throw new IllegalArgumentException("It is not possible to set a forward once a response has been set");
            this.httpForward = httpForward;
        }
        return this;
    }

    public boolean matches(HttpRequest httpRequest) {
        logger.trace("\nMatching expectation: \n{} \nwith incoming http: \n{}" + System.getProperty("line.separator"), this.httpRequest, httpRequest);
        boolean matches =
                (times == null || times.greaterThenZero()) &&
                        (
                                (httpRequest == null && this.httpRequest == null) || this.httpRequestMatcher.matches(httpRequest)
                        );
        if (matches && times != null) {
            times.decrement();
        }
        return matches;
    }

    public void setNotUnlimitedResponses() {
        if (times != null) {
            times.setNotUnlimitedResponses();
        }
    }

    public boolean contains(HttpRequest httpRequest) {
        return httpRequest != null && this.httpRequest.equals(httpRequest);
    }
}
