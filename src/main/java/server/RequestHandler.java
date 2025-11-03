package server;

import db.RequestsRepository;
import model.HttpRequest;
import model.HttpResponse;
import p2p.PeerNetwork;

public class RequestHandler {
    private final Router router;

    public RequestHandler(Statistics stats, RequestsRepository requestsRepo, PeerNetwork peerNetwork) {
        this.router = new Router();

        // templates
        this.router.registerPrefix("GET", "/templates/", new TemplatesController());

        // static files
        this.router.registerPrefix("GET", "/static/", new StaticFilesController());

        // sync stats
        SyncController sync = new SyncController(stats);
        this.router.registerExact("GET", "/sync/stats", sync);
        this.router.registerExact("POST", "/sync/stats", sync);

        if (peerNetwork != null) {
            PeersController pc = new PeersController(peerNetwork);
            this.router.registerExact("GET", "/peers", pc);
            this.router.registerExact("POST", "/peers", pc);
            this.router.registerExact("DELETE", "/peers", pc);
        }

        // contact
        ContactController contact = new ContactController(requestsRepo);
        this.router.registerExact("GET", "/contact", contact);
        this.router.registerExact("POST", "/contact", contact);
        this.router.registerExact("PUT", "/contact", contact);
        this.router.registerExact("DELETE", "/contact", contact);

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
