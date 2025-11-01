package server;

import model.HttpRequest;
import model.HttpResponse;
import factory.ErrorResponseCreator;
import factory.StaticFileResponseCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class StaticFilesController implements IController {
    private final Path publicRoot;

    public StaticFilesController() {
        this(Path.of("public"));
    }

    public StaticFilesController(Path publicRoot) {
        this.publicRoot = publicRoot;
    }

    @Override
    public HttpResponse handle(HttpRequest req) {
        String url = req.url();

        String rel = url.substring("/static/".length());
        rel = rel.replaceAll("\\.\\./", "");
        rel = rel.replaceFirst("^/+", "");
        if (rel.isEmpty()) rel = "index.html";

        Path file = publicRoot.resolve(rel).normalize();
        if (!file.startsWith(publicRoot.normalize())) {
            return new ErrorResponseCreator().createResponse(403, "<h1>Forbidden</h1>");
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return new ErrorResponseCreator().createResponse(404, "<h1>File not Found</h1>");
        }


        try {
            byte[] bytes = Files.readAllBytes(file);
            String mime = detectMimeByName(file.getFileName().toString());

            StaticFileResponseCreator creator = new StaticFileResponseCreator(mime);

            return creator.createResponse(200, bytes);
        } catch (IOException e) {
            System.err.println("Failed to read static file: " + e.getMessage());
            return new ErrorResponseCreator().createResponse(500, "<h1>Internal Server Error</h1>");
        }
    }

    private static String detectMimeByName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".html") || n.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (n.endsWith(".css")) return "text/css; charset=UTF-8";
        if (n.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (n.endsWith(".json")) return "application/json; charset=UTF-8";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".svg")) return "image/svg+xml; charset=UTF-8";
        if (n.endsWith(".txt")) return "text/plain; charset=UTF-8";
        return "application/octet-stream";
    }
}