package server;

import composite.HtmlComposite;
import composite.HtmlElement;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.SuccessResponseCreator;
import model.HttpResponse;

public class StaticPagesController implements IController {
    private final Statistics stats;

    public StaticPagesController(Statistics stats) {
        this.stats = stats;
    }

    @Override
    public HttpResponse handle(model.HttpRequest req) {
        String url = req.url();

        String body = switch (url) {
            case "/home" -> buildHomePage();
            case "/about" -> buildAboutPage();
            case "/stats" -> buildStatsPage();
            default -> "<h1>404 Page Not Found</h1>";
        };

        int statusCode = (url.equals("/home") || url.equals("/about") || url.equals("/stats")) ? 200 : 404;

        HttpResponseCreator creator = (statusCode == 200)
                ? new SuccessResponseCreator()
                : new ErrorResponseCreator();

        return creator.createResponse(statusCode, body);
    }

    private String buildHomePage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Welcome to Home!"));
        body.add(new HtmlElement("p", "This page is generated using the Composite pattern."));
        html.add(body);
        return html.render();
    }

    private String buildAboutPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "About Us"));
        body.add(new HtmlElement("p", "This page demonstrates Composite usage for dynamic HTML."));
        html.add(body);
        return html.render();
    }

    private String buildStatsPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Server statistics"));
        body.add(new HtmlElement("p", "Total requests: " + stats.getTotalRequests()));
        html.add(body);
        return html.render();
    }
}
