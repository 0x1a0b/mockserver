package org.mockserver.client.serialization;

import org.apache.commons.lang3.StringUtils;
import org.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

import javax.xml.bind.DatatypeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Charsets.UTF_8;

/**
 * @author jamesdbloom
 */
public class Base64Converter extends ObjectWithReflectiveEqualsHashCodeToString {

    private static final String BASE64_PATTERN = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";

    public byte[] base64StringToBytes(String data) {
        if (data == null) {
            return new byte[0];
        }
        if (!data.matches(BASE64_PATTERN)) {
            return data.getBytes(UTF_8);
        }
        return DatatypeConverter.parseBase64Binary(data);
    }

    public String bytesToBase64String(byte[] data) {
        return DatatypeConverter.printBase64Binary(data);
    }

}
