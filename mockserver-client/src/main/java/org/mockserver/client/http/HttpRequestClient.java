package org.mockserver.client.http;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.SettableFuture;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.mappers.HttpClientResponseMapper;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author jamesdbloom
 */
public class HttpRequestClient {
    private static final int[] urlAllowedCharacters = new int[]{'-', '.', '_', '~', '!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=', ':', '@', '/', '?'};

    static {
        Arrays.sort(urlAllowedCharacters);
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final HttpClientResponseMapper httpClientResponseMapper = new HttpClientResponseMapper();
    private final HttpClient httpClient;
    private String baseUri;
    private long maxTimeout;

    public HttpRequestClient(final String baseUri) {
        httpClient = new HttpClient(new SslContextFactory(true));
        configureHttpClient(baseUri);
    }

    @VisibleForTesting
    HttpRequestClient(final String baseUri, final HttpClient httpClient) {
        this.httpClient = httpClient;
        configureHttpClient(baseUri);
    }

    private static String encodeURL(String input) {
        byte[] sourceBytes = input.getBytes(Charset.forName("UTF-8"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(sourceBytes.length);
        for (byte aSource : sourceBytes) {
            int b = aSource;
            if (b < 0) {
                b += 256;
            }

            if (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9' || Arrays.binarySearch(urlAllowedCharacters, b) >= 0) {
                bos.write(b);
            } else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
            }
        }
        return new String(bos.toByteArray(), Charset.forName("UTF-8"));
    }

    private void configureHttpClient(String baseUri) {
        // base uri
        if (baseUri.endsWith("/")) throw new IllegalArgumentException("Base URL [" + baseUri + "] should not have a trailing slash");
        this.baseUri = baseUri;
        // timeout
        try {
            maxTimeout = TimeUnit.SECONDS.toMillis(Integer.parseInt(System.getProperty("proxy.maxTimeout", "120")));
        } catch (NumberFormatException nfe) {
            logger.error("NumberFormatException converting proxy.maxTimeout with value [" + System.getProperty("proxy.maxTimeout") + "]", nfe);
            throw new RuntimeException("NumberFormatException converting proxy.maxTimeout with value [" + System.getProperty("proxy.maxTimeout") + "]", nfe);
        }
        // http client
        try {
            httpClient.setFollowRedirects(false);
            // do not share cookies between connections
            httpClient.setCookieStore(new HttpCookieStore.Empty());
            // ensure we can proxy enough connections to same server
            httpClient.setMaxConnectionsPerDestination(1024);
            httpClient.setRequestBufferSize(1024 * 500);
            httpClient.setResponseBufferSize(1024 * 500);
            httpClient.setIdleTimeout(maxTimeout);
            httpClient.setConnectTimeout(maxTimeout);
            httpClient.start();
        } catch (Exception e) {
            logger.error("Exception starting HttpClient", e);
            throw new RuntimeException("Exception starting HttpClient", e);
        }
    }

    public void sendRequest(final String body, final String path) {
        try {
            httpClient.newRequest(baseUri + path)
                    .method(HttpMethod.PUT)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .content(new ComparableStringContentProvider(body, "UTF-8"))
                    .send();
        } catch (Exception e) {
            logger.error("Exception sending request to [" + path + "] with body [" + body + "]", e);
        }
    }

    public HttpResponse sendRequest(final HttpRequest httpRequest) {
        final SettableFuture<Response> responseFuture = SettableFuture.create();
        // todo is this large enough? - it should be configurable
        final ByteBuffer contentBuffer = ByteBuffer.allocate(1024 * 500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Received request:\n" + new HttpRequestSerializer().serialize(httpRequest));
                    }
                    String url = httpRequest.getURL();
                    if (Strings.isNullOrEmpty(url)) {
                        url = baseUri + httpRequest.getPath() + (Strings.isNullOrEmpty(httpRequest.getQueryString()) ? "" : '?' + httpRequest.getQueryString());
                    }
                    url = encodeURL(URLDecoder.decode(url, "UTF-8"));

                    if (logger.isTraceEnabled()) {
                        System.out.println(HttpMethod.fromString(httpRequest.getMethod()) + "=>" + url);
                    }
                    HttpMethod method = HttpMethod.fromString(httpRequest.getMethod());
                    Request request = httpClient
                            .newRequest(url)
                            .method((method == null ? HttpMethod.GET : method));
                    request.content(new StringContentProvider(httpRequest.getBody()));
                    for (Header header : httpRequest.getHeaders()) {
                        for (String value : header.getValues()) {
                            request.header(header.getName(), value);
                        }
                    }
                    if (HttpMethod.fromString(httpRequest.getMethod()) == HttpMethod.POST
                            && !request.getHeaders().containsKey(HttpHeader.CONTENT_TYPE.asString())) {
                        // handle missing header which causes error with IIS
                        request.header(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString());
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Cookie cookie : httpRequest.getCookies()) {
                        for (String value : cookie.getValues()) {
                            stringBuilder.append(cookie.getName()).append("=").append(value).append("; ");
                        }
                    }
                    if (stringBuilder.length() > 0) {
                        request.header("Cookie", stringBuilder.toString());
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Proxy sending request:\n" + new ObjectMapper()
                                .setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT)
                                .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
                                .setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY)
                                .configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false)
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(request));
                    }
                    request
                            .onResponseContent(new Response.ContentListener() {
                                @Override
                                public void onContent(Response response, ByteBuffer chunk) {
                                    contentBuffer.put(chunk);
                                }
                            })
                            .send(new Response.CompleteListener() {
                                @Override
                                public void onComplete(Result result) {
                                    if (result.isFailed()) {
                                        responseFuture.setException(result.getFailure());
                                    } else {
                                        responseFuture.set(result.getResponse());
                                    }
                                }
                            });
                } catch (Exception e) {
                    responseFuture.setException(e);
                }
            }
        }).start();
        try {
            Response proxiedResponse = responseFuture.get(maxTimeout, TimeUnit.SECONDS);
            byte[] content = new byte[contentBuffer.position()];
            contentBuffer.flip();
            contentBuffer.get(content);
            return httpClientResponseMapper.buildHttpResponse(proxiedResponse, content);
        } catch (Exception e) {
            throw new RuntimeException("Exception sending request to [" + httpRequest.getURL() + "] with body [" + httpRequest.getBody() + "]", e);
        }
    }
}
