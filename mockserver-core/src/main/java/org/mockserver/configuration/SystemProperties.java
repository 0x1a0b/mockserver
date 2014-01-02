package org.mockserver.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author jamesdbloom
 */
public class SystemProperties {

    static final long DEFAULT_MAX_TIMEOUT = 120;
    static final int DEFAULT_BUFFER_SIZE = 1024 * 1500;
    static final String DEFAULT_STOP_KEY = "STOP_KEY";
    private static final Logger logger = LoggerFactory.getLogger(SystemProperties.class);

    public static long maxTimeout() {
        return SystemProperties.readLongProperty("mockserver.maxTimeout", TimeUnit.SECONDS.toMillis(SystemProperties.DEFAULT_MAX_TIMEOUT));
    }

    public static void maxTimeout(long timeout) {
        System.setProperty("mockserver.maxTimeout", "" + timeout);
    }

    public static int bufferSize() {
        return SystemProperties.readIntegerProperty("mockserver.requestBufferSize", SystemProperties.DEFAULT_BUFFER_SIZE);
    }

    public static void bufferSize(int size) {
        System.setProperty("mockserver.requestBufferSize", "" + size);
    }

    public static int stopPort(Integer port, Integer securePort) {
        return SystemProperties.readIntegerProperty("mockserver.stopPort", Math.max((port != null ? port : 0), (securePort != null ? securePort : 0)) + 1);
    }

    public static void stopPort(int port) {
        System.setProperty("mockserver.stopPort", "" + port);
    }

    private static Integer readIntegerProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, "" + defaultValue));
        } catch (NumberFormatException nfe) {
            logger.error("NumberFormatException converting " + key + " with value [" + System.getProperty(key) + "]", nfe);
            throw new RuntimeException("NumberFormatException converting " + key + " with value [" + System.getProperty(key) + "]", nfe);
        }
    }

    private static Long readLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(System.getProperty(key, "" + defaultValue));
        } catch (NumberFormatException nfe) {
            logger.error("NumberFormatException converting " + key + " with value [" + System.getProperty(key) + "]", nfe);
            throw new RuntimeException("NumberFormatException converting " + key + " with value [" + System.getProperty(key) + "]", nfe);
        }
    }
}
