package org.mockserver.configuration;

import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.mockserver.socket.SSLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author jamesdbloom
 */
public class ConfigurationProperties {

    static final long DEFAULT_MAX_TIMEOUT = 120;
    static final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);
    static final Properties PROPERTIES = readPropertyFile();

    // property file config
    public static String propertyFile() {
        return System.getProperty("mockserver.propertyFile", "mockserver.properties");
    }

    // socket config
    public static long maxSocketTimeout() {
        return readLongProperty("mockserver.maxSocketTimeout", TimeUnit.SECONDS.toMillis(DEFAULT_MAX_TIMEOUT));
    }

    public static void maxSocketTimeout(long milliseconds) {
        System.setProperty("mockserver.maxSocketTimeout", "" + milliseconds);
    }

    // ssl config
    public static String javaKeyStoreFilePath() {
        return readPropertyHierarchically("mockserver.javaKeyStoreFilePath", SSLFactory.defaultKeyStoreFileName());
    }

    public static void javaKeyStoreFilePath(String keyStoreFilePath) {
        System.setProperty("mockserver.javaKeyStoreFilePath", keyStoreFilePath);
        rebuildKeyStore(true);
    }

    public static String javaKeyStorePassword() {
        return readPropertyHierarchically("mockserver.javaKeyStorePassword", SSLFactory.KEY_STORE_PASSWORD);
    }

    public static void javaKeyStorePassword(String keyStorePassword) {
        System.setProperty("mockserver.javaKeyStorePassword", keyStorePassword);
        rebuildKeyStore(true);
    }

    public static String javaKeyStoreType() {
        return readPropertyHierarchically("mockserver.javaKeyStoreType", KeyStore.getDefaultType());
    }

    public static void javaKeyStoreType(String keyStoreType) {
        System.setProperty("mockserver.javaKeyStoreType", keyStoreType);
        rebuildKeyStore(true);
    }

    public static boolean deleteGeneratedKeyStoreOnExit() {
        return Boolean.parseBoolean(readPropertyHierarchically("mockserver.deleteGeneratedKeyStoreOnExit", "" + true));
    }

    public static void deleteGeneratedKeyStoreOnExit(boolean deleteGeneratedKeyStoreOnExit) {
        System.setProperty("mockserver.deleteGeneratedKeyStoreOnExit", "" + deleteGeneratedKeyStoreOnExit);
        rebuildKeyStore(true);
    }

    public static String sslCertificateDomainName() {
        return readPropertyHierarchically("mockserver.sslCertificateDomainName", SSLFactory.CERTIFICATE_DOMAIN);
    }

    public static void sslCertificateDomainName(String domainName) {
        System.setProperty("mockserver.sslCertificateDomainName", domainName);
        rebuildKeyStore(true);
    }

    public static String[] sslSubjectAlternativeNameDomains() {
        String sslSubjectAlternativeNameDomains = readPropertyHierarchically("mockserver.sslSubjectAlternativeNameDomains", "localhost");
        if (sslSubjectAlternativeNameDomains.isEmpty()) {
            return new String[0];
        } else {
            return sslSubjectAlternativeNameDomains.split(",");
        }
    }

    public static void addSslSubjectAlternativeNameDomains(String... newSubjectAlternativeNameDomains) {
        boolean subjectAlternativeDomainsModified = false;
        Set<String> allSubjectAlternativeDomains = new TreeSet<String>();
        Collections.addAll(allSubjectAlternativeDomains, sslSubjectAlternativeNameDomains());
        for (String subjectAlternativeDomain : newSubjectAlternativeNameDomains) {
            if (allSubjectAlternativeDomains.add(subjectAlternativeDomain)) {
                subjectAlternativeDomainsModified = true;
            }
        }
        if (subjectAlternativeDomainsModified) {
            System.setProperty("mockserver.sslSubjectAlternativeNameDomains", Joiner.on(",").join(allSubjectAlternativeDomains));
            rebuildKeyStore(true);
        }
    }

    public static String[] sslSubjectAlternativeNameIps() {
        String sslSubjectAlternativeNameIps = readPropertyHierarchically("mockserver.sslSubjectAlternativeNameIps", "127.0.0.1,0.0.0.0");
        if (sslSubjectAlternativeNameIps.isEmpty()) {
            return new String[0];
        } else {
            return sslSubjectAlternativeNameIps.split(",");
        }
    }

    public static void addSslSubjectAlternativeNameIps(String... newSubjectAlternativeNameIps) {
        boolean subjectAlternativeIpsModified = false;
        Set<String> allSubjectAlternativeIps = new TreeSet<String>();
        Collections.addAll(allSubjectAlternativeIps, sslSubjectAlternativeNameIps());
        for (String subjectAlternativeDomain : newSubjectAlternativeNameIps) {
            if (allSubjectAlternativeIps.add(subjectAlternativeDomain)) {
                subjectAlternativeIpsModified = true;
            }
        }
        if (subjectAlternativeIpsModified) {
            System.setProperty("mockserver.sslSubjectAlternativeNameIps", Joiner.on(",").join(allSubjectAlternativeIps));
            rebuildKeyStore(true);
        }
    }

    public static boolean rebuildKeyStore() {
        return Boolean.parseBoolean(System.getProperty("mockserver.rebuildKeyStore", "false"));
    }

    public static void rebuildKeyStore(boolean rebuildKeyStore) {
        System.setProperty("mockserver.rebuildKeyStore", Boolean.toString(rebuildKeyStore));
    }

    // mockserver config
    public static int mockServerPort() {
        return readIntegerProperty("mockserver.mockServerPort", -1);
    }

    public static void mockServerPort(int port) {
        System.setProperty("mockserver.mockServerPort", "" + port);
    }

    // proxy config
    public static int proxyPort() {
        return readIntegerProperty("mockserver.proxyPort", -1);
    }

    public static void proxyPort(int port) {
        System.setProperty("mockserver.proxyPort", "" + port);
    }

    private static Integer readIntegerProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(readPropertyHierarchically(key, "" + defaultValue));
        } catch (NumberFormatException nfe) {
            logger.error("NumberFormatException converting " + key + " with value [" + readPropertyHierarchically(key, "" + defaultValue) + "]", nfe);
            return defaultValue;
        }
    }

    private static Long readLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(readPropertyHierarchically(key, "" + defaultValue));
        } catch (NumberFormatException nfe) {
            logger.error("NumberFormatException converting " + key + " with value [" + readPropertyHierarchically(key, "" + defaultValue) + "]", nfe);
            return defaultValue;
        }
    }

    public static Properties readPropertyFile() {

        Properties properties = new Properties();

        InputStream inputStream = ConfigurationProperties.class.getClassLoader().getResourceAsStream(propertyFile());
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Exception loading property file [" + propertyFile() + "]", e);
            }
        } else {
            logger.debug("Property file not found on classpath using path [" + propertyFile() + "]");
            try {
                properties.load(new FileInputStream(propertyFile()));
            } catch (FileNotFoundException e) {
                logger.debug("Property file not found using path [" + propertyFile() + "]");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Exception loading property file [" + propertyFile() + "]", e);
            }
        }

        if (!properties.isEmpty()) {
            IOUtils.closeQuietly(inputStream);
            Enumeration<?> propertyNames = properties.propertyNames();

            StringBuilder propertiesLogDump = new StringBuilder();
            propertiesLogDump.append("Reading properties from property file [").append(propertyFile()).append("]:\n");
            while (propertyNames.hasMoreElements()) {
                String propertyName = String.valueOf(propertyNames.nextElement());
                propertiesLogDump.append("\t").append(propertyName).append(" = ").append(properties.getProperty(propertyName)).append("\n");
            }
            logger.info(propertiesLogDump.toString());
        }

        return properties;
    }

    public static String readPropertyHierarchically(String key, String defaultValue) {
        return System.getProperty(key, PROPERTIES.getProperty(key, defaultValue));
    }

    /**
     * Override the debug WARN logging level
     *
     * @param level the log level, which can be ALL, DEBUG, INFO, WARN, ERROR, OFF
     */
    public static void overrideLogLevel(String level) {
        if (level != null) {
            if (!Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF").contains(level)) {
                throw new IllegalArgumentException("log level \"" + level + "\" is not legel it must be one of \"TRACE\", \"DEBUG\", \"INFO\", \"WARN\", \"ERROR\", \"OFF\"");
            }
            System.setProperty("mockserver.logLevel", level);
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
            overrideLogLevelWithReflection(level, "org.mockserver");
            overrideLogLevelWithReflection(level, "org.mockserver.mockserver");
            overrideLogLevelWithReflection(level, "org.mockserver.proxy");
        }
    }

    private static void overrideLogLevelWithReflection(String level, String loggerName) {
        Logger rootLogger = LoggerFactory.getLogger(loggerName);

        try {
            // create level instance
            Class logbackLevelClass = ConfigurationProperties.class.getClassLoader().loadClass("ch.qos.logback.classic.Level");
            Method toLevelMethod = logbackLevelClass.getMethod("toLevel", String.class);
            Object levelInstance = toLevelMethod.invoke(logbackLevelClass, level);

            // update root level
            Method setLevelMethod = rootLogger.getClass().getMethod("setLevel", logbackLevelClass);
            setLevelMethod.invoke(rootLogger, levelInstance);
        } catch (Exception e) {
            logger.warn("Exception updating logging level using reflection, likely cause is Logback is not on the classpath");
        }
    }


}
