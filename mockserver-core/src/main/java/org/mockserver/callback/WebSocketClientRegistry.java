package org.mockserver.callback;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.mockserver.collections.CircularHashMap;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.metrics.Metrics;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpRequestAndHttpResponse;
import org.mockserver.model.HttpResponse;
import org.mockserver.serialization.WebSocketMessageSerializer;
import org.mockserver.serialization.model.WebSocketClientIdDTO;
import org.mockserver.serialization.model.WebSocketErrorDTO;
import org.mockserver.websocket.WebSocketException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.mockserver.configuration.ConfigurationProperties.maxWebSocketExpectations;
import static org.mockserver.metrics.Metrics.Name.*;
import static org.mockserver.metrics.Metrics.clearWebSocketMetrics;
import static org.mockserver.model.HttpResponse.response;
import static org.slf4j.event.Level.WARN;

/**
 * @author jamesdbloom
 */
public class WebSocketClientRegistry {

    public static final String WEB_SOCKET_CORRELATION_ID_HEADER_NAME = "WebSocketCorrelationId";
    private final MockServerLogger mockServerLogger;
    private final WebSocketMessageSerializer webSocketMessageSerializer;
    private final Map<String, ChannelHandlerContext> clientRegistry = Collections.synchronizedMap(new CircularHashMap<>(maxWebSocketExpectations()));
    private final Map<String, WebSocketResponseCallback> responseCallbackRegistry = new CircularHashMap<>(maxWebSocketExpectations());
    private final Map<String, WebSocketRequestCallback> forwardCallbackRegistry = new CircularHashMap<>(maxWebSocketExpectations());
    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

    public WebSocketClientRegistry(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        this.webSocketMessageSerializer = new WebSocketMessageSerializer(mockServerLogger);
    }

    void receivedTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        try {
            Object deserializedMessage = webSocketMessageSerializer.deserialize(textWebSocketFrame.text());
            if (deserializedMessage instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) deserializedMessage;
                String firstHeader = httpResponse.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketResponseCallback webSocketResponseCallback = responseCallbackRegistry.get(firstHeader);
                if (webSocketResponseCallback != null) {
                    webSocketResponseCallback.handle(httpResponse);
                }
            } else if (deserializedMessage instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) deserializedMessage;
                final String firstHeader = httpRequest.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketRequestCallback webSocketRequestCallback = forwardCallbackRegistry.get(firstHeader);
                if (webSocketRequestCallback != null) {
                    webSocketRequestCallback.handle(httpRequest);
                }
            } else if (deserializedMessage instanceof WebSocketErrorDTO) {
                WebSocketErrorDTO webSocketErrorDTO = (WebSocketErrorDTO) deserializedMessage;
                if (forwardCallbackRegistry.containsKey(webSocketErrorDTO.getWebSocketCorrelationId())) {
                    forwardCallbackRegistry
                        .get(webSocketErrorDTO.getWebSocketCorrelationId())
                        .handleError(
                            response()
                                .withStatusCode(404)
                                .withBody(webSocketErrorDTO.getMessage())
                        );
                } else if (responseCallbackRegistry.containsKey(webSocketErrorDTO.getWebSocketCorrelationId())) {
                    responseCallbackRegistry
                        .get(webSocketErrorDTO.getWebSocketCorrelationId())
                        .handle(
                            response()
                                .withStatusCode(404)
                                .withBody(webSocketErrorDTO.getMessage())
                        );
                }
            } else {
                throw new WebSocketException("Unsupported web socket message " + deserializedMessage);
            }
        } catch (Exception e) {
            throw new WebSocketException("Exception while receiving web socket message" + textWebSocketFrame.text(), e);
        }
    }

    void registerClient(String clientId, ChannelHandlerContext ctx) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            try {
                ctx.writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(new WebSocketClientIdDTO().setClientId(clientId))));
            } catch (Exception e) {
                throw new WebSocketException("Exception while sending web socket registration client id message to client " + clientId, e);
            }
            clientRegistry.put(clientId, ctx);
            Metrics.set(WEBSOCKET_CALLBACK_CLIENT_COUNT, clientRegistry.size());
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public void unregisterClient(String clientId) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            ChannelHandlerContext removeChannel = clientRegistry.remove(clientId);
            if (removeChannel != null && removeChannel.channel().isOpen()) {
                removeChannel.channel().close();
            }
            Metrics.set(WEBSOCKET_CALLBACK_CLIENT_COUNT, clientRegistry.size());
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public void registerResponseCallbackHandler(String webSocketCorrelationId, WebSocketResponseCallback expectationResponseCallback) {
        responseCallbackRegistry.put(webSocketCorrelationId, expectationResponseCallback);
        Metrics.set(WEBSOCKET_CALLBACK_RESPONSE_HANDLER_COUNT, responseCallbackRegistry.size());
    }

    public void unregisterResponseCallbackHandler(String webSocketCorrelationId) {
        responseCallbackRegistry.remove(webSocketCorrelationId);
        Metrics.set(WEBSOCKET_CALLBACK_RESPONSE_HANDLER_COUNT, responseCallbackRegistry.size());
    }

    public void registerForwardCallbackHandler(String webSocketCorrelationId, WebSocketRequestCallback expectationForwardCallback) {
        forwardCallbackRegistry.put(webSocketCorrelationId, expectationForwardCallback);
        Metrics.set(WEBSOCKET_CALLBACK_FORWARD_HANDLER_COUNT, forwardCallbackRegistry.size());
    }

    public void unregisterForwardCallbackHandler(String webSocketCorrelationId) {
        forwardCallbackRegistry.remove(webSocketCorrelationId);
        Metrics.set(WEBSOCKET_CALLBACK_FORWARD_HANDLER_COUNT, forwardCallbackRegistry.size());
    }

    public boolean sendClientMessage(String clientId, HttpRequest httpRequest, HttpResponse httpResponse) {
        try {
            if (clientRegistry.containsKey(clientId)) {
                if (httpResponse == null) {
                    clientRegistry.get(clientId).channel().writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(httpRequest)));
                } else {
                    clientRegistry.get(clientId).channel().writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(
                        new HttpRequestAndHttpResponse()
                            .withHttpRequest(httpRequest)
                            .withHttpResponse(httpResponse)
                    )));
                }
                return true;
            } else {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setType(LogEntry.LogMessageType.WARN)
                        .setLogLevel(WARN)
                        .setHttpRequest(httpRequest)
                        .setMessageFormat("Client " + clientId + " not found for request {} client registry only contains {}")
                        .setArguments(httpRequest, clientRegistry)
                );
                return false;
            }
        } catch (Exception e) {
            throw new WebSocketException("Exception while sending web socket message " + httpRequest + " to client " + clientId, e);
        }
    }

    public void reset() {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            forwardCallbackRegistry.clear();
            responseCallbackRegistry.clear();
            clientRegistry.forEach((key, value) -> value.channel().close());
            clientRegistry.clear();
            clearWebSocketMetrics();
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }
}
