package org.mockserver.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.client.serialization.java.ExpectationToJavaSerializer;
import org.mockserver.client.serialization.java.HttpRequestToJavaSerializer;
import org.mockserver.log.model.ExpectationMatchLogEntry;
import org.mockserver.log.model.RequestLogEntry;
import org.mockserver.logging.LoggingFormatter;
import org.mockserver.model.HttpRequest;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author jamesdbloom
 */
public class HttpStateHandlerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer();
    private HttpRequestToJavaSerializer httpRequestToJavaSerializer = new HttpRequestToJavaSerializer();
    private ExpectationSerializer httpExpectationSerializer = new ExpectationSerializer();
    private ExpectationToJavaSerializer httpExpectationToJavaSerializer = new ExpectationToJavaSerializer();
    @Mock
    private LoggingFormatter mockLogFormatter;
    @InjectMocks
    private HttpStateHandler httpStateHandler;

    @Before
    public void prepareTestFixture() {
        httpStateHandler = new HttpStateHandler();
        initMocks(this);
    }

    @Test
    public void shouldClearLogsAndExpectationsForNullRequestMatcher() {
        // given - a request
        HttpRequest request = request();
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));

        // when
        httpStateHandler.clear(request);

        // then - correct log entries removed
        assertThat(httpStateHandler.retrieve(request), is(""));
        // then - correct expectations removed
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), nullValue());
        // then - activity logged
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("clearing expectations and request logs that match:{}", (Object) null);
    }

    @Test
    public void shouldClearLogsAndExpectations() {
        // given - a request
        HttpRequest request = request().withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_two")));

        // when
        httpStateHandler.clear(request);

        // then - correct log entries removed
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_one")))),
                is("")
        );
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_two")))),
                is(httpRequestSerializer.serialize(Collections.singletonList(
                        request("request_two")
                )))
        );
        // then - correct expectations removed
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), nullValue());
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_two")), is(expectationTwo));
        // then - activity logged
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("clearing expectations and request logs that match:{}", request("request_one"));
    }

    @Test
    public void shouldClearLogsOnly() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "log")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_two")));

        // when
        httpStateHandler.clear(request);

        // then - correct log entries removed
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_one")))),
                is("")
        );
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_two")))),
                is(httpRequestSerializer.serialize(Collections.singletonList(
                        request("request_two")
                )))
        );
        // then - correct expectations removed
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), is(expectationOne));
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_two")), is(expectationTwo));
        // then - activity logged
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("clearing request logs that match:{}", request("request_one"));
    }

    @Test
    public void shouldClearExpectationsOnly() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "expectations")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_two")));

        // when
        httpStateHandler.clear(request);

        // then - correct log entries removed
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_one")))),
                is(httpRequestSerializer.serialize(Collections.singletonList(
                        request("request_one")
                )))
        );
        assertThat(
                httpStateHandler.retrieve(request().withBody(httpRequestSerializer.serialize(request("request_two")))),
                is(httpRequestSerializer.serialize(Collections.singletonList(
                        request("request_two")
                )))
        );
        // then - correct expectations removed
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), nullValue());
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_two")), is(expectationTwo));
        // then - activity logged
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("clearing expectations that match:{}", request("request_one"));
    }

    @Test
    public void shouldThrowExceptionForInvalidClearType() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("\"invalid\" is not a valid value for \"type\" parameter, only the following values are supported [log, expectations, all]"));

        // when
        httpStateHandler.clear(request().withQueryStringParameter("type", "invalid"));
    }

    @Test
    public void shouldRetrieveRecordedRequestsAsJson() {
        // given - a request
        HttpRequest request = request()
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_two")));
        httpStateHandler.log(new RequestLogEntry(request("request_one")));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpRequestSerializer.serialize(Arrays.asList(
                request("request_one"),
                request("request_one")
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("retrieving requests in json that match:{}", request("request_one"));
    }

    @Test
    public void shouldRetrieveRecordedRequestsAsJava() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("format", "java")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_two")));
        httpStateHandler.log(new RequestLogEntry(request("request_one")));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpRequestToJavaSerializer.serialize(Arrays.asList(
                request("request_one"),
                request("request_one")
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("retrieving requests in java that match:{}", request("request_one"));
    }

    @Test
    public void shouldRetrieveRecordedExpectationsAsJson() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "recorded_expectations")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        Expectation expectationThree = new Expectation(request("request_three")).thenRespond(response("request_three"));
        // given - some log entries
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationOne));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_two"), expectationTwo));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationThree));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpExpectationSerializer.serialize(Arrays.asList(
                expectationOne,
                expectationThree
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("retrieving recorded_expectations in json that match:{}", request("request_one"));
    }

    @Test
    public void shouldRetrieveRecordedExpectationsAsJava() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "recorded_expectations")
                .withQueryStringParameter("format", "java")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        Expectation expectationThree = new Expectation(request("request_three")).thenRespond(response("request_three"));
        // given - some log entries
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationOne));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_two"), expectationTwo));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationThree));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpExpectationToJavaSerializer.serialize(Arrays.asList(
                expectationOne,
                expectationThree
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("retrieving recorded_expectations in java that match:{}", request("request_one"));
    }

    @Test
    public void shouldRetrieveActiveExpectationsAsJson() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "active_expectations")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        Expectation expectationThree = new Expectation(request("request_one")).thenRespond(response("request_three"));
        httpStateHandler.add(expectationThree);
        // given - some log entries
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationOne));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_two"), expectationTwo));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpExpectationSerializer.serialize(Arrays.asList(
                expectationOne,
                expectationThree
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationThree);
        verify(mockLogFormatter).infoLog("retrieving active_expectations in json that match:{}", request("request_one"));
    }

    @Test
    public void shouldRetrieveActiveExpectationsAsJava() {
        // given - a request
        HttpRequest request = request()
                .withQueryStringParameter("type", "active_expectations")
                .withQueryStringParameter("format", "java")
                .withBody(httpRequestSerializer.serialize(request("request_one")));
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        Expectation expectationTwo = new Expectation(request("request_two")).thenRespond(response("response_two"));
        httpStateHandler.add(expectationTwo);
        Expectation expectationThree = new Expectation(request("request_one")).thenRespond(response("request_three"));
        httpStateHandler.add(expectationThree);
        // given - some log entries
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_one"), expectationOne));
        httpStateHandler.log(new ExpectationMatchLogEntry(request("request_two"), expectationTwo));

        // when
        String retrieve = httpStateHandler.retrieve(request);

        // then
        assertThat(retrieve, is(httpExpectationToJavaSerializer.serialize(Arrays.asList(
                expectationOne,
                expectationThree
        ))));
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationTwo);
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationThree);
        verify(mockLogFormatter).infoLog("retrieving active_expectations in java that match:{}", request("request_one"));
    }

    @Test
    public void shouldThrowExceptionForInvalidRetrieveType() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("\"invalid\" is not a valid value for \"type\" parameter, only the following values are supported [requests, recorded_expectations, active_expectations]"));

        // when
        httpStateHandler.retrieve(request().withQueryStringParameter("type", "invalid"));
    }

    @Test
    public void shouldThrowExceptionForInvalidRetrieveFormat() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("\"invalid\" is not a valid value for \"format\" parameter, only the following values are supported [java, json]"));

        // when
        httpStateHandler.retrieve(request().withQueryStringParameter("format", "invalid"));
    }

    @Test
    public void shouldReset() {
        // given - a request
        HttpRequest request = request();
        // given - some existing expectations
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        // given - some log entries
        httpStateHandler.log(new RequestLogEntry(request("request_one")));

        // when
        httpStateHandler.reset();

        // then - correct log entries removed
        assertThat(httpStateHandler.retrieve(request), is(""));
        // then - correct expectations removed
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), nullValue());
        // then - activity logged
        verify(mockLogFormatter).infoLog("creating expectation:{}", expectationOne);
        verify(mockLogFormatter).infoLog("resetting all expectations and request logs");
    }

}