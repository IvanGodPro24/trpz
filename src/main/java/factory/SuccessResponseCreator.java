package factory;

import builder.HttpResponseBuilder;
import builder.IHttpResponseBuilder;
import model.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SuccessResponseCreator extends HttpResponseCreator {
    @Override
    public HttpResponse createResponse(int statusCode, String body) {
        IHttpResponseBuilder builder = new HttpResponseBuilder();

        return builder
                .setStatusCode(statusCode)
                .setHeader("Server", "JavaHTTP/1.0")
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setHeader("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .setBody(body)
                .build();
    }
}