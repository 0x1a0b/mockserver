package org.mockserver.logging;

import com.google.common.collect.ImmutableList;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.log.model.MessageLogEntry;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.LogManager;

import static org.mockserver.configuration.ConfigurationProperties.javaLoggerLogLevel;
import static org.mockserver.configuration.ConfigurationProperties.logLevel;
import static org.mockserver.formatting.StringFormatter.formatLogMessage;
import static org.mockserver.log.model.MessageLogEntry.LogMessageType.EXCEPTION;
import static org.mockserver.model.HttpRequest.request;
import static org.slf4j.event.Level.*;

/**
 * @author jamesdbloom
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public class MockServerLogger {

    static {
        configureLogger();
    }

    public static void configureLogger() {
        try {
            if (System.getProperty("java.util.logging.config.file") == null && System.getProperty("java.util.logging.config.class") == null) {
                String loggingConfiguration = "" +
                    "handlers=org.mockserver.logging.StandardOutConsoleHandler\n" +
                    "org.mockserver.logging.StandardOutConsoleHandler.level=ALL\n" +
                    "org.mockserver.logging.StandardOutConsoleHandler.formatter=java.util.logging.SimpleFormatter\n" +
                    "java.util.logging.SimpleFormatter.format=%1$tF %1$tT  %3$s  %4$s  %5$s %n\n" +
                    ".level=" + javaLoggerLogLevel() + "\n" +
                    "io.netty.handler.ssl.SslHandler.level=WARNING";
                LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(Charset.forName("UTF-8"))));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static final MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger();

    private final boolean auditEnabled;
    private final boolean logEnabled;
    private final Logger logger;
    private final HttpStateHandler httpStateHandler;

    public MockServerLogger() {
        this(MockServerLogger.class);
    }

    public MockServerLogger(final Class loggerClass) {
        this(LoggerFactory.getLogger(loggerClass), null);
    }

    public MockServerLogger(final Logger logger, final @Nullable HttpStateHandler httpStateHandler) {
        this.logger = logger;
        this.httpStateHandler = httpStateHandler;
        this.auditEnabled = !ConfigurationProperties.disableRequestAudit();
        this.logEnabled = !ConfigurationProperties.disableSystemOut();
    }

    public void trace(final String message, final Object... arguments) {
        trace(null, message, arguments);
    }

    public void trace(final HttpRequest request, final String message, final Object... arguments) {
        if (isEnabled(TRACE)) {
            addLogEvents(MessageLogEntry.LogMessageType.TRACE, TRACE, request, message, arguments);
            final String logMessage = formatLogMessage(message, arguments);
            if (logEnabled) {
                logger.trace(logMessage);
            }
        }
    }

    public void debug(final MessageLogEntry.LogMessageType type, final String message, final Object... arguments) {
        debug(type, null, message, arguments);
    }

    public void debug(final MessageLogEntry.LogMessageType type, final HttpRequest request, final String message, final Object... arguments) {
        if (isEnabled(DEBUG)) {
            addLogEvents(type, DEBUG, request, message, arguments);
            final String logMessage = formatLogMessage(message, arguments);
            if (logEnabled) {
                logger.debug(logMessage);
            }
        }
    }

    public void info(final MessageLogEntry.LogMessageType type, final String message, final Object... arguments) {
        info(type, (HttpRequest) null, message, arguments);
    }

    public void info(final MessageLogEntry.LogMessageType type, final HttpRequest request, final String message, final Object... arguments) {
        info(type, ImmutableList.of(request != null ? request : request()), message, arguments);
    }

    public void info(final MessageLogEntry.LogMessageType type, final List<HttpRequest> requests, final String message, final Object... arguments) {
        if (isEnabled(INFO)) {
            addLogEvents(type, INFO, requests, message, arguments);
            final String logMessage = formatLogMessage(message, arguments);
            if (logEnabled) {
                logger.info(logMessage);
            }
        }
    }

    public void warn(final String message) {
        warn((HttpRequest) null, message);
    }

    public void warn(final String message, final Object... arguments) {
        warn(null, message, arguments);
    }

    public void warn(final @Nullable HttpRequest request, final String message, final Object... arguments) {
        if (isEnabled(WARN)) {
            addLogEvents(MessageLogEntry.LogMessageType.WARN, WARN, request, message, arguments);
            final String logMessage = formatLogMessage(message, arguments);
            if (logEnabled) {
                logger.error(logMessage);
            }
        }
    }

    public void error(final String message, final Throwable throwable) {
        error((HttpRequest) null, throwable, message);
    }

    public void error(final String message, final Object... arguments) {
        error(null, message, arguments);
    }

    public void error(final @Nullable HttpRequest request, final String message, final Object... arguments) {
        error(request, null, message, arguments);
    }

    public void error(final @Nullable HttpRequest request, final Throwable throwable, final String message, final Object... arguments) {
        error(ImmutableList.of(request != null ? request : request()), throwable, message, arguments);
    }

    public void error(final List<HttpRequest> requests, final Throwable throwable, final String message, final Object... arguments) {
        if (isEnabled(ERROR)) {
            addLogEvents(EXCEPTION, ERROR, requests, message, arguments);
            final String logMessage = formatLogMessage(message, arguments);
            if (logEnabled) {
                logger.error(logMessage, throwable);
            }
        }
    }

    private void addLogEvents(final MessageLogEntry.LogMessageType type, final Level logLeveL, final @Nullable HttpRequest request, final String message, final Object... arguments) {
        if (auditEnabled && httpStateHandler != null) {
            httpStateHandler.log(new MessageLogEntry(type, logLeveL, request, message, arguments));
        }
    }

    private void addLogEvents(final MessageLogEntry.LogMessageType type, final Level logLeveL, final List<HttpRequest> requests, final String message, final Object... arguments) {
        if (auditEnabled && httpStateHandler != null) {
            httpStateHandler.log(new MessageLogEntry(type, logLeveL, requests, message, arguments));
        }
    }

    public boolean isEnabled(final Level level) {
        return logLevel() != null && level.toInt() >= logLevel().toInt();
    }
}
