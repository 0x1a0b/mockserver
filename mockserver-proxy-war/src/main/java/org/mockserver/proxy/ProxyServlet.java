package org.mockserver.proxy;

import com.google.common.net.MediaType;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.client.serialization.PortBindingSerializer;
import org.mockserver.client.serialization.curl.HttpRequestToCurlSerializer;
import org.mockserver.filters.HopByHopHeaderFilter;
import org.mockserver.log.model.RequestResponseLogEntry;
import org.mockserver.logging.LoggingFormatter;
import org.mockserver.mappers.HttpServletRequestToMockServerRequestDecoder;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.responsewriter.ResponseWriter;
import org.mockserver.server.ServletResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.NOT_IMPLEMENTED;
import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.PortBinding.portBinding;

/**
 * @author jamesdbloom
 */
public class ProxyServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    // generic handling
    private HttpStateHandler httpStateHandler;
    // serializers
    private PortBindingSerializer portBindingSerializer = new PortBindingSerializer();
    // mappers
    private HttpServletRequestToMockServerRequestDecoder httpServletRequestToMockServerRequestDecoder = new HttpServletRequestToMockServerRequestDecoder();
    // forwarding
    private NettyHttpClient httpClient = new NettyHttpClient();
    private HopByHopHeaderFilter hopByHopHeaderFilter = new HopByHopHeaderFilter();
    private LoggingFormatter logFormatter = new LoggingFormatter(logger);
    private HttpRequestToCurlSerializer httpRequestToCurlSerializer = new HttpRequestToCurlSerializer();

    public ProxyServlet() {
        this.httpStateHandler = new HttpStateHandler();
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        ResponseWriter responseWriter = new ServletResponseWriter(httpServletResponse);
        HttpRequest request = null;
        try {

            request = httpServletRequestToMockServerRequestDecoder.mapHttpServletRequestToMockServerRequest(httpServletRequest);
            if (!httpStateHandler.handle(request, responseWriter, true)) {

                if (request.matches("PUT", "/status")) {

                    responseWriter.writeResponse(request, OK, portBindingSerializer.serialize(portBinding(httpServletRequest.getLocalPort())), "application/json");

                } else if (request.matches("PUT", "/bind")) {

                    responseWriter.writeResponse(request, NOT_IMPLEMENTED);

                } else if (request.matches("PUT", "/stop")) {

                    responseWriter.writeResponse(request, NOT_IMPLEMENTED);

                } else {

                    HttpResponse response = httpClient.sendRequest(hopByHopHeaderFilter.onRequest(request));
                    if (response == null) {
                        response = notFoundResponse();
                    }
                    responseWriter.writeResponse(request, response);
                    httpStateHandler.log(new RequestResponseLogEntry(request, response));
                    logFormatter.infoLog(
                            "returning response:{}" + NEW_LINE + " for request as json:{}" + NEW_LINE + " as curl:{}",
                            response,
                            request,
                            httpRequestToCurlSerializer.toCurl(request)
                    );

                }
            }
        } catch (IllegalArgumentException iae) {
            logger.error("Exception processing " + request, iae);
            // send request without API CORS headers
            responseWriter.writeResponse(request, BAD_REQUEST, iae.getMessage(), MediaType.create("text", "plain").toString());
        } catch (Exception e) {
            logger.error("Exception processing " + request, e);
            responseWriter.writeResponse(request, response().withStatusCode(BAD_REQUEST.code()).withBody(e.getMessage()));
        }
    }

}
