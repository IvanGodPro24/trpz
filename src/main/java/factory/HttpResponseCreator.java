package factory;

import model.HttpResponse;

public abstract class HttpResponseCreator {
    public abstract HttpResponse createResponse(int statusCode, String body);
}
