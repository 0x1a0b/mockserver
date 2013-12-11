package org.mockserver.client.serialization;

import org.codehaus.jackson.map.ObjectMapper;
import org.mockserver.client.serialization.model.ExpectationDTO;
import org.mockserver.mock.Expectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author jamesdbloom
 */
public class ExpectationSerializer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();

    public String serialize(Expectation expectation) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ExpectationDTO(expectation));
        } catch (IOException ioe) {
            logger.error(String.format("Exception while serializing expectation to JSON with value %s", expectation), ioe);
            throw new RuntimeException(String.format("Exception while serializing expectation to JSON with value %s", expectation), ioe);
        }
    }

    public Expectation deserialize(byte[] jsonExpectation) {
        if (jsonExpectation.length == 0) throw new IllegalArgumentException("Expected an JSON expectation object but request body is empty");
        Expectation expectation = null;
        try {
            ExpectationDTO expectationDTO = objectMapper.readValue(jsonExpectation, ExpectationDTO.class);
            if (expectationDTO != null) {
                expectation = expectationDTO.buildObject();
            }
        } catch (IOException ioe) {
            logger.error("Exception while parsing response [" + new String(jsonExpectation) + "] for http response expectation", ioe);
            throw new RuntimeException("Exception while parsing response [" + new String(jsonExpectation) + "] for http response expectation", ioe);
        }
        return expectation;
    }

}
