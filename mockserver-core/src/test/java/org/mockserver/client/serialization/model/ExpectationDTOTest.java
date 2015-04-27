package org.mockserver.client.serialization.model;

import org.junit.Test;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpCallback;
import org.mockserver.model.HttpForward;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;

/**
 * @author jamesdbloom
 */
public class ExpectationDTOTest {

    @Test
    public void shouldReturnValueSetInConstructor() {
        // given
        HttpRequest httpRequest = new HttpRequest().withBody("some_body");
        HttpResponse httpResponse = new HttpResponse().withBody("some_response_body");
        HttpForward httpForward = new HttpForward().withHost("some_host");
        HttpCallback httpCallback = new HttpCallback().withCallbackClass("some_class");

        // when
        ExpectationDTO expectationWithResponse = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenRespond(httpResponse));

        // then
        assertThat(expectationWithResponse.getHttpRequest(), is(new HttpRequestDTO(httpRequest)));
        assertThat(expectationWithResponse.getTimes(), is(new TimesDTO(Times.exactly(3))));
        assertThat(expectationWithResponse.getHttpResponse(), is(new HttpResponseDTO(httpResponse)));
        assertNull(expectationWithResponse.getHttpForward());
        assertNull(expectationWithResponse.getHttpCallback());

        // when
        ExpectationDTO expectationWithForward = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenForward(httpForward));

        // then
        assertThat(expectationWithForward.getHttpRequest(), is(new HttpRequestDTO(httpRequest)));
        assertThat(expectationWithForward.getTimes(), is(new TimesDTO(Times.exactly(3))));
        assertNull(expectationWithForward.getHttpResponse());
        assertThat(expectationWithForward.getHttpForward(), is(new HttpForwardDTO(httpForward)));
        assertNull(expectationWithForward.getHttpCallback());

        // when
        ExpectationDTO expectationWithCallback = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenCallback(httpCallback));

        // then
        assertThat(expectationWithCallback.getHttpRequest(), is(new HttpRequestDTO(httpRequest)));
        assertThat(expectationWithCallback.getTimes(), is(new TimesDTO(Times.exactly(3))));
        assertNull(expectationWithCallback.getHttpResponse());
        assertNull(expectationWithCallback.getHttpForward());
        assertThat(expectationWithCallback.getHttpCallback(), is(new HttpCallbackDTO(httpCallback)));
    }

    @Test
    public void shouldBuildObject() {
        // given
        HttpRequest httpRequest = new HttpRequest().withBody("some_body");
        HttpResponse httpResponse = new HttpResponse().withBody("some_response_body");
        HttpForward httpForward = new HttpForward().withHost("some_host");
        HttpCallback httpCallback = new HttpCallback().withCallbackClass("some_class");

        // when
        Expectation expectationWithResponse = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenRespond(httpResponse)).buildObject();

        // then
        assertThat(expectationWithResponse.getHttpRequest(), is(httpRequest));
        assertThat(expectationWithResponse.getTimes(), is(Times.exactly(3)));
        assertThat(expectationWithResponse.getHttpResponse(false), is(httpResponse));
        assertNull(expectationWithResponse.getHttpForward());
        assertNull(expectationWithResponse.getHttpCallback());

        // when
        Expectation expectationWithForward = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenForward(httpForward)).buildObject();

        // then
        assertThat(expectationWithForward.getHttpRequest(), is(httpRequest));
        assertThat(expectationWithForward.getTimes(), is(Times.exactly(3)));
        assertNull(expectationWithForward.getHttpResponse(false));
        assertThat(expectationWithForward.getHttpForward(), is(httpForward));
        assertNull(expectationWithForward.getHttpCallback());

        // when
        Expectation expectationWithCallback = new ExpectationDTO(new Expectation(httpRequest, Times.exactly(3), TimeToLive.unlimited()).thenCallback(httpCallback)).buildObject();

        // then
        assertThat(expectationWithCallback.getHttpRequest(), is(httpRequest));
        assertThat(expectationWithCallback.getTimes(), is(Times.exactly(3)));
        assertNull(expectationWithCallback.getHttpResponse(false));
        assertNull(expectationWithCallback.getHttpForward());
        assertThat(expectationWithCallback.getHttpCallback(), is(httpCallback));
    }

    @Test
    public void shouldBuildObjectWithNulls() {
        // when
        Expectation expectation = new ExpectationDTO(new Expectation(null, null, TimeToLive.unlimited()).thenRespond(null).thenForward(null)).buildObject();

        // then
        assertThat(expectation.getHttpRequest(), is(nullValue()));
        assertThat(expectation.getTimes(), is(Times.once()));
        assertThat(expectation.getHttpResponse(false), is(nullValue()));
        assertThat(expectation.getHttpForward(), is(nullValue()));
        assertThat(expectation.getHttpCallback(), is(nullValue()));
    }

    @Test
    public void shouldReturnValueSetInSetter() {
        // given
        HttpRequestDTO httpRequest = new HttpRequestDTO(new HttpRequest().withBody("some_body"));
        TimesDTO times = new TimesDTO(Times.exactly(3));
        HttpResponseDTO httpResponse = new HttpResponseDTO(new HttpResponse().withBody("some_response_body"));
        HttpForwardDTO httpForward = new HttpForwardDTO(new HttpForward().withHost("some_host"));
        HttpCallbackDTO httpCallback = new HttpCallbackDTO(new HttpCallback().withCallbackClass("some_class"));

        // when
        ExpectationDTO expectation = new ExpectationDTO();
        expectation.setHttpRequest(httpRequest);
        expectation.setTimes(times);
        expectation.setHttpResponse(httpResponse);
        expectation.setHttpForward(httpForward);
        expectation.setHttpCallback(httpCallback);

        // then
        assertThat(expectation.getHttpRequest(), is(httpRequest));
        assertThat(expectation.getTimes(), is(times));
        assertThat(expectation.getHttpResponse(), is(httpResponse));
        assertThat(expectation.getHttpForward(), is(httpForward));
        assertThat(expectation.getHttpCallback(), is(httpCallback));
    }

    @Test
    public void shouldHandleNullObjectInput() {
        // when
        ExpectationDTO expectationDTO = new ExpectationDTO(null);

        // then
        assertThat(expectationDTO.getHttpRequest(), is(nullValue()));
        assertThat(expectationDTO.getTimes(), is(nullValue()));
        assertThat(expectationDTO.getHttpResponse(), is(nullValue()));
        assertThat(expectationDTO.getHttpForward(), is(nullValue()));
    }

    @Test
    public void shouldHandleNullFieldInput() {
        // when
        ExpectationDTO expectationDTO = new ExpectationDTO(new Expectation(null, null, TimeToLive.unlimited()));

        // then
        assertThat(expectationDTO.getHttpRequest(), is(nullValue()));
        assertThat(expectationDTO.getTimes(), is(nullValue()));
        assertThat(expectationDTO.getHttpResponse(), is(nullValue()));
        assertThat(expectationDTO.getHttpForward(), is(nullValue()));
        assertThat(expectationDTO.getHttpCallback(), is(nullValue()));
    }
}
