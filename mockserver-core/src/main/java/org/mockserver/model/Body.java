package org.mockserver.model;

/**
 * @author jamesdbloom
 */
public abstract class Body<T> extends ObjectWithReflectiveEqualsHashCodeToString {

    private final Type type;

    public Body(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract T getValue();

    public abstract byte[] getRawBytes();

    public enum Type {
        PARAMETERS,
        XPATH,
        JSON,
        REGEX,
        STRING,
        BINARY
    }
}
