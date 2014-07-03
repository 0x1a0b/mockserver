package org.mockserver.junit;

import com.google.common.annotations.VisibleForTesting;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import java.lang.reflect.Field;

public class MockServerRule implements TestRule {

    private final Object target;
    private final Integer httpPort;
    private final Integer httpsPort;

    /**
     * Start the MockServer prior to test execution and stop the MockServer after the tests have completed.
     * This constructor dynamically allocates a free port for the MockServer to use.
     *
     * @param target an instance of the test being executed
     */
    public MockServerRule(Object target) {
        this(PortFactory.findFreePort(), target);
    }

    /**
     * Start the proxy prior to test execution and stop the proxy after the tests have completed.
     * This constructor dynamically create a proxy that accepts HTTP requests on the specified port
     *
     * @param httpPort the HTTP port for the proxy
     * @param target an instance of the test being executed
     */
    public MockServerRule(Integer httpPort, Object target) {
        this(httpPort, null, target);
    }

    /**
     * Start the proxy prior to test execution and stop the proxy after the tests have completed.
     * This constructor dynamically create a proxy that accepts HTTP and HTTPS requests on the specified ports
     *
     * @param httpPort the HTTP port for the proxy
     * @param httpsPort the HTTPS port for the proxy
     * @param target an instance of the test being executed
     */
    public MockServerRule(Integer httpPort, Integer httpsPort, Object target) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.target = target;
    }

    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ClientAndServer clientAndServer = newClientAndServer();
                setMockServerClient(target, clientAndServer);
                try {
                    base.evaluate();
                } finally {
                    clientAndServer.stop();
                }
            }
        };
    }

    @VisibleForTesting
    ClientAndServer newClientAndServer() {
        if (httpsPort == null) {
            return ClientAndServer.startClientAndServer(httpPort);
        } else {
            return ClientAndServer.startClientAndServer(httpPort, httpsPort);
        }
    }

    private void setMockServerClient(Object target, ClientAndServer clientAndServer) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType().equals(MockServerClient.class)) {
                field.setAccessible(true);
                try {
                    field.set(target, clientAndServer);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Error setting MockServerClient field on " + target.getClass().getName(), e);
                }
            }
        }
    }
}