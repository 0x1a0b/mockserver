package org.mockserver.runner;

import ch.qos.logback.classic.Level;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mockserver.integration.proxy.SSLContextFactory;
import org.mockserver.proxy.connect.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockserver.configuration.SystemProperties.*;

/**
 * @author jamesdbloom
 */
public abstract class AbstractRunner {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractRunner.class);
    Server server;
    ShutdownThread shutdownThread;

    /**
     * Override the debug WARN logging level
     *
     * @param level the log level, which can be ALL, DEBUG, INFO, WARN, ERROR, OFF
     */
    public void overrideLogLevel(String level) {
        Logger rootLogger = LoggerFactory.getLogger("org.mockserver");
        if (rootLogger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.toLevel(level));
        }
    }

    /**
     * Start the MockServer instance in the port provided using -Dmockserver.stopPort for the stopPort.
     * If -Dmockserver.stopPort is not provided the default value used will be the port parameter + 1.
     *
     * @param port the port the listens to incoming HTTP requests
     * @return A Future that returns the state of the MockServer once it is stopped, this Future can be used to block execution until the MockServer is stopped.
     */
    public Future<String> start(final Integer port, final Integer securePort) {
        if (port == null && securePort == null) throw new IllegalStateException("You must specify a port or a secure port");
        if (isRunning()) throw new IllegalStateException("Server already running");
        final String startedMessage = "Started " + this.getClass().getSimpleName().replace("Runner", "") + " listening on:" + (port != null ? " standard port " + port : "") + (securePort != null ? " secure port " + securePort : "");

        final SettableFuture<String> future = SettableFuture.create();
        new Thread(new Runnable() {
            @Override
            public void run() {
                server = new Server();

                // add connectors
                List<ServerConnector> serverConnectors = new ArrayList<>();
                if (port != null) {
                    serverConnectors.add(createHTTPConnector(server, port, securePort));
                }
                try {
                    if (securePort != null) {
                        serverConnectors.add(createHTTPSConnector(server, securePort));
                    }
                } catch (GeneralSecurityException | IOException e) {
                    logger.error("Exception while loading SSL certificate", e);
                }
                server.setConnectors(serverConnectors.toArray(new Connector[serverConnectors.size()]));

                // add handler
                ServletHandler servletHandler = new ServletHandler();
                servletHandler.addServletWithMapping(new ServletHolder(getServlet()), "/");
                if (securePort != null) {
                    server.setHandler(new ConnectHandler(servletHandler, securePort));
                } else {
                    server.setHandler(servletHandler);
                }

                // start server
                try {
                    shutdownThread = new ShutdownThread(stopPort(port, securePort));
                    server.start();
                    shutdownThread.start();
                } catch (Exception e) {
                    logger.error("Failed to start embedded jetty server", e);
                }

                logger.info(startedMessage);
                System.out.println(startedMessage);

                try {
                    server.join();
                } catch (InterruptedException ie) {
                    logger.error("InterruptedException while waiting for jetty server", ie);
                } finally {
                    future.set(server.getState());
                }
            }
        }).start();
        return future;
    }

    protected abstract HttpServlet getServlet();

    protected void extendHTTPConfig(HttpConfiguration https_config) {
        // allow subclasses to extend http configuration
    }

    private ServerConnector createHTTPConnector(Server server, Integer port, Integer securePort) {
        // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        if (securePort != null) {
            http_config.setSecurePort(securePort);
        }
        http_config.setOutputBufferSize(bufferSize());
        http_config.setRequestHeaderSize(bufferSize());
        http_config.setResponseHeaderSize(bufferSize());
        extendHTTPConfig(http_config);

        // HTTP connector
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(port);
        http.setIdleTimeout(maxTimeout());
        return http;
    }

    private ServerConnector createHTTPSConnector(Server server, Integer securePort) throws GeneralSecurityException, IOException {
        // HTTPS Configuration
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecurePort(securePort);
        https_config.setOutputBufferSize(bufferSize());
        https_config.setRequestHeaderSize(bufferSize());
        https_config.setResponseHeaderSize(bufferSize());
        https_config.addCustomizer(new SecureRequestCustomizer());
        extendHTTPConfig(https_config);

        // HTTPS connector
        ServerConnector https = new ServerConnector(server, new SslConnectionFactory(SSLContextFactory.createSSLContextFactory(), "http/1.1"), new HttpConnectionFactory(https_config));
        https.setPort(securePort);
        https.setIdleTimeout(maxTimeout());
        return https;
    }

    /**
     * Is this instance running?
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    /**
     * Stop this MockServer instance
     */
    public AbstractRunner stop() {
        if (!isRunning()) throw new IllegalStateException("Server is not running");
        try {
            shutdownThread.stopListening();
            server.stop();
            server.join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop embedded jetty server gracefully, stopping JVM", e);
        }
        return this;
    }

    /**
     * Stop a forked or remote MockServer instance
     *
     * @param ipAddress IP address as string of remote MockServer (i.e. "127.0.0.1")
     * @param stopPort  the stopPort for the MockServer to stop (default is HTTP port + 1)
     * @param stopWait  the period to wait for MockServer to confirm it has stopped, in seconds.  A value of <= 0 means do not wait for confirmation MockServer has stopped.
     */
    public boolean stop(String ipAddress, int stopPort, int stopWait) {
        if (stopPort <= 0)
            throw new IllegalArgumentException("Please specify a valid stopPort");

        boolean stopped = false;
        try {
            try (Socket socket = new Socket(InetAddress.getByName(ipAddress), stopPort)) {
                if (socket.isConnected() && socket.isBound()) {
                    OutputStream out = socket.getOutputStream();
                    out.write("stop".getBytes(StandardCharsets.UTF_8));
                    socket.shutdownOutput();

                    if (stopWait > 0) {
                        socket.setSoTimeout(stopWait * 1000);

                        logger.info("Waiting " + stopWait + " seconds for MockServer to stop");

                        if (new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine().contains("stopped")) {
                            logger.info("MockServer has stopped");
                            stopped = true;
                        }
                    } else {
                        stopped = true;
                    }
                }
            }
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Throwable t) {
            logger.error("Exception stopping MockServer", t);
            stopped = false;
        }
        return stopped;
    }

    private class ShutdownThread extends Thread {
        private final int port;
        private ServerSocket serverSocket;

        public ShutdownThread(int port) {
            this.port = port;
            setDaemon(true);
            setName("ShutdownThread");
        }

        @Override
        public void run() {
            try {
                try {

                    serverSocket = new ServerSocket(port);
                    logger.info("Waiting to receive MockServer stop request on port [" + port + "]");

                    while (serverSocket.isBound() && !serverSocket.isClosed()) {
                        try (Socket socket = serverSocket.accept()) {
                            if (new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine().contains("stop")) {
                                // shutdown server
                                AbstractRunner.this.stop();

                                // inform client
                                OutputStream out = socket.getOutputStream();
                                out.write("stopped".getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                } finally {
                    serverSocket.close();
                }
            } catch (Exception e) {
                logger.trace("Exception in shutdown thread", e);
            }
        }

        public void stopListening() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ioe) {
                    logger.error("Exception in shutdown thread", ioe);
                }
            }
        }

    }
}
