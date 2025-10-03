package model;

public class HttpResponse {
    private int statusCode;
    private String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return statusCode + " " + body;
    }
}
