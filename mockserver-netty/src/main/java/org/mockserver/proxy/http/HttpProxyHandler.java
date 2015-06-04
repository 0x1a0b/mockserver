package org.mockserver.proxy.http;

import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.client.serialization.VerificationSequenceSerializer;
import org.mockserver.client.serialization.VerificationSerializer;
import org.mockserver.filters.Filters;
import org.mockserver.filters.HopByHopHeaderFilter;
import org.mockserver.filters.LogFilter;
import org.mockserver.logging.LogFormatter;
import org.mockserver.mappers.ContentTypeMapper;
import org.mockserver.mock.Expectation;
import org.mockserver.model.Body;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.proxy.Proxy;
import org.mockserver.proxy.connect.HttpConnectHandler;
import org.mockserver.proxy.unification.PortUnificationHandler;
import org.mockserver.socket.SSLFactory;
import org.mockserver.verify.Verification;
import org.mockserver.verify.VerificationSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.OutboundHttpRequest.outboundRequest;

@ChannelHandler.Sharable
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // mockserver
    private final Proxy server;
    private final LogFilter logFilter;
    private final Filters filters = new Filters();
    private LogFormatter logFormatter = new LogFormatter(logger);
    // http client
    private NettyHttpClient httpClient = new NettyHttpClient();
    // serializers
    private ExpectationSerializer expectationSerializer = new ExpectationSerializer();
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer();
    private VerificationSerializer verificationSerializer = new VerificationSerializer();
    private VerificationSequenceSerializer verificationSequenceSerializer = new VerificationSequenceSerializer();

    public HttpProxyHandler(Proxy server, LogFilter logFilter) {
        super(false);
        this.server = server;
        this.logFilter = logFilter;
        filters.withFilter(new org.mockserver.model.HttpRequest(), new HopByHopHeaderFilter());
        filters.withFilter(new org.mockserver.model.HttpRequest(), logFilter);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {

        try {

            logFormatter.traceLog("received request:{}" + System.getProperty("line.separator"), request);

            if (request.getMethod().getValue().equals("CONNECT")) {

                // assume CONNECT always for SSL
                PortUnificationHandler.enabledSslUpstreamAndDownstream(ctx.channel());
                // add Subject Alternative Name for SSL certificate
                SSLFactory.addSubjectAlternativeName(request.getPath().getValue());
                ctx.pipeline().addLast(new HttpConnectHandler());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(request);

            } else if (request.matches("PUT", "/status")) {

                writeResponse(ctx, request, HttpResponseStatus.OK);

            } else if (request.matches("PUT", "/clear")) {

                org.mockserver.model.HttpRequest httpRequest = httpRequestSerializer.deserialize(request.getBodyAsString());
                logFilter.clear(httpRequest);
                logFormatter.infoLog("clearing expectations and request logs that match:{}", httpRequest);
                writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);

            } else if (request.matches("PUT", "/reset")) {

                logFilter.reset();
                logFormatter.infoLog("resetting all expectations and request logs");
                writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);

            } else if (request.matches("PUT", "/dumpToLog")) {

                logFilter.dumpToLog(httpRequestSerializer.deserialize(request.getBodyAsString()), request.hasQueryStringParameter("type", "java"));
                writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);

            } else if (request.matches("PUT", "/retrieve")) {

                Expectation[] expectations = logFilter.retrieve(httpRequestSerializer.deserialize(request.getBodyAsString()));
                writeResponse(ctx, request, HttpResponseStatus.OK, expectationSerializer.serialize(expectations), "application/json");

            } else if (request.matches("PUT", "/verify")) {

                Verification verification = verificationSerializer.deserialize(request.getBodyAsString());
                logFormatter.infoLog("verifying:{}", verification);
                String result = logFilter.verify(verification);
                if (result.isEmpty()) {
                    writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);
                } else {
                    writeResponse(ctx, request, HttpResponseStatus.NOT_ACCEPTABLE, result, MediaType.create("text", "plain").toString());
                }

            } else if (request.matches("PUT", "/verifySequence")) {

                VerificationSequence verificationSequence = verificationSequenceSerializer.deserialize(request.getBodyAsString());
                String result = logFilter.verify(verificationSequence);
                logFormatter.infoLog("verifying sequence:{}", verificationSequence);
                if (result.isEmpty()) {
                    writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);
                } else {
                    writeResponse(ctx, request, HttpResponseStatus.NOT_ACCEPTABLE, result, MediaType.create("text", "plain").toString());
                }

            } else if (request.matches("PUT", "/stop")) {

                writeResponse(ctx, request, HttpResponseStatus.ACCEPTED);
                ctx.flush();
                ctx.close();
                server.stop();

            } else {

                HttpResponse response = sendRequest(ctx.channel(), filters.applyOnRequestFilters(request));
                logFormatter.infoLog("returning response:{}" + System.getProperty("line.separator") + " for request:{}", response, request);
                writeResponse(ctx, request, response);

            }
        } catch (Exception e) {
            logger.error("Exception processing " + request, e);
            writeResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
        }

    }

    private HttpResponse sendRequest(Channel channel, HttpRequest httpRequest) {
        // if HttpRequest was set to null by a filter don't send request
        if (httpRequest != null) {


            InetSocketAddress inetSocketAddress = channel.attr(HttpProxy.REMOTE_SOCKET).get();

            // read remote socket from channel attribute to direct proxy (port forwarding)
            if (inetSocketAddress != null) {
                if (inetSocketAddress.getPort() == 443 || inetSocketAddress.getPort() == 8443) {
                    httpRequest.setSecure(true);
                }
            } else if (!Strings.isNullOrEmpty(httpRequest.getFirstHeader("Host"))) {
                // read remote socket from host header for HTTP proxy
                String[] hostHeaderParts = httpRequest.getFirstHeader("Host").split(":");

                Integer port = (httpRequest.isSecure() ? 443 : 80); // default
                if (hostHeaderParts.length > 1) {
                    port = Integer.parseInt(hostHeaderParts[1]);  // non-default
                }

                // add Subject Alternative Name for SSL certificate (just in case this hasn't been added before)
                SSLFactory.addSubjectAlternativeName(hostHeaderParts[0]);

                inetSocketAddress = new InetSocketAddress(hostHeaderParts[0], port);
            } else {
                logger.error("Host header must be provided for requests being forwarded, the following request does not include the \"Host\" header:" + System.getProperty("line.separator") + httpRequest);
                return notFoundResponse();
            }

            HttpResponse httpResponse = filters.applyOnResponseFilters(httpRequest, httpClient.sendRequest(outboundRequest(inetSocketAddress, "", httpRequest)));
            if (httpResponse != null) {
                return httpResponse;
            }
        }
        return notFoundResponse();
    }

    private void writeResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus responseStatus) {
        writeResponse(ctx, request, responseStatus, "", "application/json");
    }

    private void writeResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus responseStatus, String body, String contentType) {
        writeResponse(ctx, request,
                response()
                        .withStatusCode(responseStatus.code())
                        .withBody(body)
                        .updateHeader(header(HttpHeaders.Names.CONTENT_TYPE, contentType + "; charset=utf-8"))
        );
    }

    private void writeResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response) {
        addContentLengthHeader(response);
        if (request.isKeepAlive()) {
            response.updateHeader(header(CONNECTION, HttpHeaders.Values.KEEP_ALIVE));
            ctx.write(response);
        } else {
            response.updateHeader(header(CONNECTION, HttpHeaders.Values.CLOSE));
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void addContentLengthHeader(HttpResponse response) {
        Body body = response.getBody();
        byte[] bodyBytes = new byte[0];
        if (body != null) {
            Object bodyContents = body.getValue();
            Charset bodyCharset = body.getCharset(ContentTypeMapper.determineCharsetForMessage(response));
            if (bodyContents instanceof byte[]) {
                bodyBytes = (byte[]) bodyContents;
            } else if (bodyContents instanceof String) {
                bodyBytes = ((String) bodyContents).getBytes(bodyCharset);
            } else if (body.toString() != null) {
                bodyBytes = body.toString().getBytes(bodyCharset);
            }
        }
        response.updateHeader(header(CONTENT_LENGTH, bodyBytes.length));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!cause.getMessage().contains("Connection reset by peer")) {
            logger.warn("Exception caught by MockServer handler closing pipeline", cause);
        }
        ctx.close();
    }
}
