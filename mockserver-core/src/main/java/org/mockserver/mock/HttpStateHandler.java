package org.mockserver.mock;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.callback.WebSocketClientRegistry;
import org.mockserver.log.MockServerEventLog;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.*;
import org.mockserver.responsewriter.ResponseWriter;
import org.mockserver.scheduler.Scheduler;
import org.mockserver.serialization.*;
import org.mockserver.serialization.java.ExpectationToJavaSerializer;
import org.mockserver.serialization.java.HttpRequestToJavaSerializer;
import org.mockserver.server.initialize.ExpectationInitializerLoader;
import org.mockserver.verify.Verification;
import org.mockserver.verify.VerificationSequence;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.net.MediaType.*;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.log.model.LogEntry.LogMessageType.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.socket.tls.KeyAndCertificateFactory.addSubjectAlternativeName;
import static org.slf4j.event.Level.TRACE;

/**
 * @author jamesdbloom
 */
public class HttpStateHandler {

    public static final String LOG_SEPARATOR = NEW_LINE + "------------------------------------" + NEW_LINE;
    public static final String PATH_PREFIX = "/mockserver";
    private final String uniqueLoopPreventionHeaderValue = "MockServer_" + UUID.randomUUID().toString();
    private final MockServerEventLog mockServerLog;
    private final Scheduler scheduler;
    // mockserver
    private MockServerMatcher mockServerMatcher;
    private MockServerLogger mockServerLogger = new MockServerLogger(this);
    private WebSocketClientRegistry webSocketClientRegistry = new WebSocketClientRegistry();
    // serializers
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer(mockServerLogger);
    private HttpRequestResponseSerializer httpRequestResponseSerializer = new HttpRequestResponseSerializer(mockServerLogger);
    private ExpectationSerializer expectationSerializer = new ExpectationSerializer(mockServerLogger);
    private HttpRequestToJavaSerializer httpRequestToJavaSerializer = new HttpRequestToJavaSerializer();
    private ExpectationToJavaSerializer expectationToJavaSerializer = new ExpectationToJavaSerializer();
    private VerificationSerializer verificationSerializer = new VerificationSerializer(mockServerLogger);
    private VerificationSequenceSerializer verificationSequenceSerializer = new VerificationSequenceSerializer(mockServerLogger);
    private LogEntrySerializer logEntrySerializer = new LogEntrySerializer(mockServerLogger);

    public HttpStateHandler(Scheduler scheduler) {
        this.scheduler = scheduler;
        mockServerLog = new MockServerEventLog(mockServerLogger, scheduler, true);
        mockServerMatcher = new MockServerMatcher(mockServerLogger, scheduler, webSocketClientRegistry);
        addExpectationsFromInitializer();
    }

