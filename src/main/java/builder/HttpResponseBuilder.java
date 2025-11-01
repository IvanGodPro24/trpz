package builder;

import model.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseBuilder implements IHttpResponseBuilder {
    private int statusCode;
    private String statusMessage;
    private final Map<String, String> headers = new HashMap<>();
    private String body = null;
    private byte[] bodyBytes = null;

    @Override
    public IHttpResponseBuilder setStatusCode(int code) {
        this.statusCode = code;
        switch (code) {
            case 200 -> this.statusMessage = "OK";
            case 201 -> this.statusMessage = "Created";
            case 400 -> this.statusMessage = "Bad Request";
            case 401 -> this.statusMessage = "Unauthorized";
            case 403 -> this.statusMessage = "Forbidden";
            case 404 -> this.statusMessage = "Not Found";
            case 405 -> this.statusMessage = "Method Not Allowed";
            case 500 -> this.statusMessage = "Internal Server Error";
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
        this.bodyBytes = null;

        if (body != null) {
            int length = body.getBytes(StandardCharsets.UTF_8).length;
            headers.put("Content-Length", String.valueOf(length));
        } else {
            headers.put("Content-Length", "0");
        }

        return this;
    }

    public IHttpResponseBuilder setBodyBytes(byte[] bytes) {
        this.bodyBytes = (bytes == null || bytes.length == 0) ? null : bytes.clone();
        this.body = null;
        headers.put("Content-Length", String.valueOf(bodyBytes == null ? 0 : bodyBytes.length));
        return this;
    }

    @Override
    public HttpResponse build() {
        return new HttpResponse(statusCode, statusMessage, headers, body, bodyBytes);
    }
}
