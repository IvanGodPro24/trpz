package builder;

import model.HttpResponse;

public interface IHttpResponseBuilder {
    IHttpResponseBuilder setStatusCode(int code);
    IHttpResponseBuilder setHeader(String key, String value);
    IHttpResponseBuilder setBody(String body);
    IHttpResponseBuilder setBodyBytes(byte[] bodyBytes);
    HttpResponse build();
}