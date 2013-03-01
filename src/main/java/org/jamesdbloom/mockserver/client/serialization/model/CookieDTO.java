package org.jamesdbloom.mockserver.client.serialization.model;

import org.jamesdbloom.mockserver.model.Cookie;

/**
 * @author jamesdbloom
 */
public class CookieDTO extends KeyToMultiValueDTO<String, String> {

    public CookieDTO(Cookie cookie) {
        super(cookie);
    }

    protected CookieDTO() {
    }

    public Cookie buildObject() {
        return new Cookie(getName(), getValues());
    }
}
