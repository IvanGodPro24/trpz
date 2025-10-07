package builder;

import model.HttpResponse;

public class HttpResponseDirector {
    private final IHttpResponseBuilder builder;

    public HttpResponseDirector(IHttpResponseBuilder builder) {
        this.builder = builder;
    }

    public HttpResponse createSuccessResponse(String body) {
        return builder
                .setStatusCode(200)
                .setHeader("Server", "JavaHTTP/1.0")
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(body)
                .build();
    }

    public HttpResponse createErrorResponse(int code, String body) {
        return builder
                .setStatusCode(code)
                .setHeader("Server", "JavaHTTP/1.0")
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(body)
                .build();
    }
}