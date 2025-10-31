package factory;

import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import model.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TemplateResponseCreator extends HttpResponseCreator {
    private final String serverName;

    public TemplateResponseCreator() {
        this("JavaHTTP/1.0");
    }

    public TemplateResponseCreator(String serverName) {
        this.serverName = serverName == null ? "JavaHTTP/1.0" : serverName;
    }

    @Override
    public HttpResponse createResponse(int statusCode, String body) {
        IHttpResponseBuilder builder = new HttpResponseBuilder();

        return builder
                .setStatusCode(statusCode)
                .setHeader("Server", serverName)
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setHeader("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .setBody(body)
                .build();
    }
}
