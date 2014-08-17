package org.mockserver.mock;

import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpForward;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockserver.model.HttpRequest.request;

/**
 * @author jamesdbloom
 */
public class ExpectationTest {

    @Test
    public void shouldConstructAndGetFields() {
        // given
        HttpRequest httpRequest = new HttpRequest();
        HttpResponse httpResponse = new HttpResponse();
        HttpForward httpForward = new HttpForward();
        Times times = Times.exactly(3);

        // when
        Expectation expectationThatResponds = new Expectation(httpRequest, times).thenRespond(httpResponse);

        // then
        assertEquals(httpRequest, expectationThatResponds.getHttpRequest());
        assertEquals(httpResponse, expectationThatResponds.getHttpResponse(false));
        assertNull(expectationThatResponds.getHttpForward());
        assertEquals(httpResponse, expectationThatResponds.getAction(false));
        assertEquals(times, expectationThatResponds.getTimes());

        // when
        Expectation expectationThatForwards = new Expectation(httpRequest, times).thenForward(httpForward);

        // then
        assertEquals(httpRequest, expectationThatForwards.getHttpRequest());
        assertNull(expectationThatForwards.getHttpResponse(false));
        assertEquals(httpForward, expectationThatForwards.getHttpForward());
        assertEquals(httpForward, expectationThatForwards.getAction(false));
        assertEquals(times, expectationThatForwards.getTimes());
    }

    @Test
    public void shouldAllowForNulls() {
        // when
        Expectation expectation = new Expectation(null, null).thenRespond(null).thenForward(null);

        // then
        expectation.setNotUnlimitedResponses();
        assertTrue(expectation.matches(null));
        assertTrue(expectation.matches(new HttpRequest()));
        assertFalse(expectation.contains(null));
        assertNull(expectation.getHttpRequest());
        assertNull(expectation.getHttpResponse(false));
        assertNull(expectation.getHttpForward());
        assertNull(expectation.getTimes());
    }

    @Test
    public void shouldMatchCorrectly() {
        // when request null should return true
        assertTrue(new Expectation(null, null).thenRespond(null).thenForward(null).matches(null));
        assertTrue(new Expectation(null, Times.unlimited()).thenRespond(null).thenForward(null).matches(null));

        // when request null should return true and should decrement times remaining
        Expectation expectation = new Expectation(null, Times.once());
        assertTrue(expectation.thenRespond(null).thenForward(null).matches(null));
        assertThat(expectation.getTimes().getRemainingTimes(), is(0));

        // when basic matching request should return true
        assertTrue(new Expectation(request(), null).thenRespond(null).thenForward(null).matches(request()));
        assertTrue(new Expectation(request(), Times.unlimited()).thenRespond(null).thenForward(null).matches(request()));

        // when basic matching request should return true and should decrement times remaining
        expectation = new Expectation(request(), Times.once());
        assertTrue(expectation.thenRespond(null).thenForward(null).matches(request()));
        assertThat(expectation.getTimes().getRemainingTimes(), is(0));

        // when un-matching request should return false
        assertFalse(new Expectation(request().withPath("un-matching"), null).thenRespond(null).thenForward(null).matches(request()));
        assertFalse(new Expectation(request().withPath("un-matching"), Times.unlimited()).thenRespond(null).thenForward(null).matches(request()));
        assertFalse(new Expectation(request().withPath("un-matching"), Times.once()).thenRespond(null).thenForward(null).matches(request()));

        // when no times left should return false
        assertFalse(new Expectation(null, Times.exactly(0)).thenRespond(null).thenForward(null).matches(null));
        assertFalse(new Expectation(request(), Times.exactly(0)).thenRespond(null).thenForward(null).matches(request()));
        assertFalse(new Expectation(request().withPath("un-matching"), Times.exactly(0)).thenRespond(null).thenForward(null).matches(request()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldPreventForwardAfterResponse() {
        // given
        HttpRequest httpRequest = new HttpRequest();
        HttpResponse httpResponse = new HttpResponse();
        HttpForward httpForward = new HttpForward();

        // then
        new Expectation(httpRequest, Times.once()).thenRespond(httpResponse).thenForward(httpForward);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldPreventResponseAfterForward() {
        // given
        HttpRequest httpRequest = new HttpRequest();
        HttpResponse httpResponse = new HttpResponse();
        HttpForward httpForward = new HttpForward();

        // then
        new Expectation(httpRequest, Times.once()).thenForward(httpForward).thenRespond(httpResponse);
    }
}
