package org.mockserver.proxy;

import org.mockserver.client.http.ApacheHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.client.serialization.VerificationSerializer;
import org.mockserver.filters.*;
import org.mockserver.mappers.HttpServletToMockServerRequestMapper;
import org.mockserver.mappers.MockServerToHttpServletResponseMapper;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.streams.IOStreamUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jamesdbloom
 */
public class ProxyServlet extends HttpServlet {
    private static final long serialVersionUID = 8490389904399790169L;
    // mockserver
    private Filters filters = new Filters();
    private LogFilter logFilter = new LogFilter();
    private ApacheHttpClient apacheHttpClient = new ApacheHttpClient(true);
    // mappers
    private HttpServletToMockServerRequestMapper httpServletToMockServerRequestMapper = new HttpServletToMockServerRequestMapper();
    private MockServerToHttpServletResponseMapper mockServerToHttpServletResponseMapper = new MockServerToHttpServletResponseMapper();
    // serializers
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer();
    private ExpectationSerializer expectationSerializer = new ExpectationSerializer();
    private VerificationSerializer verificationSerializer = new VerificationSerializer();

    public ProxyServlet() {
        filters.withFilter(new HttpRequest(), new HopByHopHeaderFilter());
        filters.withFilter(new HttpRequest(), logFilter);
    }

    /**
     * Add filter for HTTP requests, each filter get called before each request is proxied, if the filter return null then the request is not proxied
     *
     * @param httpRequest the request to match against for this filter
     * @param filter the filter to execute for this request, if the filter returns null the request will not be proxied
     */
    public ProxyServlet withFilter(HttpRequest httpRequest, RequestFilter filter) {
        filters.withFilter(httpRequest, filter);
        return this;
    }

    /**
     * Add filter for HTTP response, each filter get called after each request has been proxied
     *
     * @param httpRequest the request to match against for this filter
     * @param filter the filter that is executed after this request has been proxied
     */
    public ProxyServlet withFilter(HttpRequest httpRequest, ResponseFilter filter) {
        filters.withFilter(httpRequest, filter);
        return this;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String requestPath = httpServletRequest.getPathInfo() != null && httpServletRequest.getContextPath() != null ? httpServletRequest.getPathInfo() : httpServletRequest.getRequestURI();
        if (requestPath.equals("/stop")) {
            httpServletResponse.setStatus(HttpStatusCode.NOT_IMPLEMENTED_501.code());
        } else if (requestPath.equals("/dumpToLog")) {
            logFilter.dumpToLog(httpRequestSerializer.deserialize(IOStreamUtils.readInputStreamToString(httpServletRequest)), "java".equals(httpServletRequest.getParameter("type")));
            httpServletResponse.setStatus(HttpStatusCode.ACCEPTED_202.code());
        } else if (requestPath.equals("/retrieve")) {
            Expectation[] expectations = logFilter.retrieve(httpRequestSerializer.deserialize(IOStreamUtils.readInputStreamToString(httpServletRequest)));
            IOStreamUtils.writeToOutputStream(expectationSerializer.serialize(expectations).getBytes(), httpServletResponse);
            httpServletResponse.setStatus(HttpStatusCode.OK_200.code());
        } else if (requestPath.equals("/reset")) {
            logFilter.reset();
            httpServletResponse.setStatus(HttpStatusCode.ACCEPTED_202.code());
        } else if (requestPath.equals("/clear")) {
            logFilter.clear(httpRequestSerializer.deserialize(IOStreamUtils.readInputStreamToString(httpServletRequest)));
            httpServletResponse.setStatus(HttpStatusCode.ACCEPTED_202.code());
        } else if (requestPath.equals("/verify")) {
            String result = logFilter.verify(verificationSerializer.deserialize(IOStreamUtils.readInputStreamToString(httpServletRequest)));
            if (result.isEmpty()) {
                httpServletResponse.setStatus(HttpStatusCode.ACCEPTED_202.code());
            } else {
                IOStreamUtils.writeToOutputStream(result.getBytes(), httpServletResponse);
                httpServletResponse.setStatus(HttpStatusCode.NOT_ACCEPTABLE_406.code());
            }
        } else {
            forwardRequest(httpServletRequest, httpServletResponse);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) {
        forwardRequest(request, response);
    }

    private void forwardRequest(HttpServletRequest request, HttpServletResponse response) {
        sendRequest(filters.applyOnRequestFilters(httpServletToMockServerRequestMapper.mapHttpServletRequestToMockServerRequest(request)), response);
    }

    private void sendRequest(final HttpRequest httpRequest, final HttpServletResponse httpServletResponse) {
        // if HttpRequest was set to null by a filter don't send request
        if (httpRequest != null) {
            HttpResponse httpResponse = filters.applyOnResponseFilters(httpRequest, apacheHttpClient.sendRequest(httpRequest, false));
            mockServerToHttpServletResponseMapper.mapMockServerResponseToHttpServletResponse(httpResponse, httpServletResponse);
        }
    }
}
