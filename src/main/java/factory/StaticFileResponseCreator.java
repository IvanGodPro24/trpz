package factory;

import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import model.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class StaticFileResponseCreator extends HttpResponseCreator {
    private final String serverName;
    private final String mimeType;

    public StaticFileResponseCreator(String mimeType) {
        this(mimeType, "JavaHTTP/1.0");
    }

    public StaticFileResponseCreator(String mimeType, String serverName) {
        this.mimeType = (mimeType == null || mimeType.isBlank()) ? "application/octet-stream" : mimeType;
        this.serverName = serverName == null ? "JavaHTTP/1.0" : serverName;
    }

    @Override
    public HttpResponse createResponse(int statusCode, String body) {
        IHttpResponseBuilder builder = new HttpResponseBuilder();
        String ct = normalizeContentTypeForText(mimeType);

        return builder
                .setStatusCode(statusCode)
                .setHeader("Server", serverName)
                .setHeader("Content-Type", ct)
                .setHeader("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .setBody(body)
                .build();
    }

    public HttpResponse createResponse(int statusCode, byte[] bodyBytes) {
        IHttpResponseBuilder builder = new HttpResponseBuilder();
        String ct = normalizeContentTypeForText(mimeType);

        return builder
                .setStatusCode(statusCode)
                .setHeader("Server", serverName)
                .setHeader("Content-Type", ct)
                .setHeader("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .setBodyBytes(bodyBytes)
                .build();
    }

    private String normalizeContentTypeForText(String ct) {
        String lower = ct.toLowerCase(Locale.ROOT);
        if (lower.startsWith("text/") || lower.contains("json") || lower.contains("xml") || lower.contains("html")) {
            if (!ct.contains("charset")) ct = ct + "; charset=UTF-8";
        }
        return ct;
    }
}