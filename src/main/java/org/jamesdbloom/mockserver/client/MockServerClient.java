package org.jamesdbloom.mockserver.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.jamesdbloom.mockserver.mappers.ExpectationMapper;
import org.jamesdbloom.mockserver.matchers.Times;
import org.jamesdbloom.mockserver.model.HttpRequest;

/**
 * @author jamesdbloom
 */
public class MockServerClient {

    private final String mockServerURI;

    private ExpectationMapper expectationMapper = new ExpectationMapper();

    public MockServerClient(String host, int port) {
        mockServerURI = "http://" + host + ":" + port + "/";
    }

    public ExpectationDTO when(final HttpRequest httpRequest) {
        return when(httpRequest, Times.unlimited());
    }

    public ExpectationDTO when(HttpRequest httpRequest, Times times) {
        return new ExpectationDTO(this, expectationMapper.transformsToMatcher(httpRequest), times);
    }

    public void sendExpectation(ExpectationDTO expectationDTO) {
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.start();
            httpClient.newRequest(mockServerURI).method(HttpMethod.POST).content(new StringContentProvider(expectationMapper.serialize(expectationDTO))).send();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception sending expectation to MockServer as %s", expectationDTO), e);
        }
    }
}
