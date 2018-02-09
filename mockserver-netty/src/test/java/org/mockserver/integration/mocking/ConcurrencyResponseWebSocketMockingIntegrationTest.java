package org.mockserver.integration.mocking;

import org.junit.*;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ConcurrencyResponseWebSocketMockingIntegrationTest {

    private ClientAndServer server;
    private NettyHttpClient httpClient;

    @Before
    public void setUp() {
        server = ClientAndServer.startClientAndServer();
        server
            .when(
                request()
                    .withPath("/my/echo")
            )
            .respond(new ExpectationResponseCallback() {
                @Override
                public HttpResponse handle(HttpRequest request) {
                    return response()
                        .withHeader(CONTENT_LENGTH.toString(), String.valueOf(request.getBodyAsString().length()))
                        .withBody(request.getBodyAsString());
                }
            });
        httpClient = new NettyHttpClient();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void sendMultipleRequestsSingleThreaded() throws ExecutionException, InterruptedException, TimeoutException {
        scheduleTasksAndWaitForResponses(1);
    }

    @Test
    public void sendMultipleRequestsMultiThreaded() throws ExecutionException, InterruptedException, TimeoutException {
        scheduleTasksAndWaitForResponses(100);
    }

    private void scheduleTasksAndWaitForResponses(int parallelThreads) throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(parallelThreads);

        List<ScheduledFuture> scheduledFutures = new ArrayList<>();
        for (int i = 0; i < parallelThreads; i++) {
            scheduledFutures.add(executor.schedule(new Task(), 1L, TimeUnit.SECONDS));
        }

        for (int i = 0; i < parallelThreads; i++) {
            scheduledFutures.get(i).get(15L, TimeUnit.SECONDS);
        }
    }

    private void sendRequestAndVerifyResponse() {
        try {
            String requestBody = "thread: " + Thread.currentThread().getName() + ", random content: " + Math.random();
            HttpResponse httpResponse = httpClient.sendRequest(
                request()
                    .withMethod("POST")
                    .withPath("/my/echo")
                    .withBody(requestBody),
                new InetSocketAddress("localhost", server.getLocalPort())
            ).get(20, TimeUnit.MINUTES);
            Assert.assertEquals(requestBody, httpResponse.getBodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public class Task implements Runnable {
        @Override
        public void run() {
            ConcurrencyResponseWebSocketMockingIntegrationTest.this.sendRequestAndVerifyResponse();
        }
    }

}
