package org.mockserver.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.mockserver.integration.proxy.SSLContextFactory;
import org.mockserver.integration.server.AbstractClientServerIntegrationTest;
import org.mockserver.socket.PortFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author jamesdbloom
 */
@Ignore
public class ClientServerWarIntegrationTest extends AbstractClientServerIntegrationTest {

    private final int serverPort = PortFactory.findFreePort();
    private final int serverSecurePort = PortFactory.findFreePort();
    private Server server = new Server();

    @Before
    public void startServer() throws Exception {
        // add http connector
        ServerConnector http = new ServerConnector(server);
        http.setPort(serverPort);
        server.addConnector(http);
        // add https connector
        ServerConnector https = new ServerConnector(server, new SslConnectionFactory(SSLContextFactory.createSSLContextFactory(), "http/1.1"));
        https.setPort(serverSecurePort);
        server.addConnector(https);

        // add handler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/" + getServletContext());
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MockServerServlet()), "/*");

        // start server
        server.start();

        TimeUnit.SECONDS.sleep(2);
    }

    public String getServletContext() {
        return "mockserver";
    }

    @Override
    public int getPort() {
        return serverPort;
    }

    @Override
    public int getSecurePort() {
        return serverSecurePort;
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }
}
