package org.mockserver.matchers;

import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.file.FileReader;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.OpenAPIDefinition;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;
import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.matchers.MatchDifference.Field.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

/**
 * @author jamesdbloom
 */
@Ignore("still work in progress")
public class HttpRequestsPropertiesMatcherTest {

    private final MockServerLogger mockServerLogger = new MockServerLogger(HttpRequestsPropertiesMatcherTest.class);

    @Test
    public void shouldHandleBlankStrings() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("")
                .withOperationId("")
        ));
        HttpRequest httpRequest = request();
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldHandleNulls() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload(null)
                .withOperationId(null)
        ));
        HttpRequest httpRequest = request();
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldHandleInvalidOpenAPIJsonSpec() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);

        try {
            // when
            httpRequestsPropertiesMatcher.update(
                new OpenAPIDefinition()
                    .withSpecUrlOrPayload(FileReader.readFileFromClassPathOrPath("org/mockserver/mock/openapi_petstore_example.json").substring(0, 100))
                    .withOperationId("listPets")
            );

            // then
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("Unable to load API spec from provided URL or payload Unexpected end-of-input in field name" + NEW_LINE +
                " at [Source: (String)\"{" + NEW_LINE +
                "  \"openapi\": \"3.0.0\"," + NEW_LINE +
                "  \"info\": {" + NEW_LINE +
                "    \"version\": \"1.0.0\"," + NEW_LINE +
                "    \"title\": \"Swagger Petstore\"," + NEW_LINE +
                "    \"li\"; line: 6, column: 8]"));
        }
    }

    @Test
    public void shouldHandleInvalidOpenAPIYamlSpec() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);

        try {
            // when
            httpRequestsPropertiesMatcher.update(
                new OpenAPIDefinition()
                    .withSpecUrlOrPayload(FileReader.readFileFromClassPathOrPath("org/mockserver/mock/openapi_petstore_example.yaml").substring(0, 100))
                    .withOperationId("listPets")
            );

            // then
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("Unable to load API spec from provided URL or payload while scanning a simple key" + NEW_LINE +
                " in 'reader', line 8, column 1:" + NEW_LINE +
                "    servers" + NEW_LINE +
                "    ^" + NEW_LINE +
                "could not find expected ':'" + NEW_LINE +
                " in 'reader', line 8, column 8:" + NEW_LINE +
                "    servers" + NEW_LINE +
                "           ^" + NEW_LINE +
                "" + NEW_LINE +
                " at [Source: (StringReader); line: 8, column: 1]"));
        }
    }

    @Test
    public void shouldHandleInvalidOpenAPIUrl() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);

        try {
            // when
            httpRequestsPropertiesMatcher.update(
                new OpenAPIDefinition()
                    .withSpecUrlOrPayload("org/mockserver/mock/does_not_exist.json")
                    .withOperationId("listPets")
            );

            // then
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("Unable to load API spec from provided URL or payload"));
        }
    }

    @Test
    public void shouldMatchSingleOperationInOpenAPIJsonUrl() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("listPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldMatchSingleOperationWithPathVariablesInOpenAPIJsonUrl() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("showPetById")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets/12345")
            .withHeader("X-Request-ID", UUID.randomUUID().toString())
            .withHeader("Other-Header", UUID.randomUUID().toString());
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldMatchSingleOperationInOpenAPIJsonSpec() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload(FileReader.readFileFromClassPathOrPath("org/mockserver/mock/openapi_petstore_example.json"))
                .withOperationId("listPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldMatchSingleOperationInOpenAPIYamlUrl() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.yaml")
                .withOperationId("listPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldMatchSingleOperationInOpenAPIYamlSpec() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload(FileReader.readFileFromClassPathOrPath("org/mockserver/mock/openapi_petstore_example.yaml"))
                .withOperationId("listPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldMatchAnyOperationInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, true);
    }

    @Test
    public void shouldNotMatchRequestWithWrongOperationIdInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("showPetById")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), containsInAnyOrder("  expected match against operation" + NEW_LINE +
            NEW_LINE +
            "    showPetById" + NEW_LINE +
            NEW_LINE +
            "   but request matched operation" + NEW_LINE +
            NEW_LINE +
            "    listPets" + NEW_LINE));
        assertThat(matchDifference.getDifferences(OPENAPI), nullValue());
    }

    @Test
    public void shouldNotMatchRequestWithWrongOperationIdWithNullContextInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("showPetById")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(httpRequest);

        // then
        thenMatchesEmptyFieldDifferences(matchDifference, matches, false);
    }

    @Test
    public void shouldNotMatchRequestWithMethodMismatchInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("showPetById")
        ));
        HttpRequest httpRequest = request()
            .withMethod("PUT")
            .withPath("/pets")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder("  PUT operation not allowed on path '/pets'"));
    }

    @Test
    public void shouldNotMatchRequestWithPathMismatchInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("showPetById")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/wrong")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder("  No API path found that matches request '/wrong'"));
    }

    @Test
    public void shouldNotMatchRequestWithHeaderMismatchInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("somePath")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/some/path")
            .withQueryStringParameter("limit", "10");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder("  Header parameter 'X-Request-ID' is required on path '/some/path' but not found in request"));
    }

    @Test
    public void shouldNotMatchRequestWithHeaderAndQueryParameterMismatchInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("somePath")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/some/path")
            .withQueryStringParameter("limit", "not_a_number");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder(
            "  Header parameter 'X-Request-ID' is required on path '/some/path' but not found in request",
            "  Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"
        ));
    }

    @Test
    public void shouldNotMatchRequestWithHeaderAndQueryParameterMismatchInOpenAPIWithoutOperationId() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
        ));
        HttpRequest httpRequest = request()
            .withMethod("GET")
            .withPath("/some/path")
            .withQueryStringParameter("limit", "not_a_number");
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder(
            "  Header parameter 'X-Request-ID' is required on path '/some/path' but not found in request",
            "  Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"
        ));
    }

    @Test
    public void shouldNotMatchRequestWithBodyMismatchWithContentTypeInOpenAPI() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("createPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("POST")
            .withPath("/pets")
            .withHeader("content-type", "application/json")
            .withBody(json("{" + NEW_LINE +
                "    \"id\": \"invalid_id_format\", " + NEW_LINE +
                "    \"name\": \"scruffles\", " + NEW_LINE +
                "    \"tag\": \"dog\"" + NEW_LINE +
                "}"));
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder(
            "  [Path '/id'] Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"
        ));
    }

    @Test
    public void shouldNotMatchRequestWithBodyMismatchWithContentTypeInOpenAPIWithoutOperationID() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
        ));
        HttpRequest httpRequest = request()
            .withMethod("POST")
            .withPath("/pets")
            .withHeader("content-type", "application/json")
            .withBody(json("{" + NEW_LINE +
                "    \"id\": \"invalid_id_format\", " + NEW_LINE +
                "    \"name\": \"scruffles\", " + NEW_LINE +
                "    \"tag\": \"dog\"" + NEW_LINE +
                "}"));
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder(
            "  [Path '/id'] Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"
        ));
    }

    @Test
    public void shouldNotMatchRequestInOpenAPIWithBodyMismatch() {
        // given
        HttpRequestsPropertiesMatcher httpRequestsPropertiesMatcher = new HttpRequestsPropertiesMatcher(mockServerLogger);
        httpRequestsPropertiesMatcher.update(new Expectation(
            new OpenAPIDefinition()
                .withSpecUrlOrPayload("org/mockserver/mock/openapi_petstore_example.json")
                .withOperationId("createPets")
        ));
        HttpRequest httpRequest = request()
            .withMethod("POST")
            .withPath("/pets")
            .withBody(json("{" + NEW_LINE +
                "    \"id\": \"invalid_id_format\", " + NEW_LINE +
                "    \"name\": \"scruffles\", " + NEW_LINE +
                "    \"tag\": \"dog\"" + NEW_LINE +
                "}"));
        MatchDifference matchDifference = new MatchDifference(httpRequest);

        // when
        boolean matches = httpRequestsPropertiesMatcher.matches(matchDifference, httpRequest);

        // then
        assertThat(matches, is(false));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), containsInAnyOrder(
            "  [Path '/id'] Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"
        ));
    }

    private void thenMatchesEmptyFieldDifferences(MatchDifference matchDifference, boolean matches, boolean expected) {
        assertThat(matches, is(expected));
        assertThat(matchDifference.getDifferences(METHOD), nullValue());
        assertThat(matchDifference.getDifferences(PATH), nullValue());
        assertThat(matchDifference.getDifferences(QUERY_PARAMETERS), nullValue());
        assertThat(matchDifference.getDifferences(COOKIES), nullValue());
        assertThat(matchDifference.getDifferences(HEADERS), nullValue());
        assertThat(matchDifference.getDifferences(BODY), nullValue());
        assertThat(matchDifference.getDifferences(SSL_MATCHES), nullValue());
        assertThat(matchDifference.getDifferences(KEEP_ALIVE), nullValue());
        assertThat(matchDifference.getDifferences(OPERATION), nullValue());
        assertThat(matchDifference.getDifferences(OPENAPI), nullValue());
    }

}