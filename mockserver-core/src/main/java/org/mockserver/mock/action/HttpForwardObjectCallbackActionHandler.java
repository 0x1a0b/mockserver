package org.mockserver.mock.action;

import org.mockserver.callback.WebSocketClientRegistry;
import org.mockserver.callback.WebSocketRequestCallback;
import org.mockserver.client.NettyHttpClient;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.model.HttpObjectCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpRequestAndHttpResponse;
import org.mockserver.model.HttpResponse;
import org.mockserver.responsewriter.ResponseWriter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.mockserver.callback.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.slf4j.event.Level.*;

/**
 * @author jamesdbloom
 */
public class HttpForwardObjectCallbackActionHandler extends HttpForwardAction {
    private WebSocketClientRegistry webSocketClientRegistry;

    public HttpForwardObjectCallbackActionHandler(HttpStateHandler httpStateHandler, NettyHttpClient httpClient) {
        super(httpStateHandler.getMockServerLogger(), httpClient);
        this.webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
    }

    public void handle(final ActionHandler actionHandler, final HttpObjectCallback httpObjectCallback, final HttpRequest request, final ResponseWriter responseWriter, final boolean synchronous, Runnable expectationPostProcessor) {
        final String clientId = httpObjectCallback.getClientId();
        final String webSocketCorrelationId = UUID.randomUUID().toString();
        webSocketClientRegistry.registerForwardCallbackHandler(webSocketCorrelationId, new WebSocketRequestCallback() {
            @Override
            public void handle(final HttpRequest request) {
                if (MockServerLogger.isEnabled(TRACE)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(LogEntry.LogMessageType.TRACE)
                            .setLogLevel(TRACE)
                            .setHttpRequest(request)
                            .setMessageFormat("received request over websocket{}from client " + clientId + " for correlationId " + webSocketCorrelationId)
                            .setArguments(request)
                    );
                }
                final HttpForwardActionResult responseFuture = sendRequest(
                    request.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME),
                    null,
                    null
                );
                if (MockServerLogger.isEnabled(TRACE)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(LogEntry.LogMessageType.TRACE)
                            .setLogLevel(TRACE)
                            .setHttpRequest(request)
                            .setMessageFormat("received response for request{}from client " + clientId)
                            .setArguments(request)
                    );
                }
                webSocketClientRegistry.unregisterForwardCallbackHandler(webSocketCorrelationId);
                if (expectationPostProcessor != null && isFalse(httpObjectCallback.getResponseCallback())) {
                    expectationPostProcessor.run();
                }
                if (isTrue(httpObjectCallback.getResponseCallback())) {
                    actionHandler.executeAfterForwardActionResponse(responseFuture, response -> {
                        // register callback for overridden response
                        CompletableFuture<HttpResponse> httpResponseCompletableFuture = new CompletableFuture<>();
                        webSocketClientRegistry.registerResponseCallbackHandler(webSocketCorrelationId, overriddenResponse -> {
                            if (MockServerLogger.isEnabled(TRACE)) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setType(LogEntry.LogMessageType.TRACE)
                                        .setLogLevel(TRACE)
                                        .setHttpRequest(request)
                                        .setMessageFormat("received response over websocket{}for request and response{}from client " + clientId + " for correlationId " + webSocketCorrelationId)
                                        .setArguments(
                                            overriddenResponse,
                                            new HttpRequestAndHttpResponse()
                                                .withHttpRequest(request)
                                                .withHttpResponse(response)
                                        )
                                );
                            }
                            webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
                            if (expectationPostProcessor != null) {
                                expectationPostProcessor.run();
                            }
                            httpResponseCompletableFuture.complete(overriddenResponse.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME));
                        });
                        // send websocket message to override response
                        if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), response)) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setType(LogEntry.LogMessageType.WARN)
                                    .setLogLevel(WARN)
                                    .setHttpRequest(request)
                                    .setMessageFormat("returning{}because client " + clientId + " has closed web socket connection")
                                    .setArguments(notFoundResponse())
                            );
                            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
                        } else if (MockServerLogger.isEnabled(TRACE)) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setType(LogEntry.LogMessageType.TRACE)
                                    .setLogLevel(TRACE)
                                    .setHttpRequest(request)
                                    .setMessageFormat("sending response over websocket{}to client " + clientId + " for correlationId " + webSocketCorrelationId)
                                    .setArguments(response)
                            );
                        }
                        // return overridden response
                        actionHandler.writeForwardActionResponse(responseFuture.setHttpResponse(httpResponseCompletableFuture), responseWriter, request, httpObjectCallback, synchronous);
                    }, synchronous);
                } else {
                    actionHandler.writeForwardActionResponse(responseFuture, responseWriter, request, httpObjectCallback, synchronous);
                }
            }

            @Override
            public void handleError(HttpResponse httpResponse) {
                if (MockServerLogger.isEnabled(DEBUG)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(LogEntry.LogMessageType.DEBUG)
                            .setLogLevel(DEBUG)
                            .setHttpRequest(request)
                            .setMessageFormat("error sending request over websocket for client " + clientId + " for correlationId " + webSocketCorrelationId)
                    );
                }
                webSocketClientRegistry.unregisterForwardCallbackHandler(webSocketCorrelationId);
                actionHandler.writeResponseActionResponse(httpResponse, responseWriter, request, httpObjectCallback, synchronous);
            }
        });
        if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), null)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(LogEntry.LogMessageType.WARN)
                    .setLogLevel(WARN)
                    .setHttpRequest(request)
                    .setMessageFormat("returning{}because client " + clientId + " has closed web socket connection")
                    .setArguments(notFoundResponse())
            );
            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
        } else if (MockServerLogger.isEnabled(TRACE)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(LogEntry.LogMessageType.TRACE)
                    .setLogLevel(TRACE)
                    .setHttpRequest(request)
                    .setMessageFormat("sending request over websocket{}to client " + clientId + " for correlationId " + webSocketCorrelationId)
                    .setArguments(request)
            );
        }
    }

}
