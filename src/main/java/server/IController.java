package server;

import model.HttpRequest;
import model.HttpResponse;

public interface IController {
    HttpResponse handle(HttpRequest req);
}
