package org.mockserver.client.serialization.model;

import org.mockserver.model.EqualsHashCodeToString;
import org.mockserver.model.HttpCallback;

/**
 * @author jamesdbloom
 */
public class HttpCallbackDTO extends EqualsHashCodeToString {

    private String callbackClass;

    public HttpCallbackDTO(HttpCallback httpCallback) {
        callbackClass = httpCallback.getCallbackClass();
    }

    public HttpCallbackDTO() {
    }

    public HttpCallback buildObject() {
        return new HttpCallback()
                .withCallbackClass(callbackClass);
    }

    public String getCallbackClass() {
        return callbackClass;
    }

    public HttpCallbackDTO setCallbackClass(String callbackClass) {
        this.callbackClass = callbackClass;
        return this;
    }
}

