package org.mockserver.cli;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.mockserver.mockserver.NettyMockServer;
import org.mockserver.proxy.http.HttpProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author jamesdbloom
 */
public class Main {
    public static final String PROXY_PORT_KEY = "proxyPort";
    public static final String PROXY_SECURE_PORT_KEY = "proxySecurePort";
    public static final String SERVER_PORT_KEY = "serverPort";
    public static final String SERVER_SECURE_PORT_KEY = "serverSecurePort";
    public static final String USAGE = "" +
            "   java -jar <path to mockserver-jetty-jar-with-dependencies.jar> [-serverPort <port>] [-serverSecurePort <port>] [-proxyPort <port>] [-proxySecurePort <port>]\n" +
            "   \n" +
            "     valid options are:\n" +
            "        -serverPort <port>         specifies the HTTP port for the MockServer      \n" +
            "                                   if neither serverPort or serverSecurePort       \n" +
            "                                   are provide the MockServer is not started       \n" +
            "        -serverSecurePort <port>   specifies the HTTPS port for the MockServer     \n" +
            "                                   if neither serverPort or serverSecurePort       \n" +
            "                                   are provide the MockServer is not started       \n" +
            "                                                                                   \n" +
            "        -proxyPort <path>          specifies the HTTP port for the proxy           \n" +
            "                                   if neither proxyPort or proxySecurePort         \n" +
            "                                   are provide the MockServer is not started       \n" +
            "        -proxySecurePort <path>    specifies the HTTPS port for the proxy          \n" +
            "                                   if neither proxyPort or proxySecurePort         \n" +
            "                                   are provide the MockServer is not started       \n";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @VisibleForTesting
    static HttpProxy proxy = new HttpProxy();
    @VisibleForTesting
    static NettyMockServer mockServer = new NettyMockServer();
    @VisibleForTesting
    static PrintStream outputPrintStream = System.out;
    @VisibleForTesting
    static boolean shutdownOnUsage = true;

    /**
     * Run the MockServer directly providing the parseArguments for the server and proxy as the only input parameters (if not provided the server port defaults to 8080 and the proxy is not started).
     *
     * @param arguments the entries are in pairs:
     *                  - the first  pair is "-serverPort" followed by the server port if not provided the MockServer is not started,
     *                  - the second pair is "-proxyPort"  followed by the proxy  port if not provided the proxy      is not started
     */
    public static void main(String... arguments) {
        Map<String, Integer> parseArguments = parseArguments(arguments);

        if (logger.isDebugEnabled()) {
            logger.debug("\n\nUsing command line options: " + Joiner.on(", ").withKeyValueSeparator("=").join(parseArguments) + "\n");
        }

        if (parseArguments.size() > 0) {
            if (parseArguments.containsKey(PROXY_PORT_KEY) || parseArguments.containsKey(PROXY_SECURE_PORT_KEY)) {
                proxy.overrideLogLevel(System.getProperty("mockserver.logLevel"));

                proxy.startHttpProxy(parseArguments.get(PROXY_PORT_KEY), parseArguments.get(PROXY_SECURE_PORT_KEY));
            }

            if (parseArguments.containsKey(SERVER_PORT_KEY) || parseArguments.containsKey(SERVER_SECURE_PORT_KEY)) {
                mockServer.overrideLogLevel(System.getProperty("mockserver.logLevel"));
                mockServer.start(parseArguments.get(SERVER_PORT_KEY), parseArguments.get(SERVER_SECURE_PORT_KEY));
            }
        } else {
            showUsage();
        }
    }

    @VisibleForTesting
    public static void reset() {
        proxy = new HttpProxy();
        mockServer = new NettyMockServer();
    }

    private static Map<String, Integer> parseArguments(String... arguments) {
        Map<String, Integer> parsedArguments = new HashMap<String, Integer>();
        Iterator<String> argumentsIterator = Arrays.asList(arguments).iterator();
        while (argumentsIterator.hasNext()) {
            String argumentName = argumentsIterator.next();
            if (argumentsIterator.hasNext()) {
                String argumentValue = argumentsIterator.next();
                if (!parsePort(parsedArguments, SERVER_PORT_KEY, argumentName, argumentValue)
                        && !parsePort(parsedArguments, PROXY_SECURE_PORT_KEY, argumentName, argumentValue)
                        && !parsePort(parsedArguments, PROXY_PORT_KEY, argumentName, argumentValue)
                        && !parsePort(parsedArguments, SERVER_SECURE_PORT_KEY, argumentName, argumentValue)) {
                    showUsage();
                }
            } else {
                showUsage();
            }
        }
        return parsedArguments;
    }

    private static boolean parsePort(Map<String, Integer> parsedArguments, final String key, final String argumentName, final String argumentValue) {
        if (argumentName.equals("-" + key)) {
            try {
                parsedArguments.put(key, Integer.parseInt(argumentValue));
                return true;
            } catch (NumberFormatException nfe) {
                System.out.println("Please provide a value integer for -" + key + ", [" + argumentValue + "] is not a valid integer");
                logger.error("Please provide a value integer for -" + key + ", [" + argumentValue + "] is not a valid integer", nfe);
            }
        }
        return false;
    }

    private static void showUsage() {
        outputPrintStream.println(USAGE);
        if (shutdownOnUsage) System.exit(1);
    }

}
