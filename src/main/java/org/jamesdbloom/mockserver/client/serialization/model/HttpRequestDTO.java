package org.jamesdbloom.mockserver.client.serialization.model;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jamesdbloom.mockserver.model.*;

import java.util.List;

/**
 * @author jamesdbloom
 */
public class HttpRequestDTO extends ModelObject {
    private String path;
    private String body;
    private List<HeaderDTO> headers;
    private List<CookieDTO> cookies;
    private List<ParameterDTO> queryParameters;
    private List<ParameterDTO> bodyParameters;

    public HttpRequestDTO(HttpRequest httpRequest) {
        path = httpRequest.getPath();
        body = httpRequest.getBody();
        headers = Lists.transform(httpRequest.getHeaders(), new Function<Header, HeaderDTO>() {
            public HeaderDTO apply(Header header) {
                return new HeaderDTO(header);
            }
        });
        cookies = Lists.transform(httpRequest.getCookies(), new Function<Cookie, CookieDTO>() {
            public CookieDTO apply(Cookie cookie) {
                return new CookieDTO(cookie);
            }
        });
        queryParameters = Lists.transform(httpRequest.getQueryParameters(), new Function<Parameter, ParameterDTO>() {
            public ParameterDTO apply(Parameter parameter) {
                return new ParameterDTO(parameter);
            }
        });
        bodyParameters = Lists.transform(httpRequest.getBodyParameters(), new Function<Parameter, ParameterDTO>() {
            public ParameterDTO apply(Parameter parameter) {
                return new ParameterDTO(parameter);
            }
        });
    }

    public HttpRequestDTO() {
    }

    public String getPath() {
        return path;
    }

    public HttpRequestDTO setPath(String path) {
        this.path = path;
        return this;
    }

    public String getBody() {
        return body;
    }

    public HttpRequestDTO setBody(String body) {
        this.body = body;
        return this;
    }

    public List<HeaderDTO> getHeaders() {
        return headers;
    }

    public HttpRequestDTO setHeaders(List<HeaderDTO> headers) {
        this.headers = headers;
        return this;
    }

    public List<ParameterDTO> getQueryParameters() {
        return queryParameters;
    }

    public HttpRequestDTO setQueryParameters(List<ParameterDTO> queryParameters) {
        this.queryParameters = queryParameters;
        return this;
    }

    public List<ParameterDTO> getBodyParameters() {
        return bodyParameters;
    }

    public HttpRequestDTO setBodyParameters(List<ParameterDTO> bodyParameters) {
        this.bodyParameters = bodyParameters;
        return this;
    }

    public List<CookieDTO> getCookies() {
        return cookies;
    }

    public HttpRequestDTO setCookies(List<CookieDTO> cookies) {
        this.cookies = cookies;
        return this;
    }
}
