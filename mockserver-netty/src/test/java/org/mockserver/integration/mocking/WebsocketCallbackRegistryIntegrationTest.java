package org.mockserver.integration.mocking;

import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.integration.server.AbstractMockingIntegrationTestBase;
import org.mockserver.metrics.Metrics;
import org.mockserver.mock.action.ExpectationForwardCallback;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * @author jamesdbloom
 */
public class WebsocketCallbackRegistryIntegrationTest extends AbstractMockingIntegrationTestBase {

    @BeforeClass
    public static void startServer() {
        mockServerClient = new ClientAndServer();
    }

    @AfterClass
    public static void stopServer() {
        stopQuietly(mockServerClient);
    }

    @Override
    public int getServerPort() {
        return ((ClientAndServer) mockServerClient).getLocalPort();
    }


    @Test // same JVM due to dynamic calls to static Metrics class
    public void shouldRemoveWebsocketCallbackClientFromRegistryForClientReset() {
        // given
        Metrics.clear();
        final MockServerClient mockServerClient = new MockServerClient("localhost", getServerPort());
        mockServerClient
            .when(
                request()
            )
            .respond(
                new ExpectationResponseCallback() {
                    @Override
                    public HttpResponse handle(HttpRequest httpRequest) {
                        return response();
                    }
                }
            );

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(1));

        // when
        mockServerClient.reset();

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(0));
    }

    @Test // same JVM due to dynamic calls to static Metrics class
    public void shouldRemoveWebsocketCallbackClientFromRegistryForClientStop() {
        // given
        Metrics.clear();
        final MockServerClient mockServerClient = new ClientAndServer();
        mockServerClient
            .when(
                request()
            )
            .respond(
                new ExpectationResponseCallback() {
                    @Override
                    public HttpResponse handle(HttpRequest httpRequest) {
                        return response();
                    }
                }
            );

        try {
            // then
            Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(1));

            // when
            mockServerClient.stop();

            // then
            Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(0));
        } finally {
            mockServerClient.stop();
        }
    }

    @Test // same JVM due to dynamic calls to static Metrics class
    public void shouldRemoveWebsocketResponseHandlerFromRegistry() {
        // given
        Metrics.clear();
        mockServerClient
            .when(
                request()
                    .withPath(calculatePath("websocket_response_handler"))
            )
            .respond(
                new ExpectationResponseCallback() {
                    @Override
                    public HttpResponse handle(HttpRequest httpRequest) {
                        // then
                        return response()
                            .withBody("websocket_response_handler_count_" + Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_RESPONSE_HANDLER_COUNT) + "_" + Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_FORWARD_HANDLER_COUNT));
                    }
                }
            );

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(1));

        // when
        assertEquals(
            response()
                .withStatusCode(OK_200.code())
                .withReasonPhrase(OK_200.reasonPhrase())
                .withBody("websocket_response_handler_count_1_0"),
            makeRequest(
                request()
                    .withPath(calculatePath("websocket_response_handler")),
                headersToIgnore)
        );

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_RESPONSE_HANDLER_COUNT), CoreMatchers.is(0));
    }

    @Test // same JVM due to dynamic calls to static Metrics class
    public void shouldRemoveWebsocketForwardHandlerFromRegistry() {
        // given
        Metrics.clear();
        // when
        mockServerClient
            .when(
                request()
                    .withPath(calculatePath("websocket_forward_handler"))
            )
            .forward(
                new ExpectationForwardCallback() {
                    @Override
                    public HttpRequest handle(HttpRequest httpRequest) {
                        return request()
                            .withHeader("Host", "localhost:" + insecureEchoServer.getPort())
                            .withBody("websocket_forward_handler_count_" + Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_FORWARD_HANDLER_COUNT) + "_" + Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_RESPONSE_HANDLER_COUNT));
                    }
                }
            );

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_CLIENT_COUNT), CoreMatchers.is(1));

        // when
        assertEquals(
            response()
                .withStatusCode(OK_200.code())
                .withReasonPhrase(OK_200.reasonPhrase())
                .withBody("websocket_forward_handler_count_1_0"),
            makeRequest(
                request()
                    .withPath(calculatePath("websocket_forward_handler")),
                headersToIgnore)
        );

        // then
        Assert.assertThat(Metrics.get(Metrics.Name.WEBSOCKET_CALLBACK_FORWARD_HANDLER_COUNT), CoreMatchers.is(0));
    }
}
