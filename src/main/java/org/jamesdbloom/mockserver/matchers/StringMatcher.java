package org.jamesdbloom.mockserver.matchers;

import com.google.common.base.Strings;
import org.jamesdbloom.mockserver.model.ModelObject;

/**
 * @author jamesdbloom
 */
public class StringMatcher extends ModelObject implements Matcher<String> {
    private final String path;

    public StringMatcher(String path) {
        this.path = path;
    }

    public boolean matches(String path) {
        boolean result = false;

        if (Strings.isNullOrEmpty(this.path)) {
            result = true;
        } else if (path != null && path.matches(this.path)) {
            result = true;
        }

        return result;
    }
}
