package org.mockserver.client.serialization.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.mockserver.client.serialization.ObjectMapperFactory;
import org.mockserver.client.serialization.model.JsonBodyDTO;
import org.mockserver.client.serialization.model.JsonSchemaBodyDTO;
import org.mockserver.matchers.JsonBodyMatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.JsonSchemaBody;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JsonSchemaBodyDTOSerializerTest {

    @Test
    public void shouldSerializeJsonBody() throws JsonProcessingException {
        assertThat(ObjectMapperFactory.createObjectMapper().writeValueAsString(new JsonSchemaBodyDTO(new JsonSchemaBody("{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"integer\"}}, \"required\": [\"id\"]}"))),
                is("{\"type\":\"JSON_SCHEMA\",\"value\":\"{\\\"type\\\": \\\"object\\\", \\\"properties\\\": {\\\"id\\\": {\\\"type\\\": \\\"integer\\\"}}, \\\"required\\\": [\\\"id\\\"]}\"}"));
    }

}