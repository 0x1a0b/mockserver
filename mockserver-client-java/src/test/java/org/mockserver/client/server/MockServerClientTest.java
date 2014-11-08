package org.mockserver.client.server;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.http.ApacheHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.VerificationSerializer;
import org.mockserver.client.serialization.model.*;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.*;
import org.mockserver.verify.Verification;
import org.mockserver.verify.VerificationTimes;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.atLeast;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author jamesdbloom
 */
public class MockServerClientTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private ApacheHttpClient mockApacheHttpClient;
    @Mock
    private ExpectationSerializer expectationSerializer;
    @Mock
    private VerificationSerializer verificationSerializer;
    @InjectMocks
    private MockServerClient mockServerClient;

    @Before
    public void setupTestFixture() throws Exception {
        mockServerClient = new MockServerClient("localhost", 8080);

        initMocks(this);
    }

    @Test
    public void shouldHandleNullHostnameExceptions() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Host can not be null or empty"));


        // when
        new MockServerClient(null, 8080);
    }

    @Test
    public void shouldHandleNullContextPathExceptions() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("ContextPath can not be null"));


        // when
        new MockServerClient("localhost", 8080, null);
    }

    @Test
    public void shouldSetupExpectationWithResponse() {
        // given
        HttpRequest httpRequest =
                new HttpRequest()
                        .withPath("/some_path")
                        .withBody(new StringBody("some_request_body", Body.Type.STRING));
        HttpResponse httpResponse =
                new HttpResponse()
                        .withBody("some_response_body")
                        .withHeaders(new Header("responseName", "responseValue"));

        // when
        ForwardChainExpectation forwardChainExpectation = mockServerClient.when(httpRequest);
        forwardChainExpectation.respond(httpResponse);

        // then
        Expectation expectation = forwardChainExpectation.getExpectation();
        assertTrue(expectation.matches(httpRequest));
        assertSame(httpResponse, expectation.getHttpResponse(false));
        assertEquals(Times.unlimited(), expectation.getTimes());
    }

    @Test
    public void shouldSetupExpectationWithForward() {
        // given
        HttpRequest httpRequest =
                new HttpRequest()
                        .withPath("/some_path")
                        .withBody(new StringBody("some_request_body", Body.Type.STRING));
        HttpForward httpForward =
                new HttpForward()
                        .withHost("some_host")
                        .withPort(9090)
                        .withScheme(HttpForward.Scheme.HTTPS);

        // when
        ForwardChainExpectation forwardChainExpectation = mockServerClient.when(httpRequest);
        forwardChainExpectation.forward(httpForward);

        // then
        Expectation expectation = forwardChainExpectation.getExpectation();
        assertTrue(expectation.matches(httpRequest));
        assertSame(httpForward, expectation.getHttpForward());
        assertEquals(Times.unlimited(), expectation.getTimes());
    }

    @Test
    public void shouldSetupExpectationWithCallback() {
        // given
        HttpRequest httpRequest =
                new HttpRequest()
                        .withPath("/some_path")
                        .withBody(new StringBody("some_request_body", Body.Type.STRING));
        HttpCallback httpCallback =
                new HttpCallback()
                        .withCallbackClass("some_class");

        // when
        ForwardChainExpectation forwardChainExpectation = mockServerClient.when(httpRequest);
        forwardChainExpectation.callback(httpCallback);

        // then
        Expectation expectation = forwardChainExpectation.getExpectation();
        assertTrue(expectation.matches(httpRequest));
        assertSame(httpCallback, expectation.getHttpCallback());
        assertEquals(Times.unlimited(), expectation.getTimes());
    }

    @Test
    public void shouldSendExpectationRequestWithExactTimes() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING)),
                        Times.exactly(3)
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_response_body")
                                .withHeaders(new Header("responseName", "responseValue"))
                );

        // then
        verify(expectationSerializer).serialize(
                new ExpectationDTO()
                        .setHttpRequest(new HttpRequestDTO(new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))))
                        .setHttpResponse(new HttpResponseDTO(new HttpResponse()
                                .withBody("some_response_body")
                                .withHeaders(new Header("responseName", "responseValue"))))
                        .setTimes(new TimesDTO(Times.exactly(3)))
                        .buildObject()
        );
    }

    @Test
    public void shouldSendExpectationWithForward() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING)),
                        Times.exactly(3)
                )
                .forward(
                        new HttpForward()
                                .withHost("some_host")
                                .withPort(9090)
                                .withScheme(HttpForward.Scheme.HTTPS)
                );

        // then
        verify(expectationSerializer).serialize(
                new ExpectationDTO()
                        .setHttpRequest(new HttpRequestDTO(new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))))
                        .setHttpForward(
                                new HttpForwardDTO(
                                        new HttpForward()
                                                .withHost("some_host")
                                                .withPort(9090)
                                                .withScheme(HttpForward.Scheme.HTTPS)
                                )
                        )
                        .setTimes(new TimesDTO(Times.exactly(3)))
                        .buildObject()
        );
    }


    @Test
    public void shouldSendExpectationWithCallback() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING)),
                        Times.exactly(3)
                )
                .callback(
                        new HttpCallback()
                                .withCallbackClass("some_class")
                );

        // then
        verify(expectationSerializer).serialize(
                new ExpectationDTO()
                        .setHttpRequest(new HttpRequestDTO(new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))))
                        .setHttpCallback(
                                new HttpCallbackDTO(
                                        new HttpCallback()
                                                .withCallbackClass("some_class")
                                )
                        )
                        .setTimes(new TimesDTO(Times.exactly(3)))
                        .buildObject()
        );
    }

    @Test
    public void shouldSendExpectationRequestWithDefaultTimes() throws Exception {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_response_body")
                                .withHeaders(new Header("responseName", "responseValue"))
                );

        // then
        verify(expectationSerializer).serialize(
                new ExpectationDTO()
                        .setHttpRequest(new HttpRequestDTO(new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))))
                        .setHttpResponse(new HttpResponseDTO(new HttpResponse()
                                .withBody("some_response_body")
                                .withHeaders(new Header("responseName", "responseValue"))))
                        .setTimes(new TimesDTO(Times.unlimited()))
                        .buildObject()
        );
    }

    @Test
    public void shouldSendResetRequest() throws Exception {
        // when
        mockServerClient.reset();

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/reset", "");
    }

    @Test
    public void shouldSendStopRequest() throws Exception {
        // when
        mockServerClient.stop();

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/stop", "");
    }

    @Test
    public void shouldSendDumpToLogRequest() throws Exception {
        // when
        mockServerClient.dumpToLog();

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/dumpToLog", "");
    }

    @Test
    public void shouldSendClearRequest() throws Exception {
        // when
        mockServerClient
                .clear(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))
                );

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/clear", "" +
                "{" + System.getProperty("line.separator") +
                "  \"path\" : \"/some_path\"," + System.getProperty("line.separator") +
                "  \"body\" : \"some_request_body\"" + System.getProperty("line.separator") +
                "}");
    }

    @Test
    public void shouldSendClearRequestForNullRequest() throws Exception {
        // when
        mockServerClient
                .clear(null);

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/clear", "");
    }

    @Test
    public void shouldReceiveExpectationsAsObjects() throws UnsupportedEncodingException {
        // given
        Expectation[] expectations = {};
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("body");
        when(expectationSerializer.deserializeArray("body")).thenReturn(expectations);

        // when
        assertSame(expectations, mockServerClient
                .retrieveAsExpectations(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))
                ));

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/retrieve", "" +
                "{" + System.getProperty("line.separator") +
                "  \"path\" : \"/some_path\"," + System.getProperty("line.separator") +
                "  \"body\" : \"some_request_body\"" + System.getProperty("line.separator") +
                "}");
        verify(expectationSerializer).deserializeArray("body");
    }

    @Test
    public void shouldReceiveExpectationsAsObjectsWithNullRequest() throws UnsupportedEncodingException {
        // given
        Expectation[] expectations = {};
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("body");
        when(expectationSerializer.deserializeArray("body")).thenReturn(expectations);

        // when
        assertSame(expectations, mockServerClient.retrieveAsExpectations(null));

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/retrieve", "");
        verify(expectationSerializer).deserializeArray("body");
    }

    @Test
    public void shouldReceiveExpectationsAsJSON() throws UnsupportedEncodingException {
        // given
        String expectations = "body";
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("body");

        // when
        assertEquals(expectations, mockServerClient
                .retrieveAsJSON(
                        new HttpRequest()
                                .withPath("/some_path")
                                .withBody(new StringBody("some_request_body", Body.Type.STRING))
                ));

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/retrieve", "" +
                "{" + System.getProperty("line.separator") +
                "  \"path\" : \"/some_path\"," + System.getProperty("line.separator") +
                "  \"body\" : \"some_request_body\"" + System.getProperty("line.separator") +
                "}");
    }

    @Test
    public void shouldReceiveExpectationsAsJSONWithNullRequest() throws UnsupportedEncodingException {
        // given
        String expectations = "body";
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("body");

        // when
        assertEquals(expectations, mockServerClient.retrieveAsJSON(null));

        // then
        verify(mockApacheHttpClient).sendPUTRequest("http://localhost:8080", "/retrieve", "");
    }

    @Test
    public void shouldVerifyDoesNotMatchSingleRequestNoVerificationTimes() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest);

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(atLeast(1)));
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesNotMatchMultipleRequestsNoVerificationTimes() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, httpRequest);

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(atLeast(1)));
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesMatchSingleRequestNoVerificationTimes() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest);

            // then
        } catch (AssertionError ae) {
            fail();
        }

        // then
        verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(atLeast(1)));
    }

    @Test
    public void shouldVerifyDoesMatchSingleRequestOnce() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, once());

            // then
        } catch (AssertionError ae) {
            fail();
        }

        // then
        verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(once()));
    }

    @Test
    public void shouldVerifyDoesNotMatchRequestAtLeastOnce() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, atLeast(1));

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(atLeast(1)));
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesNotMatchRequestExactlyOnce() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, once());

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(once()));
            assertThat(ae.getMessage(), is("Request not found exactly once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesNotMatchRequestAtLeastTwice() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, atLeast(2));

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(atLeast(2)));
            assertThat(ae.getMessage(), is("Request not found at least 2 times expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesNotMatchRequestExactlyTwice() throws UnsupportedEncodingException {
        // given
        when(mockApacheHttpClient.sendPUTRequest(anyString(), anyString(), anyString())).thenReturn("expected:<foo> but was:<bar>");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body", Body.Type.STRING));

        try {
            mockServerClient.verify(httpRequest, exactly(2));

            // then
            fail();
        } catch (AssertionError ae) {
            verify(verificationSerializer).serialize(new Verification().withRequest(httpRequest).withTimes(exactly(2)));
            assertThat(ae.getMessage(), is("Request not found exactly 2 times expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldHandleNullHttpRequest() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest, VerificationTimes) requires a non null HttpRequest object"));

        // when
        mockServerClient.verify(null, VerificationTimes.exactly(2));
    }

    @Test
    public void shouldHandleNullVerificationTimes() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest, VerificationTimes) requires a non null VerificationTimes object"));

        // when
        mockServerClient.verify(request(), null);
    }
}