    private void addExpectationsFromInitializer() {
        for (Expectation expectation : ExpectationInitializerLoader.loadExpectations()) {
            mockServerMatcher.add(expectation);
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(CREATED_EXPECTATION)
                    .setLogLevel(Level.INFO)
                    .setHttpRequest(expectation.getHttpRequest())
                    .setMessageFormat("creating expectation:{}")
                    .setArguments(expectation.clone())
            );
        }
    }

    public MockServerLogger getMockServerLogger() {
        return mockServerLogger;
    }

    public void clear(HttpRequest request) {
        HttpRequest requestMatcher = null;
        if (isNotBlank(request.getBodyAsString())) {
            requestMatcher = httpRequestSerializer.deserialize(request.getBodyAsString());
        }
        try {
            ClearType retrieveType = ClearType.valueOf(StringUtils.defaultIfEmpty(request.getFirstQueryStringParameter("type").toUpperCase(), "ALL"));
            switch (retrieveType) {
                case LOG:
                    mockServerLog.clear(requestMatcher);
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(CLEARED)
                            .setLogLevel(Level.INFO)
                            .setHttpRequest(requestMatcher)
                            .setMessageFormat("clearing logs that match:{}")
                            .setArguments((requestMatcher == null ? "{}" : requestMatcher))
                    );
                    break;
                case EXPECTATIONS:
                    mockServerMatcher.clear(requestMatcher);
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(CLEARED)
                            .setLogLevel(Level.INFO)
                            .setHttpRequest(requestMatcher)
                            .setMessageFormat("clearing expectations that match:{}")
                            .setArguments((requestMatcher == null ? "{}" : requestMatcher))
                    );
                    break;
                case ALL:
                    mockServerLog.clear(requestMatcher);
                    mockServerMatcher.clear(requestMatcher);
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(CLEARED)
                            .setLogLevel(Level.INFO)
                            .setHttpRequest(requestMatcher)
                            .setMessageFormat("clearing expectations and logs that match:{}")
                            .setArguments((requestMatcher == null ? "{}" : requestMatcher))
                    );
                    break;
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type") + "\" is not a valid value for \"type\" parameter, only the following values are supported " + Lists.transform(Arrays.asList(ClearType.values()), new Function<ClearType, String>() {
                public String apply(ClearType input) {
                    return input.name().toLowerCase();
                }
            }));
        }
    }

    public void reset() {
        mockServerMatcher.reset();
        mockServerLog.reset();
        mockServerLogger.logEvent(
            new LogEntry()
                .setType(CLEARED)
                .setLogLevel(Level.INFO)
                .setHttpRequest(request())
                .setMessageFormat("resetting all expectations and request logs")
        );
    }

    public void add(Expectation... expectations) {
        for (Expectation expectation : expectations) {
            if (expectation.getHttpRequest() != null) {
                final String hostHeader = expectation.getHttpRequest().getFirstHeader(HOST.toString());
                if (isNotBlank(hostHeader)) {
                    scheduler.submit(() -> addSubjectAlternativeName(hostHeader));
                }
            }
            mockServerMatcher.add(expectation);
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.INFO)
                    .setType(CREATED_EXPECTATION)
                    .setHttpRequest(expectation.getHttpRequest())
                    .setMessageFormat("creating expectation:{}")
                    .setArguments(expectation.clone())
            );
        }
    }

    public Expectation firstMatchingExpectation(HttpRequest request) {
        if (mockServerMatcher.isEmpty()) {
            return null;
        } else {
            return mockServerMatcher.firstMatchingExpectation(request);
        }
    }

    public void log(LogEntry logEntry) {
        if (mockServerLog != null) {
            mockServerLog.add(logEntry);
        }
    }

    public HttpResponse retrieve(HttpRequest request) {
        SettableFuture<HttpResponse> httpResponseSettableFuture = SettableFuture.create();
        HttpResponse response = response().withStatusCode(200);
        if (request != null) {
            try {
                final HttpRequest httpRequest = isNotBlank(request.getBodyAsString()) ? httpRequestSerializer.deserialize(request.getBodyAsString()) : null;
                Format format = Format.valueOf(StringUtils.defaultIfEmpty(request.getFirstQueryStringParameter("format").toUpperCase(), "JSON"));
                RetrieveType retrieveType = RetrieveType.valueOf(StringUtils.defaultIfEmpty(request.getFirstQueryStringParameter("type").toUpperCase(), "REQUESTS"));
                switch (retrieveType) {
                    case LOGS: {
                        final Object[] arguments = new Object[]{(httpRequest == null ? request() : httpRequest)};
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(RETRIEVED)
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("retrieving logs that match:{}")
                                .setArguments(arguments)
                        );
                        mockServerLog.retrieveMessageLogEntries(httpRequest, (List<LogEntry> logEntries) -> {
                            StringBuilder stringBuffer = new StringBuilder();
                            for (int i = 0; i < logEntries.size(); i++) {
                                LogEntry messageLogEntry = logEntries.get(i);
                                stringBuffer
                                    .append(messageLogEntry.getTimestamp())
                                    .append(" - ")
                                    .append(messageLogEntry.getMessage());
                                if (i < logEntries.size() - 1) {
                                    stringBuffer.append(LOG_SEPARATOR);
                                }
                            }
                            stringBuffer.append(NEW_LINE);
                            response.withBody(stringBuffer.toString(), PLAIN_TEXT_UTF_8);
                            httpResponseSettableFuture.set(response);
                        });
                        break;
                    }
                    case REQUESTS: {
                        final Object[] arguments = new Object[]{(httpRequest == null ? request() : httpRequest)};
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(RETRIEVED)
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("retrieving requests in " + format.name().toLowerCase() + " that match:{}")
                                .setArguments(arguments)
                        );
                        switch (format) {
                            case JAVA:
                                mockServerLog
                                    .retrieveRequests(
                                        httpRequest,
                                        requests -> {
                                            response.withBody(
                                                httpRequestToJavaSerializer.serialize(requests),
                                                create("application", "java").withCharset(UTF_8)
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRequests(
                                        httpRequest,
                                        requests -> {
                                            response.withBody(
                                                httpRequestSerializer.serialize(requests),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                            case LOG_ENTRIES:
                                mockServerLog
                                    .retrieveRequestLogEntries(
                                        httpRequest,
                                        logEntries -> {
                                            response.withBody(
                                                logEntrySerializer.serialize(logEntries),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case REQUEST_RESPONSES: {
                        final Object[] arguments = new Object[]{(httpRequest == null ? request() : httpRequest)};
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(RETRIEVED)
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("retrieving requests and responses in " + format.name().toLowerCase() + " that match:{}")
                                .setArguments(arguments)
                        );
                        switch (format) {
                            case JAVA:
                                response.withBody("JAVA not supported for REQUEST_RESPONSES", create("text", "plain").withCharset(UTF_8));
                                httpResponseSettableFuture.set(response);
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRequestResponses(
                                        httpRequest,
                                        requests -> {
                                            response.withBody(
                                                httpRequestResponseSerializer.serialize(requests),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                            case LOG_ENTRIES:
                                mockServerLog
                                    .retrieveRequestResponseMessageLogEntries(
                                        httpRequest,
                                        logEntries -> {
                                            response.withBody(
                                                logEntrySerializer.serialize(logEntries),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case RECORDED_EXPECTATIONS: {
                        final Object[] arguments = new Object[]{(httpRequest == null ? request() : httpRequest)};
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(RETRIEVED)
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("retrieving recorded expectations in " + format.name().toLowerCase() + " that match:{}")
                                .setArguments(arguments)
                        );
                        switch (format) {
                            case JAVA:
                                mockServerLog
                                    .retrieveRecordedExpectations(
                                        httpRequest,
                                        requests -> {
                                            response.withBody(
                                                expectationToJavaSerializer.serialize(requests),
                                                create("application", "java").withCharset(UTF_8)
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRecordedExpectations(
                                        httpRequest,
                                        requests -> {
                                            response.withBody(
                                                expectationSerializer.serialize(requests),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                            case LOG_ENTRIES:
                                mockServerLog
                                    .retrieveRecordedExpectationLogEntries(
                                        httpRequest,
                                        logEntries -> {
                                            response.withBody(
                                                logEntrySerializer.serialize(logEntries),
                                                JSON_UTF_8
                                            );
                                            httpResponseSettableFuture.set(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case ACTIVE_EXPECTATIONS: {
                        final Object[] arguments = new Object[]{(httpRequest == null ? request() : httpRequest)};
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(RETRIEVED)
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("retrieving active expectations in " + format.name().toLowerCase() + " that match:{}")
                                .setArguments(arguments)
                        );
                        List<Expectation> expectations = mockServerMatcher.retrieveActiveExpectations(httpRequest);
                        switch (format) {
                            case JAVA:
                                response.withBody(expectationToJavaSerializer.serialize(expectations), create("application", "java").withCharset(UTF_8));
                                break;
                            case JSON:
                                response.withBody(expectationSerializer.serialize(expectations), JSON_UTF_8);
                                break;
                            case LOG_ENTRIES:
                                response.withBody("LOG_ENTRIES not supported for ACTIVE_EXPECTATIONS", create("text", "plain").withCharset(UTF_8));
                                break;
                        }
                        httpResponseSettableFuture.set(response);
                        break;
                    }
                }

                try {
                    return httpResponseSettableFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException("Exception retrieving state for " + request, e);
                }
            } catch (IllegalArgumentException iae) {
                if (iae.getMessage().contains(RetrieveType.class.getSimpleName())) {
                    throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type") + "\" is not a valid value for \"type\" parameter, only the following values are supported " + Arrays.stream(RetrieveType.values()).map(input -> input.name().toLowerCase()).collect(Collectors.toList()));
                } else {
                    throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("format") + "\" is not a valid value for \"format\" parameter, only the following values are supported " + Arrays.stream(Format.values()).map(input -> input.name().toLowerCase()).collect(Collectors.toList()));
                }
            }
        } else {
            return response().withStatusCode(200);
        }
    }

    public Future<String> verify(Verification verification) {
        SettableFuture<String> result = SettableFuture.create();
        mockServerLog.verify(verification, result::set);
        return result;
    }

    public void verify(Verification verification, Consumer<String> resultConsumer) {
        mockServerLog.verify(verification, resultConsumer);
    }

    public Future<String> verify(VerificationSequence verification) {
        SettableFuture<String> result = SettableFuture.create();
        mockServerLog.verify(verification, result::set);
        return result;
    }

    public void verify(VerificationSequence verification, Consumer<String> resultConsumer) {
        mockServerLog.verify(verification, resultConsumer);
    }

    public boolean handle(HttpRequest request, ResponseWriter responseWriter, boolean warDeployment) {
        SettableFuture<Boolean> canHandle = SettableFuture.create();

        mockServerLogger.logEvent(
            new LogEntry()
                .setType(LogEntry.LogMessageType.TRACE)
                .setLogLevel(TRACE)
                .setHttpRequest(request)
                .setMessageFormat("received request:{}")
                .setArguments(request)
        );

        if (request.matches("PUT", PATH_PREFIX + "/expectation", "/expectation")) {

            for (Expectation expectation : expectationSerializer.deserializeArray(request.getBodyAsString())) {
                if (!warDeployment || validateSupportedFeatures(expectation, request, responseWriter)) {
                    add(expectation);
                }
            }
            responseWriter.writeResponse(request, CREATED);
            canHandle.set(true);

        } else if (request.matches("PUT", PATH_PREFIX + "/clear", "/clear")) {

            clear(request);
            responseWriter.writeResponse(request, OK);
            canHandle.set(true);

        } else if (request.matches("PUT", PATH_PREFIX + "/reset", "/reset")) {

            reset();
            responseWriter.writeResponse(request, OK);
            canHandle.set(true);

        } else if (request.matches("PUT", PATH_PREFIX + "/retrieve", "/retrieve")) {

            responseWriter.writeResponse(request, retrieve(request), true);
            canHandle.set(true);

        } else if (request.matches("PUT", PATH_PREFIX + "/verify", "/verify")) {

            Verification verification = verificationSerializer.deserialize(request.getBodyAsString());
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(VERIFICATION)
                    .setLogLevel(Level.INFO)
                    .setHttpRequest(verification.getHttpRequest())
                    .setMessageFormat("verifying requests that match:{}")
                    .setArguments(verification)
            );
            verify(verification, result -> {
                if (StringUtils.isEmpty(result)) {
                    responseWriter.writeResponse(request, ACCEPTED);

                } else {
                    responseWriter.writeResponse(request, NOT_ACCEPTABLE, result, create("text", "plain").toString());
                }
                canHandle.set(true);
            });

        } else if (request.matches("PUT", PATH_PREFIX + "/verifySequence", "/verifySequence")) {

            VerificationSequence verificationSequence = verificationSequenceSerializer.deserialize(request.getBodyAsString());
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(VERIFICATION)
                    .setLogLevel(Level.INFO)
                    .setHttpRequests(verificationSequence.getHttpRequests().toArray(new HttpRequest[0]))
                    .setMessageFormat("verifying sequence that match:{}")
                    .setArguments(verificationSequence)
            );
            verify(verificationSequence, result -> {
                if (StringUtils.isEmpty(result)) {
                    responseWriter.writeResponse(request, ACCEPTED);
                } else {
                    responseWriter.writeResponse(request, NOT_ACCEPTABLE, result, create("text", "plain").toString());
                }
                canHandle.set(true);
            });

        } else {
            canHandle.set(false);
        }

        try {
            return canHandle.get();
        } catch (InterruptedException | ExecutionException ignore) {
            return false;
        }
    }

    private boolean validateSupportedFeatures(Expectation expectation, HttpRequest request, ResponseWriter responseWriter) {
        boolean valid = true;
        Action action = expectation.getAction();
        String NOT_SUPPORTED_MESSAGE = " is not supported by MockServer deployed as a WAR due to limitations in the JEE specification; use mockserver-netty to enable these features";
        if (action instanceof HttpResponse && ((HttpResponse) action).getConnectionOptions() != null) {
            valid = false;
            responseWriter.writeResponse(request, response("ConnectionOptions" + NOT_SUPPORTED_MESSAGE), true);
        } else if (action instanceof HttpObjectCallback) {
            valid = false;
            responseWriter.writeResponse(request, response("HttpObjectCallback" + NOT_SUPPORTED_MESSAGE), true);
        } else if (action instanceof HttpError) {
            valid = false;
            responseWriter.writeResponse(request, response("HttpError" + NOT_SUPPORTED_MESSAGE), true);
        }
        return valid;
    }

    public WebSocketClientRegistry getWebSocketClientRegistry() {
        return webSocketClientRegistry;
    }

    public MockServerMatcher getMockServerMatcher() {
        return mockServerMatcher;
    }

    public MockServerEventLog getMockServerLog() {
        return mockServerLog;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String getUniqueLoopPreventionHeaderName() {
        return "x-forwarded-by";
    }

    public String getUniqueLoopPreventionHeaderValue() {
        return uniqueLoopPreventionHeaderValue;
    }
}
