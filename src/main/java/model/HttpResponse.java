package model;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public record HttpResponse(int statusCode, String statusMessage, Map<String, String> headers, String body, byte[] bodyBytes) {
    public HttpResponse(int statusCode, String statusMessage, Map<String, String> headers, String body, byte[] bodyBytes) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
        this.body = body;
        this.bodyBytes = bodyBytes;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        for (var entry : headers.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        response.append("\r\n");

        if (body != null) {
            response.append(body);
        } else if (bodyBytes != null) {
            response.append(new String(bodyBytes, StandardCharsets.UTF_8));
        }

        return response.toString();
    }
}