package org.mockserver.client.serialization;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.serialization.model.CookieDTO;
import org.mockserver.client.serialization.model.HeaderDTO;
import org.mockserver.client.serialization.model.HttpRequestDTO;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author jamesdbloom
 */
public class HttpRequestSerializerTest {

    private final HttpRequest fullHttpRequest =
            new HttpRequest()
                    .withMethod("GET")
                    .withURL("url")
                    .withPath("somepath")
                    .withQueryString("queryString")
                    .withBody("somebody")
                    .withHeaders(new Header("headerName", "headerValue"))
                    .withCookies(new Cookie("cookieName", "cookieValue"));
    private final HttpRequestDTO fullHttpRequestDTO =
            new HttpRequestDTO()
                    .setMethod("GET")
                    .setURL("url")
                    .setPath("somepath")
                    .setQueryString("queryString")
                    .setBody("somebody")
                    .setHeaders(Arrays.<HeaderDTO>asList(new HeaderDTO(new Header("headerName", Arrays.asList("headerValue")))))
                    .setCookies(Arrays.<CookieDTO>asList(new CookieDTO(new Cookie("cookieName", Arrays.asList("cookieValue")))));
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ObjectWriter objectWriter;
    @InjectMocks
    private HttpRequestSerializer httpRequestSerializer;

    @Before
    public void setupTestFixture() {
        httpRequestSerializer = spy(new HttpRequestSerializer());

        initMocks(this);
    }

    @Test
    public void deserialize() throws IOException {
        // given
        byte[] requestBytes = "requestBytes".getBytes();
        when(objectMapper.readValue(eq(requestBytes), same(HttpRequestDTO.class))).thenReturn(fullHttpRequestDTO);

        // when
        HttpRequest httpRequest = httpRequestSerializer.deserialize(requestBytes);

        // then
        assertEquals(fullHttpRequest, httpRequest);
    }

    @Test
    public void deserializeHandleException() throws IOException {
        // given
        byte[] requestBytes = "requestBytes".getBytes();
        when(objectMapper.readValue(eq(requestBytes), same(HttpRequestDTO.class))).thenThrow(new IOException());

        try {
            // when
            httpRequestSerializer.deserialize(requestBytes);
        } catch (Throwable t) {
            fail();
        }
    }

    @Test
    public void serialize() throws IOException {
        // given
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);

        // when
        httpRequestSerializer.serialize(fullHttpRequest);

        // then
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(fullHttpRequestDTO);
    }

    @Test(expected = RuntimeException.class)
    public void serializeHandlesException() throws IOException {
        // given
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any(HttpRequestDTO.class))).thenThrow(new IOException());

        // when
        httpRequestSerializer.serialize(httpRequest);
    }
}
