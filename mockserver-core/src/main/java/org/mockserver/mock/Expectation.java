package org.mockserver.mock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.*;

/**
 * @author jamesdbloom
 */
public class Expectation extends ObjectWithJsonToString {

    private final HttpRequest httpRequest;
    private final Times times;
    private final TimeToLive timeToLive;
    private HttpResponse httpResponse;
    private HttpTemplate httpResponseTemplate;
    private HttpForward httpForward;
    private HttpTemplate httpForwardTemplate;
    private HttpError httpError;
    private HttpClassCallback httpClassCallback;
    private HttpObjectCallback httpObjectCallback;

    public Expectation(HttpRequest httpRequest) {
        this(httpRequest, Times.unlimited(), TimeToLive.unlimited());
    }

    public Expectation(HttpRequest httpRequest, Times times, TimeToLive timeToLive) {
        this.httpRequest = httpRequest;
        this.times = times;
        this.timeToLive = timeToLive;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public HttpTemplate getHttpResponseTemplate() {
        return httpResponseTemplate;
    }

    public HttpTemplate getHttpForwardTemplate() {
        return httpForwardTemplate;
    }

    public HttpForward getHttpForward() {
        return httpForward;
    }

    public HttpError getHttpError() {
        return httpError;
    }

    public HttpClassCallback getHttpClassCallback() {
        return httpClassCallback;
    }

    public HttpObjectCallback getHttpObjectCallback() {
        return httpObjectCallback;
    }

    @JsonIgnore
    public Action getAction() {
        if (httpResponse != null) {
            return getHttpResponse();
        } else if (httpResponseTemplate != null) {
            return getHttpResponseTemplate();
        } else if (httpForward != null) {
            return getHttpForward();
        } else if (httpForwardTemplate != null) {
            return getHttpForwardTemplate();
        } else if (httpError != null) {
            return getHttpError();
        } else if (httpClassCallback != null) {
            return getHttpClassCallback();
        } else if (httpObjectCallback != null) {
            return getHttpObjectCallback();
        } else {
            return null;
        }
    }

    public Times getTimes() {
        return times;
    }

    public TimeToLive getTimeToLive() {
        return timeToLive;
    }

    public Expectation thenRespond(HttpResponse httpResponse) {
        if (httpResponse != null) {
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a response template has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set a response once an error has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a class callback has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a response once an object callback has been set");
            }
            this.httpResponse = httpResponse;
        }
        return this;
    }

    public Expectation thenRespond(HttpTemplate httpTemplate) {
        if (httpTemplate != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set a response template once a response has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set a response template once a forward has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set a response template once an error has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a response template once a class callback has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a response template once an object callback has been set");
            }
            httpTemplate.setActionType(Action.Type.RESPONSE_TEMPLATE);
            this.httpResponseTemplate = httpTemplate;
        }
        return this;
    }

    public Expectation thenForward(HttpForward httpForward) {
        if (httpForward != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a response has been set");
            }
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a response template has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once an error has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a class callback has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once an object callback has been set");
            }
            this.httpForward = httpForward;
        }
        return this;
    }

    public Expectation thenForward(HttpTemplate httpTemplate) {
        if (httpTemplate != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a response has been set");
            }
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a response template has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once an error has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once a class callback has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a forward once an object callback has been set");
            }
            httpTemplate.setActionType(Action.Type.FORWARD_TEMPLATE);
            this.httpForwardTemplate = httpTemplate;
        }
        return this;
    }

    public Expectation thenError(HttpError httpError) {
        if (httpError != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set an error once a response has been set");
            }
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set an error once a response template has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set an error once a forward has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a error once a class callback has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a error once an object callback has been set");
            }
            this.httpError = httpError;
        }
        return this;
    }

    public Expectation thenCallback(HttpClassCallback httpClassCallback) {
        if (httpClassCallback != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set a class callback once a response has been set");
            }
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a class callback once a response template has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set a class callback once a forward has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set a class callback once an error has been set");
            }
            if (httpObjectCallback != null) {
                throw new IllegalArgumentException("It is not possible to set a class callback once an object callback has been set");
            }
            this.httpClassCallback = httpClassCallback;
        }
        return this;
    }

    public Expectation thenCallback(HttpObjectCallback httpObjectCallback) {
        if (httpObjectCallback != null) {
            if (httpResponse != null) {
                throw new IllegalArgumentException("It is not possible to set an object callback once a response has been set");
            }
            if (httpResponseTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set an object callback once a response template has been set");
            }
            if (httpForward != null) {
                throw new IllegalArgumentException("It is not possible to set an object callback once a forward has been set");
            }
            if (httpForwardTemplate != null) {
                throw new IllegalArgumentException("It is not possible to set a response once a forward template has been set");
            }
            if (httpError != null) {
                throw new IllegalArgumentException("It is not possible to set an object callback once an error has been set");
            }
            if (httpClassCallback != null) {
                throw new IllegalArgumentException("It is not possible to set an object callback once an class callback has been set");
            }
            this.httpObjectCallback = httpObjectCallback;
        }
        return this;
    }

    @JsonIgnore
    public boolean isActive() {
        return hasRemainingMatches() && isStillAlive();
    }

    private boolean hasRemainingMatches() {
        return times == null || times.greaterThenZero();
    }

    private boolean isStillAlive() {
        return timeToLive == null || timeToLive.stillAlive();
    }

    public Expectation decrementRemainingMatches() {
        if (times != null) {
            times.decrement();
        }
        return this;
    }

    public void setNotUnlimitedResponses() {
        if (times != null) {
            times.setNotUnlimitedResponses();
        }
    }

    public boolean contains(HttpRequest httpRequest) {
        return httpRequest != null && this.httpRequest.equals(httpRequest);
    }

    public Expectation clone() {
        return new Expectation(httpRequest, times.clone(), timeToLive)
            .thenRespond(httpResponse)
            .thenRespond(httpResponseTemplate)
            .thenForward(httpForward)
            .thenForward(httpForwardTemplate)
            .thenError(httpError)
            .thenCallback(httpClassCallback)
            .thenCallback(httpObjectCallback);
    }
}
