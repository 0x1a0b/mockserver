package org.mockserver.model;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author jamesdbloom
 */
public class HttpRequestTest {

    @Test
    public void returnsPath() {
        assertEquals("somepath", new HttpRequest().withPath("somepath").getPath());
    }

    @Test
    public void returnsBody() {
        assertEquals("somebody", new HttpRequest().withBody("somebody").getBody());
    }

    @Test
    public void returnsHeaders() {
        assertEquals(new Header("name", "value"), new HttpRequest().withHeaders(new Header("name", "value")).getHeaders().get(0));
        assertEquals(new Header("name", "value_one", "value_two"), new HttpRequest().withHeaders(new Header("name", "value_one", "value_two")).getHeaders().get(0));
        assertEquals(new Header("name", (Collection<String>)null), new HttpRequest().withHeaders(new Header("name")).getHeaders().get(0));
        assertEquals(new Header("name"), new HttpRequest().withHeaders(new Header("name")).getHeaders().get(0));
    }

    @Test
    public void returnsCookies() {
        assertEquals(new Cookie("name", "value"), new HttpRequest().withCookies(new Cookie("name", "value")).getCookies().get(0));
        assertEquals(new Cookie("name", "value_one", "value_two"), new HttpRequest().withCookies(new Cookie("name", "value_one", "value_two")).getCookies().get(0));
        assertEquals(new Cookie("name", (Collection<String>)null), new HttpRequest().withCookies(new Cookie("name")).getCookies().get(0));
        assertEquals(new Cookie("name"), new HttpRequest().withCookies(new Cookie("name")).getCookies().get(0));
    }

    @Test
    public void returnsParameters() {
        assertEquals("name=value", new HttpRequest().withQueryString("name=value").getQueryString());
    }

}
