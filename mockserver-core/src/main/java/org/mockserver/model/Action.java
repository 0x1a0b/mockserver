package org.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author jamesdbloom
 */
public abstract class Action extends ObjectWithJsonToString {

    @JsonIgnore
    public abstract Type getType();

    public enum Type {
        FORWARD,
        FORWARD_TEMPLATE,
        RESPONSE,
        RESPONSE_TEMPLATE,
        CALLBACK,
        ERROR
    }
}
