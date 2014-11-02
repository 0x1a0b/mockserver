package org.mockserver.mockserver;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.http.ApacheHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.mappers.MockServerToNettyResponseMapper;
import org.mockserver.mappers.NettyToMockServerRequestMapper;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.MockServerMatcher;
import org.mockserver.mock.action.HttpCallbackActionHandler;
import org.mockserver.mock.action.HttpForwardActionHandler;
import org.mockserver.mock.action.HttpResponseActionHandler;
import org.mockserver.model.*;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.proxy.filters.Filters;
import org.mockserver.proxy.filters.LogFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpCallback.callback;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author jamesdbloom
 */
public class MockServerHandlerTest {

    private MockServerMatcher mockServerMatcher;
    private LogFilter logFilter;
    @Mock
    private Filters filters;
    @Mock
    private ApacheHttpClient apacheHttpClient;
    @Mock
    private NettyToMockServerRequestMapper nettyToMockServerRequestMapper;
    @Mock
    private MockServerToNettyResponseMapper mockServerToNettyResponseMapper;
    @Mock
    private ExpectationSerializer expectationSerializer;
    @Mock
    private HttpRequestSerializer httpRequestSerializer;
    @Mock
    private HttpResponseActionHandler httpResponseActionHandler;
    @Mock
    private HttpForwardActionHandler httpForwardActionHandler;
    @Mock
    private HttpCallbackActionHandler httpCallbackActionHandler;

    @InjectMocks
    private MockServerHandler mockServerHandler;


    @Before
    public void setupFixture() {
        mockServerMatcher = mock(MockServerMatcher.class);
        logFilter = mock(LogFilter.class);
        mockServerHandler = new MockServerHandler(mockServerMatcher, logFilter, mock(MockServer.class), true);

        initMocks(this);
    }

    private NettyHttpRequest createNettyHttpRequest(String uri, HttpMethod method, String some_content) {
        NettyHttpRequest nettyHttpRequest = new NettyHttpRequest(HttpVersion.HTTP_1_1, method, uri, false);
        nettyHttpRequest.content(Unpooled.copiedBuffer(some_content.getBytes()));
        return nettyHttpRequest;
    }

    @Test
    @Ignore("spy function is unreliable and fails the build randomly about 50% of the time")
    public void shouldAddExpectationWithResponse() {
        // given
        HttpResponse httpResponse = new HttpResponse();
        Expectation expectation = spy(new Expectation(new HttpRequest(), once()).thenRespond(httpResponse));
        when(expectationSerializer.deserialize(anyString())).thenReturn(expectation);
        when(mockServerMatcher.when(any(HttpRequest.class), any(Times.class))).thenReturn(expectation);

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/expectation", HttpMethod.PUT, "some_content"));

        // then
        verify(expectationSerializer).deserialize("some_content");
        verify(expectation).thenRespond(same(httpResponse));
        assertThat(response.getStatus(), is(HttpResponseStatus.CREATED));
    }

    @Test
    @Ignore("spy function is unreliable and fails the build randomly about 50% of the time")
    public void shouldAddExpectationWithForward() {
        // given
        HttpForward httpForward = new HttpForward();
        Expectation expectation = spy(new Expectation(new HttpRequest(), once()).thenForward(httpForward));
        when(expectationSerializer.deserialize(anyString())).thenReturn(expectation);
        when(mockServerMatcher.when(any(HttpRequest.class), any(Times.class))).thenReturn(expectation);

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/expectation", HttpMethod.PUT, "some_content"));

        // then
        verify(expectationSerializer).deserialize("some_content");
        verify(expectation).thenForward(same(httpForward));
        assertThat(response.getStatus(), is(HttpResponseStatus.CREATED));
    }

    @Test
    @Ignore("spy function is unreliable and fails the build randomly about 50% of the time")
    public void shouldAddExpectationWithCallback() {
        // given
        HttpCallback httpCallback = new HttpCallback();
        Expectation expectation = spy(new Expectation(new HttpRequest(), once()).thenCallback(httpCallback));
        when(expectationSerializer.deserialize(anyString())).thenReturn(expectation);
        when(mockServerMatcher.when(any(HttpRequest.class), any(Times.class))).thenReturn(expectation);

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/expectation", HttpMethod.PUT, "some_content"));

        // then
        verify(expectationSerializer).deserialize("some_content");
        verify(expectation).thenCallback(same(httpCallback));
        assertThat(response.getStatus(), is(HttpResponseStatus.CREATED));
    }

    @Test
    public void shouldResetExpectations() {
        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/reset", HttpMethod.PUT, "some_content"));

        // then
        verify(logFilter).reset();
        verify(mockServerMatcher).reset();
        assertThat(response.getStatus(), is(HttpResponseStatus.ACCEPTED));
    }

    @Test
    public void shouldClearExpectations() {
        // given
        HttpRequest request = request();
        when(httpRequestSerializer.deserialize(anyString())).thenReturn(request);

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/clear", HttpMethod.PUT, "some_content"));

        // then
        verify(httpRequestSerializer).deserialize("some_content");
        verify(logFilter).clear(same(request));
        verify(mockServerMatcher).clear(same(request));
        assertThat(response.getStatus(), is(HttpResponseStatus.ACCEPTED));
    }

    @Test
    public void shouldDumpExpectationsToLog() {
        // given
        HttpRequest request = request();
        when(httpRequestSerializer.deserialize(anyString())).thenReturn(request);

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/dumpToLog", HttpMethod.PUT, "some_content"));

        // then
        verify(httpRequestSerializer).deserialize("some_content");
        verify(mockServerMatcher).dumpToLog(same(request));
        assertThat(response.getStatus(), is(HttpResponseStatus.ACCEPTED));
    }

