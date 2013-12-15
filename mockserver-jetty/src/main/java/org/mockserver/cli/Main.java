package org.mockserver.cli;

import org.mockserver.proxy.ProxyRunner;
import org.mockserver.runner.AbstractRunner;
import org.mockserver.server.MockServerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author jamesdbloom
 */
public class Main {
    public static final String PROXY_PORT_KEY = "proxyPort";
    public static final String SERVER_PORT_KEY = "serverPort";
    private static final Logger logger = LoggerFactory.getLogger(MockServerRunner.class);

    /**
     * Run the MockServer directly providing the parseArguments for the server and proxy as the only input parameters (if not provided the server port defaults to 8080 and the proxy is not started).
     *
     * @param arguments the entries are in pairs:
     *                  - the first  pair is "-serverPort" followed by the server port if not provided the MockServer is not started,
     *                  - the second pair is "-proxyPort"  followed by the proxy  port if not provided the proxy      is not started
     */
    public static void main(String... arguments) {
        Map<String, Integer> parseArguments = parseArguments(arguments);
        AbstractRunner.overrideLogLevel(System.getProperty("mockserver.logLevel"));

        if (parseArguments.containsKey(PROXY_PORT_KEY)) {
            int proxyPort = parseArguments.get(PROXY_PORT_KEY);
            new ProxyRunner().start(proxyPort);
            logger.info("Started proxy listening on " + proxyPort);
            System.out.println("Started proxy listening on " + proxyPort);
        }

        if (parseArguments.containsKey(SERVER_PORT_KEY)) {
            int mockServerPort = parseArguments.get(SERVER_PORT_KEY);
            new MockServerRunner().start(mockServerPort);
            logger.info("Started MockServer listening on " + mockServerPort);
            System.out.println("Started MockServer listening on " + mockServerPort);
        }
    }

    private static Map<String, Integer> parseArguments(String... arguments) {
        Map<String, Integer> parsedArguments = new HashMap<>();
        Iterator<String> argumentsIterator = Arrays.asList(arguments).iterator();
        for (int i = 0; i < arguments.length; i++) {
            System.out.println("arguments[" + i + "] = " + arguments[i]);
        }
        while (argumentsIterator.hasNext()) {
            String argumentName = argumentsIterator.next();
            if (argumentsIterator.hasNext()) {
                String argumentValue = argumentsIterator.next();
                if (!parsePort(parsedArguments, SERVER_PORT_KEY, argumentName, argumentValue) && !parsePort(parsedArguments, PROXY_PORT_KEY, argumentName, argumentValue)) {
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
        String usage = "" +
                "   java -jar <path to mockserver-jetty-2.0-SNAPSHOT-jar-with-dependencies.jar> [-serverPort <port>] [-proxyPort <port>]\n" +
                "   \n" +
                "     valid options are:\n" +
                "        -serverPort <port>     specifies the port for the MockServer           \n" +
                "                               if not provide the MockServer is not started    \n" +
                "        -proxyPort <path>      specifies the port for the proxy                \n" +
                "                               if not provide the proxy is not started         \n";
        System.out.println(usage);
    }

}
