package model;

public class HttpRequest {
    private final String method;
    private final String url;

    public HttpRequest(String method, String url) {
        this.method = method;
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
