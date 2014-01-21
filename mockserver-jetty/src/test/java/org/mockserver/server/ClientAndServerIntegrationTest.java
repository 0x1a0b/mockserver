package org.mockserver.server;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.integration.server.AbstractClientServerIntegrationTest;
import org.mockserver.socket.PortFactory;

import java.util.concurrent.ExecutionException;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * @author jamesdbloom
 */
public class ClientAndServerIntegrationTest extends AbstractClientServerIntegrationTest {

    private static final int serverPort = PortFactory.findFreePort();
    private static final int serverSecurePort = PortFactory.findFreePort();

    @BeforeClass
    public static void startServer() throws InterruptedException, ExecutionException {
        mockServerClient = startClientAndServer(serverPort, serverSecurePort);
    }

    @Override
    public int getPort() {
        return serverPort;
    }

    @Override
    public int getSecurePort() {
        return serverSecurePort;
    }

    @AfterClass
    public static void stopServer() {
        if (mockServerClient instanceof ClientAndServer) {
            ((ClientAndServer) mockServerClient).stop();
        }
    }
}
