package org.mockserver.matchers;

import com.google.common.collect.Multimap;
import org.mockserver.model.EqualsHashCodeToString;
import org.mockserver.model.KeyToMultiValue;

import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * @author jamesdbloom
 */
public class MapMatcher extends EqualsHashCodeToString implements Matcher<List<KeyToMultiValue>> {
    private final Multimap<String, String> multimap;

    public MapMatcher(Multimap<String, String> multimap) {
        this.multimap = multimap;
    }

    public boolean matches(List<KeyToMultiValue> values) {
        boolean result = false;

        if (containsAll(KeyToMultiValue.toMultiMap(values), this.multimap)) {
            result = true;
        } else {
            logger.trace("Map [{}] is not a subset of [{}]", this.multimap, KeyToMultiValue.toMultiMap(values));
        }

        return result;
    }

    private boolean containsAll(Multimap<String, String> superSet, Multimap<String, String> subSet) {
        for (String key : subSet.keySet()) {
            for (String subSetValue : subSet.get(key)) {
                if (!superSet.containsEntry(key, subSetValue)) {
                    if (!superSet.containsKey(key)) { // check if sub-set key exists in super-set
                        return false;
                    } else { // check if sub-set value matches at least one super-set values using regex
                        boolean atLeastOneRegexMatches = false;
                        for (String superSetValue : superSet.get(key)) {
                            try {
                                if (superSetValue.matches(subSetValue)) {
                                    atLeastOneRegexMatches = true;
                                }
                            } catch (PatternSyntaxException pse) {
                                logger.error("Error while matching regex [" + subSetValue + "] for string [" + superSetValue + "] " + pse.getMessage());
                            }
                        }
                        if (!atLeastOneRegexMatches) {
                            return false;
                        }
                    }
                }

            }
        }
        return true;
    }
}
