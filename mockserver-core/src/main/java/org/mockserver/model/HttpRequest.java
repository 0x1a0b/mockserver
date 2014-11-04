package org.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mockserver.client.serialization.ObjectMapperFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author jamesdbloom
 */
public class HttpRequest extends EqualsHashCodeToString {
    private String method = "";
    private String url = "";
    private String path = "";
    private Map<String, Parameter> queryStringParameters = new LinkedHashMap<String, Parameter>();
    private Body body = null;
    private byte[] rawBodyBytes = null;
    private Map<String, Header> headers = new LinkedHashMap<String, Header>();
    private Map<String, Cookie> cookies = new LinkedHashMap<String, Cookie>();

    public HttpRequest() {
    }

    public static HttpRequest request() {
        return new HttpRequest();
    }

    /**
     * The HTTP method to match on such as "GET" or "POST"
     *
     * @param method the HTTP method such as "GET" or "POST"
     */
    public HttpRequest withMethod(String method) {
        this.method = method;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public String getURL() {
        return url;
    }

    /**
     * The URL to match on such as "http://localhost:9999/some_mocked_path" regex values are also supported
     * such as ".*some_mocked_path", see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
     * for full details of the supported regex syntax
     *
     * @param url the full URL such as "http://localhost:9999/some_mocked_path" or a regex
     */
    public HttpRequest withURL(String url) {
        this.url = url;
        return this;
    }

    public String getPath() {
        return path;
    }

    /**
     * The path to match on such as "/some_mocked_path" any servlet context path is ignored for matching and should not be specified here
     * regex values are also supported such as ".*_path", see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
     * for full details of the supported regex syntax
     *
     * @param path the path such as "/some_mocked_path" or a regex
     */
    public HttpRequest withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * The query string parameters to match on as a list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(List<Parameter> parameters) {
        this.queryStringParameters.clear();
        for (Parameter parameter : parameters) {
            withQueryStringParameter(parameter);
        }
        return this;
    }

    /**
     * The query string parameters to match on as a varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(Parameter... parameters) {
        return withQueryStringParameters(Arrays.asList(parameters));
    }

    /**
     * The query string parameters to match on as a Map<String, List<String>> where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the Map<String, List<String>> object where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(Map<String, List<String>> parameters) {
        this.queryStringParameters.clear();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                withQueryStringParameter(new Parameter(name, value));
            }
        }
        return this;
    }

    /**
     * Adds one query string parameter to match on as a Parameter object where the parameter values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameter the Parameter object which can have a values list of strings or regular expressions
     */
    public HttpRequest withQueryStringParameter(Parameter parameter) {
        if (this.queryStringParameters.containsKey(parameter.getName())) {
            this.queryStringParameters.get(parameter.getName()).addValues(parameter.getValues());
        } else {
            this.queryStringParameters.put(parameter.getName(), parameter);
        }
        return this;
    }

    public List<Parameter> getQueryStringParameters() {
        return new ArrayList<Parameter>(queryStringParameters.values());
    }

    public Body getBody() {
        return body;
    }

    public byte[] getRawBodyBytes() {
        return rawBodyBytes;
    }

    public void setRawBodyBytes(byte[] bodyBytes) {
        this.rawBodyBytes = bodyBytes;
    }

    /**
     * The body to match on such as "this is an exact string body" or a json expression such as "{username: 'foo', password: 'bar'}"
     * or a regex (see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     * or an XPath expression which returns one or more values or evaluates to true (see http://saxon.sourceforge.net/saxon6.5.3/expressions.html)
     *
     * @param body the body on such as "this is an exact string body"
     *             or a regex such as "username[a-z]{4}"
     *             or a json expression such as "{username: 'foo', password: 'bar'}"
     *             or an XPath such as "/element[key = 'some_key' and value = 'some_value']"
     */
    public HttpRequest withBody(String body) {
        this.body = new StringBody(body, Body.Type.STRING);
        return this;
    }

    /**
     * The body match rules on such as using one of the Body subclasses as follows:
     *
     * exact string match:
     *   - exact("this is an exact string body");
     *
     *   or
     *
     *   - new StringBody("this is an exact string body")
     *
     * exact match:
     *   - regex("username[a-z]{4}");
     *
     *   or
     *
     *   - new StringBody("username[a-z]{4}", Body.Type.REGEX);
     *
     * json match:
     *   - json("{username: 'foo', password: 'bar'}");
     *
     *   or
     *
     *   - new StringBody("{username: 'foo', password: 'bar'}", Body.Type.JSON);
     *
     * xpath match:
     *   - xpath("/element[key = 'some_key' and value = 'some_value']");
     *
     *   or
     *
     *   - new StringBody("/element[key = 'some_key' and value = 'some_value']", Body.Type.XPATH);
     *
     * body parameter match:
     *   - params(
     *             param("name_one", "value_one_one", "value_one_two")
     *             param("name_two", "value_two")
     *     );
     *
     *   or
     *
     *   - new ParameterBody(
     *             new Parameter("name_one", "value_one_one", "value_one_two")
     *             new Parameter("name_two", "value_two")
     *     );
     *
     * binary match:
     *   - binary(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
     *
     *   or
     *
     *   - new BinaryBody(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
     *
     * for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
     * for more detail of XPath syntax see http://saxon.sourceforge.net/saxon6.5.3/expressions.html
     *
     * @param body an instance of one of the Body subclasses including StringBody, ParameterBody or BinaryBody
     */
    public HttpRequest withBody(Body body) {
        if (body instanceof BinaryBody) {
            setRawBodyBytes(((BinaryBody) body).getValue());
        }
        this.body = body;
        return this;
    }

    /**
     * The headers to match on as a list of Header objects where the values or keys of each header can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param headers the list of Header objects where the values or keys of each header can be either a string or a regex
     */
    public HttpRequest withHeaders(List<Header> headers) {
        this.headers.clear();
        for (Header header : headers) {
            withHeader(header);
        }
        return this;
    }

    /**
     * The headers to match on as a varags of Header objects where the values or keys of each header can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param headers the varags of Header objects where the values or keys of each header can be either a string or a regex
     */
    public HttpRequest withHeaders(Header... headers) {
        withHeaders(Arrays.asList(headers));
        return this;
    }

    /**
     * Adds one header to match on as a Header object where the header values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param header the Header object which can have a values list of strings or regular expressions
     */
    public HttpRequest withHeader(Header header) {
        if (this.headers.containsKey(header.getName())) {
            this.headers.get(header.getName()).addValues(header.getValues());
        } else {
            this.headers.put(header.getName(), header);
        }
        return this;
    }

    /**
     * Adds one header to match on as a Header object where the header values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param header the Header object which can have a values list of strings or regular expressions
     */
    public HttpRequest replaceHeader(Header header) {
        for (String key : new HashSet<String>(this.headers.keySet())) {
            if (header.getName().equalsIgnoreCase(key)) {
                this.headers.remove(key);
            }
        }
        this.headers.put(header.getName(), header);
        return this;
    }

    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers.values());
    }

    /**
     * Returns true if a header with the specified name has been added
     *
     * @param name the Header name
     * @return true if a header has been added with that name otherwise false
     */
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    /**
     * The cookies to match on as a list of Cookie objects where the values or keys of each cookie can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookies the list of Cookie objects where the values or keys of each cookie can be either a string or a regex
     */
    public HttpRequest withCookies(List<Cookie> cookies) {
        this.cookies.clear();
        for (Cookie cookie : cookies) {
            withCookie(cookie);
        }
        return this;
    }

    /**
     * The cookies to match on as a varags Cookie objects where the values or keys of each cookie can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookies the varags Cookie objects where the values or keys of each cookie can be either a string or a regex
     */
    public HttpRequest withCookies(Cookie... cookies) {
        withCookies(Arrays.asList(cookies));
        return this;
    }

    /**
     * Adds one cookie to match on as a Cookie object where the cookie values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookie the Cookie object which can have a values list of strings or regular expressions
     */
    public HttpRequest withCookie(Cookie cookie) {
        if (this.cookies.containsKey(cookie.getName())) {
            this.cookies.get(cookie.getName()).addValues(cookie.getValues());
        } else {
            this.cookies.put(cookie.getName(), cookie);
        }
        return this;
    }

    public List<Cookie> getCookies() {
        return new ArrayList<Cookie>(cookies.values());
    }

    @JsonIgnore
    public int getPort() {
        if (this.url != null) {
            URL url = null;
            try {
                url = new URL(this.url);
            } catch (MalformedURLException murle) {
                logger.debug("MalformedURLException parsing uri [" + this.url + "]", murle);
            }
            if (url != null && url.getPort() != -1) {
                return url.getPort();
            } else {
                if (this.url.startsWith("https")) {
                    return 443;
                } else {
                    return 80;
                }
            }
        }
        return 80;
    }

    @Override
    public String toString() {
        try {
            return ObjectMapperFactory
                    .createObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
