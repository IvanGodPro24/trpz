package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HttpRequest(String method, String url, String version, Map<String, String> headers, String body) {
    public HttpRequest(String method, String url) {
        this(method, url, "HTTP/1.1", new LinkedHashMap<>(), null);
    }

    public HttpRequest(String method, String url, String version, Map<String, String> headers, String body) {
        this.method = Objects.requireNonNull(method);
        this.url = Objects.requireNonNull(url);
        this.version = version == null ? "HTTP/1.1" : version;
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
        this.body = body;
    }

    @Override
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public String toString() {
        return method + " " + url + " " + version + " headers=" + headers.size() + " bodyLength=" + (body == null ? 0 : body.length());
    }
}
