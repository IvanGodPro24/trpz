package server;

import db.RequestsRepository;
import model.HttpRequest;
import model.HttpResponse;

public class RequestHandler {
    private final Router router;

    public RequestHandler(Statistics stats, RequestsRepository requestsRepo) {
        this.router = new Router();

        // templates
        this.router.registerPrefix("GET", "/templates/", new TemplatesController());

        // sync stats
        SyncController sync = new SyncController(stats);
        this.router.registerExact("GET", "/sync/stats", sync);
        this.router.registerExact("POST", "/sync/stats", sync);

        // contact
        ContactController contact = new ContactController(requestsRepo);
        this.router.registerExact("GET", "/contact", contact);
        this.router.registerExact("POST", "/contact", contact);

        // static pages
        StaticPagesController staticPages = new StaticPagesController(stats);
        this.router.registerExact("GET", "/home", staticPages);
        this.router.registerExact("GET", "/about", staticPages);
        this.router.registerExact("GET", "/stats", staticPages);
    }

    public HttpResponse Handle(HttpRequest req) {
        return router.route(req);
    }
}
