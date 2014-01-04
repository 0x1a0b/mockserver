package org.mockserver.proxy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.http.HttpRequestClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.mappers.HttpServletRequestMapper;
import org.mockserver.mappers.HttpServletResponseMapper;
import org.mockserver.mock.Expectation;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.proxy.filters.LogFilter;
import org.mockserver.proxy.filters.ProxyRequestFilter;
import org.mockserver.proxy.filters.ProxyResponseFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProxyServletTest {

    @Mock
    private HttpServletRequestMapper httpServletRequestMapper;
    @Mock
    private HttpServletResponseMapper httpServletResponseMapper;
    @Mock
    private HttpRequestClient httpRequestClient;
    @Mock
    private LogFilter logFilter;
    @Mock
    private HttpRequestSerializer httpRequestSerializer;
    @Mock
    private ExpectationSerializer expectationSerializer;
    @InjectMocks
    private ProxyServlet proxyServlet;
    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private ArgumentCaptor<HttpRequest> httpRequestArgumentCaptor;

    @Before
    public void setupMocks() {
        // create mocks
        proxyServlet = new ProxyServlet();
        initMocks(this);

        // additional mock objects
        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletResponse = new MockHttpServletResponse();
        httpRequest = new HttpRequest().withPath("some_path");
        httpResponse = new HttpResponse();

        // mappers
        when(httpServletRequestMapper.mapHttpServletRequestToHttpRequest(any(MockHttpServletRequest.class))).thenReturn(httpRequest);
        httpRequestArgumentCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(httpRequestClient.sendRequest(httpRequestArgumentCaptor.capture())).thenReturn(httpResponse);
    }

    @Test
    public void shouldProxyGETRequest() throws Exception {
        // when
        proxyServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyHEADRequest() throws Exception {
        // when
        proxyServlet.doHead(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyPOSTRequest() throws Exception {
        // when
        proxyServlet.doPost(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyPUTRequest() throws Exception {
        // when
        proxyServlet.doPut(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyDELETERequest() throws Exception {
        // when
        proxyServlet.doDelete(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyOPTIONSRequest() throws Exception {
        // when
        proxyServlet.doOptions(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldProxyTRACERequest() throws Exception {
        // when
        proxyServlet.doTrace(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(httpServletRequestMapper).mapHttpServletRequestToHttpRequest(same(mockHttpServletRequest));
        verify(httpServletResponseMapper).mapHttpResponseToHttpServletResponse(same(httpResponse), same(mockHttpServletResponse));
        verify(httpRequestClient).sendRequest(same(httpRequest));
    }

    @Test
    public void shouldCallMatchingFiltersBeforeForwardingRequest() throws Exception {
        // given
        // - add first filter
        ProxyRequestFilter filter = mock(ProxyRequestFilter.class);
        proxyServlet.withFilter(httpRequest, filter);
        // - add first filter with other request
        HttpRequest someOtherRequest = new HttpRequest().withPath("some_other_path");
        proxyServlet.withFilter(someOtherRequest, filter);
        // - add second filter
        ProxyRequestFilter someOtherFilter = mock(ProxyRequestFilter.class);
        proxyServlet.withFilter(someOtherRequest, someOtherFilter);

        // when
        proxyServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(filter, times(1)).onRequest(same(httpRequest));
        verify(filter, times(0)).onRequest(same(someOtherRequest));
        verifyZeroInteractions(someOtherFilter);
    }

    @Test
    public void shouldApplyFiltersBeforeAndAfterRequest() throws Exception {
        // given
        // - add first filter
        ProxyResponseFilter filter = mock(ProxyResponseFilter.class);
        when(filter.onResponse(any(HttpRequest.class), any(HttpResponse.class))).thenReturn(new HttpResponse());
        proxyServlet.withFilter(httpRequest, filter);
        // - add first filter with other request
        HttpRequest someOtherRequest = new HttpRequest().withPath("some_other_path");
        proxyServlet.withFilter(someOtherRequest, filter);
        // - add second filter
        ProxyResponseFilter someOtherFilter = mock(ProxyResponseFilter.class);
        proxyServlet.withFilter(someOtherRequest, someOtherFilter);

        // when
        proxyServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(filter, times(1)).onResponse(same(httpRequest), same(httpResponse));
        verify(filter, times(0)).onResponse(same(someOtherRequest), same(httpResponse));
        verifyZeroInteractions(someOtherFilter);
    }

    @Test
    public void shouldNotForwardHopByHopHeaders() throws Exception {
        // given
        httpRequest.withHeaders(
                new Header("some_other_header"),
                new Header("proxy-connection"),
                new Header("connection"),
                new Header("keep-alive"),
                new Header("transfer-encoding"),
                new Header("te"),
                new Header("trailer"),
                new Header("proxy-authorization"),
                new Header("proxy-authenticate"),
                new Header("upgrade")
        );

        // when
        proxyServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);

        // then
        HttpRequest actual = httpRequestArgumentCaptor.getValue();
        assertEquals(actual.getHeaders().size(), 1);
    }

    /*
    httpServletRequest.getPathInfo() != null && httpServletRequest.getContextPath() != null ? httpServletRequest.getPathInfo() : httpServletRequest.getRequestURI()
     */

    @Test
    public void shouldDumpToLog() throws Exception {
        // given
        mockHttpServletRequest.setRequestURI("/dumpToLog");
        mockHttpServletRequest.setContent("body".getBytes());
        when(httpRequestSerializer.deserialize("body".getBytes())).thenReturn(httpRequest);

        // when
        proxyServlet.doPut(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(logFilter).dumpToLog(httpRequest);
        assertEquals(HttpStatusCode.ACCEPTED_202.code(), mockHttpServletResponse.getStatus());
    }

    @Test
    public void shouldRetrieve() throws Exception {
        // given
        Expectation[] expectations = new Expectation[]{};
        mockHttpServletRequest.setRequestURI("/retrieve");
        mockHttpServletRequest.setContent("body".getBytes());
        when(httpRequestSerializer.deserialize("body".getBytes())).thenReturn(httpRequest);
        when(logFilter.retrieve(httpRequest)).thenReturn(expectations);
        when(expectationSerializer.serialize(aryEq(new Expectation[]{}))).thenReturn("expectationsArray");

        // when
        proxyServlet.doPut(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(logFilter).retrieve(httpRequest);
        assertEquals(HttpStatusCode.OK_200.code(), mockHttpServletResponse.getStatus());
        assertEquals("expectationsArray", mockHttpServletResponse.getContentAsString());
    }

    @Test
    public void shouldReset() throws Exception {
        // given
        mockHttpServletRequest.setRequestURI("/reset");

        // when
        proxyServlet.doPut(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(logFilter).reset();
        assertEquals(HttpStatusCode.ACCEPTED_202.code(), mockHttpServletResponse.getStatus());
    }

    @Test
    public void shouldClear() throws Exception {
        // given
        mockHttpServletRequest.setRequestURI("/clear");
        mockHttpServletRequest.setContent("body".getBytes());
        when(httpRequestSerializer.deserialize("body".getBytes())).thenReturn(httpRequest);

        // when
        proxyServlet.doPut(mockHttpServletRequest, mockHttpServletResponse);

        // then
        verify(logFilter).clear(httpRequest);
        assertEquals(HttpStatusCode.ACCEPTED_202.code(), mockHttpServletResponse.getStatus());
    }
}
