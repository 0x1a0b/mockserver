package org.mockserver.runner;

import ch.qos.logback.classic.Level;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ShutdownMonitor;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.mockserver.proxy.ProxyRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Future;

/**
 * @author jamesdbloom
 */
public abstract class AbstractRunner {
    private static final Logger logger = LoggerFactory.getLogger(ProxyRunner.class);
    private static final String STOP_KEY = "STOP_KEY";

    private Server server;

    /**
     * Override the debug WARN logging level
     *
     * @param level the log level, which can be ALL, DEBUG, INFO, WARN, ERROR, OFF
     */
    public static void overrideLogLevel(String level) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.mockserver");
        rootLogger.setLevel(Level.toLevel(level));
    }

    /**
     * Start the MockServer instance in the port provided using -Dmockserver.stopPort and -Dmockserver.stopKey for the stopPort and stopKey respectively.
     * If -Dmockserver.stopPort is not provided the default value used will be the port parameter + 1.
     * If -Dmockserver.stopKey is not provided the default value used will be "STOP_KEY"
     *
     * @param port the port the listens to incoming HTTP requests
     * @return A Future that returns the state of the MockServer once it is stopped, this Future can be used to block execution until the MockServer is stopped.
     */
    public Future start(final int port) {
        if (isRunning()) throw new IllegalStateException("Server already running");
        runStopThread(port + 1, STOP_KEY);
        final SettableFuture<String> future = SettableFuture.create();
        new Thread(new Runnable() {
            @Override
            public void run() {
                server = new Server(port);
                addCertificates(server);
                ShutdownThread.register(server);
                ServletHandler handler = new ServletHandler();
                server.setHandler(handler);
                handler.addServletWithMapping(getServletName(), "/");

                try {
                    server.start();
                } catch (Exception e) {
                    logger.error("Failed to start embedded jetty server", e);
                    System.exit(1);
                }

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

    protected abstract String getServletName();

    protected void addCertificates(Server server) {

    }

    private void runStopThread(final int defaultStopPort, final String defaultStopKey) {
        ShutdownMonitor shutdownMonitor = ShutdownMonitor.getInstance();
        if (!shutdownMonitor.isAlive()) {
            try {
                int stopPort = Integer.parseInt(System.getProperty("mockserver.stopPort", "" + defaultStopPort));
                String stopKey = System.getProperty("mockserver.stopKey", defaultStopKey);
                logger.info("Listening on stopPort " + stopPort + " for stop requests with stopKey [" + stopKey + "]");
                System.out.println("Listening on stopPort " + stopPort + " for stop requests with stopKey [" + stopKey + "]");

                shutdownMonitor.setPort(stopPort);
                shutdownMonitor.setKey(stopKey);
                shutdownMonitor.setExitVm(false);
                shutdownMonitor.start();

            } catch (NumberFormatException nfe) {
                logger.error("Value specified for -Dmockserver.stopPort=" + System.getProperty("mockserver.stopPort") + " is not a valid number");
                System.exit(1);
            }
        }
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
            server.stop();
        } catch (Exception e) {
            logger.error("Failed to stop embedded jetty server gracefully, stopping JVM", e);
            System.exit(1);
        }
        return this;
    }

    /**
     * Stop a forked or remote MockServer instance
     *
     * @param ipAddress IP address as string of remote MockServer (i.e. "127.0.0.1")
     * @param stopPort  the stopPort for the MockServer to stop (default is HTTP port + 1)
     * @param stopKey   the stopKey for the MockServer to step (default is "STOP_KEY")
     * @param stopWait  the period to wait for MockServer to confirm it has stopped, in seconds.  A value of <= 0 means do not wait for confirmation MockServer has stopped.
     */
    public static void stopRemote(String ipAddress, int stopPort, String stopKey, int stopWait) {
        if (stopPort <= 0)
            throw new IllegalArgumentException("Please specify a valid stopPort");
        if (stopKey == null)
            throw new IllegalArgumentException("Please specify a valid stopKey");

        try {
            Socket s = new Socket(InetAddress.getByName(ipAddress), stopPort);
            s.setSoLinger(false, 0);

            OutputStream out = s.getOutputStream();
            out.write((stopKey + "\r\nstop\r\n").getBytes());
            out.flush();

            if (stopWait > 0) {
                s.setSoTimeout(stopWait * 1000);
                s.getInputStream();

                logger.info("Waiting %d seconds for MockServer to stop%n", stopWait);
                LineNumberReader lin = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                String response;
                boolean stopped = false;
                while (!stopped && ((response = lin.readLine()) != null)) {
                    if ("Stopped".equals(response)) {
                        stopped = true;
                        logger.info("MockServer has stopped");
                    }
                }
            } else {
                logger.info("MockServer stop http has been sent");
            }
            s.close();
        } catch (ConnectException e) {
            logger.info("MockServer is not running");
        } catch (Exception e) {
            logger.error("Exception stopping MockServer", e);
        }
    }
}
