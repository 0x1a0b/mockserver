package org.mockserver.client.serialization.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.mockserver.model.StringBody;

import java.io.IOException;

/**
 * @author jamesdbloom
 */
public class StringBodySerializer extends StdSerializer<StringBody> {

    public StringBodySerializer() {
        super(StringBody.class);
    }

    @Override
    public void serialize(StringBody stringBody, JsonGenerator json, SerializerProvider provider) throws IOException {
        if (stringBody.isNot() != null && stringBody.isNot()) {
            json.writeStartObject();
            json.writeBooleanField("not", true);
            json.writeStringField("type", stringBody.getType().name());
            json.writeStringField("value", stringBody.getValue());
            json.writeEndObject();
        } else {
            json.writeString(stringBody.getValue());
        }
    }
}