    @Test
    public void shouldReturnRecordedRequests() {
        // given
        HttpRequest request = request();
        Expectation[] expectations = new Expectation[0];
        when(httpRequestSerializer.deserialize(anyString())).thenReturn(request);
        when(logFilter.retrieve(any(HttpRequest.class))).thenReturn(expectations);
        when(expectationSerializer.serialize(expectations)).thenReturn("serialized_expectation");

        // when
        FullHttpResponse response = mockServerHandler.mockResponse(createNettyHttpRequest("/retrieve", HttpMethod.PUT, "some_content"));

        // then
        verify(httpRequestSerializer).deserialize("some_content");
        verify(expectationSerializer).serialize(expectations);
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
    }

    @Test
    public void shouldReturnMatchedExpectation() {
        // given
        HttpRequest request = request();
        HttpResponse response = response();
        NettyHttpRequest nettyHttpRequest = createNettyHttpRequest("/some_other_path", HttpMethod.GET, "some_content");
        DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);

        when(nettyToMockServerRequestMapper.mapNettyRequestToMockServerRequest(any(NettyHttpRequest.class))).thenReturn(request);
        when(mockServerMatcher.handle(any(HttpRequest.class))).thenReturn(response);
        when(httpResponseActionHandler.handle(any(HttpResponse.class), any(HttpRequest.class))).thenReturn(response);
        when(mockServerToNettyResponseMapper.mapMockServerResponseToNettyResponse(response)).thenReturn(defaultFullHttpResponse);

        // when
        FullHttpResponse result = mockServerHandler.mockResponse(nettyHttpRequest);

        // then
        verify(nettyToMockServerRequestMapper).mapNettyRequestToMockServerRequest(nettyHttpRequest);
        verify(httpResponseActionHandler).handle(response, request);
        assertThat(result.getStatus(), is(HttpResponseStatus.NO_CONTENT));
    }

    @Test
    public void shouldForwardMatchedExpectation() {
        // given
        HttpRequest request = request();
        HttpResponse response = response();
        HttpForward forward = forward();
        NettyHttpRequest nettyHttpRequest = createNettyHttpRequest("/some_other_path", HttpMethod.GET, "some_content");
        DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);

        when(nettyToMockServerRequestMapper.mapNettyRequestToMockServerRequest(any(NettyHttpRequest.class))).thenReturn(request);
        when(mockServerMatcher.handle(any(HttpRequest.class))).thenReturn(forward);
        when(httpForwardActionHandler.handle(any(HttpForward.class), any(HttpRequest.class))).thenReturn(response);
        when(mockServerToNettyResponseMapper.mapMockServerResponseToNettyResponse(response)).thenReturn(defaultFullHttpResponse);

        // when
        FullHttpResponse result = mockServerHandler.mockResponse(nettyHttpRequest);

        // then                                                   \
        verify(nettyToMockServerRequestMapper).mapNettyRequestToMockServerRequest(nettyHttpRequest);
        verify(mockServerMatcher).handle(request);
        verify(httpForwardActionHandler).handle(forward, request);
        assertThat(result.getStatus(), is(HttpResponseStatus.NO_CONTENT));
    }

    @Test
    public void shouldCallbackMatchedExpectation() {
        // given
        HttpRequest request = request();
        HttpResponse response = response();
        HttpCallback callback = callback();
        NettyHttpRequest nettyHttpRequest = createNettyHttpRequest("/some_other_path", HttpMethod.GET, "some_content");
        DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);

        when(nettyToMockServerRequestMapper.mapNettyRequestToMockServerRequest(any(NettyHttpRequest.class))).thenReturn(request);
        when(mockServerMatcher.handle(any(HttpRequest.class))).thenReturn(callback);
        when(httpCallbackActionHandler.handle(any(HttpCallback.class), any(HttpRequest.class))).thenReturn(response);
        when(mockServerToNettyResponseMapper.mapMockServerResponseToNettyResponse(response)).thenReturn(defaultFullHttpResponse);

        // when
        FullHttpResponse result = mockServerHandler.mockResponse(nettyHttpRequest);

        // then                                                   \
        verify(nettyToMockServerRequestMapper).mapNettyRequestToMockServerRequest(nettyHttpRequest);
        verify(mockServerMatcher).handle(request);
        verify(httpCallbackActionHandler).handle(callback, request);
        assertThat(result.getStatus(), is(HttpResponseStatus.NO_CONTENT));
    }

    @Test
    public void shouldReturnNotFound() {
        // given
        HttpRequest request = request();
        HttpResponse response = response();
        NettyHttpRequest nettyHttpRequest = createNettyHttpRequest("/some_other_path", HttpMethod.GET, "some_content");
        DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

        when(nettyToMockServerRequestMapper.mapNettyRequestToMockServerRequest(any(NettyHttpRequest.class))).thenReturn(request);
        when(mockServerMatcher.handle(any(HttpRequest.class))).thenReturn(null);
        when(httpResponseActionHandler.handle((HttpResponse) isNull(), any(HttpRequest.class))).thenReturn(response);
        when(mockServerToNettyResponseMapper.mapMockServerResponseToNettyResponse(response)).thenReturn(defaultFullHttpResponse);

        // when
        FullHttpResponse result = mockServerHandler.mockResponse(nettyHttpRequest);

        // then
        verify(mockServerMatcher).handle(request);
        assertThat(result.getStatus(), is(HttpResponseStatus.NOT_FOUND));
    }
}
