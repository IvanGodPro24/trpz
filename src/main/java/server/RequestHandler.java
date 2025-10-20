package server;

import composite.HtmlComposite;
import composite.HtmlElement;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.SuccessResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

public class RequestHandler {
    public RequestHandler() {}

    public HttpResponse Handle(HttpRequest req) {
        String url = req.getUrl();
        String body = switch (url) {
            case "/home" -> buildHomePage();
            case "/about" -> buildAboutPage();
            case "/contact" -> buildContactPage();
            default -> "<h1>404 Page Not Found</h1>";
        };

        int statusCode = (url.equals("/home") || url.equals("/about") || url.equals("/contact")) ? 200 : 404;

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

    private String buildContactPage() {
        HtmlComposite html = new HtmlComposite("html");
        HtmlComposite body = new HtmlComposite("body");
        body.add(new HtmlElement("h1", "Contact information"));
        body.add(new HtmlElement("p", "Email: contact@server.com"));
        html.add(body);
        return html.render();
    }
}
