package builder;

import model.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseBuilder implements IHttpResponseBuilder {
    private int statusCode;
    private String statusMessage;
    private final Map<String, String> headers = new HashMap<>();
    private String body = "";

    @Override
    public IHttpResponseBuilder setStatusCode(int code) {
        this.statusCode = code;
        switch (code) {
            case 200 -> this.statusMessage = "OK";
            case 404 -> this.statusMessage = "Not Found";
            case 503 -> this.statusMessage = "Service Unavailable";
            default -> this.statusMessage = "Unknown";
        }
        return this;
    }

    @Override
    public IHttpResponseBuilder setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public IHttpResponseBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public HttpResponse build() {
        return new HttpResponse(statusCode, statusMessage, headers, body);
    }
}
