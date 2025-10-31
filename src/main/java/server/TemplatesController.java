package server;

import chtml.ChtmlEngine;
import factory.ErrorResponseCreator;
import factory.HttpResponseCreator;
import factory.TemplateResponseCreator;
import model.HttpRequest;
import model.HttpResponse;

import java.util.Map;

import static http.HttpUtils.parseQueryToContext;
import static http.HttpUtils.escapeHtml;

public class TemplatesController implements IController {
    @Override
    public HttpResponse handle(HttpRequest req) {
        String raw = req.url();
        String tplNameRaw = raw.substring("/templates/".length());
        int qidx = tplNameRaw.indexOf('?');
        String tplPart = qidx >= 0 ? tplNameRaw.substring(0, qidx) : tplNameRaw;
        String tplName = tplPart.replaceAll("\\.\\./", "").replaceAll("^/+", "");
        try {
            tplName = java.net.URLDecoder.decode(tplName, java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException ignored) {}

        try {
            Map<String, Object> ctx = parseQueryToContext(req);
            ctx.putIfAbsent("name", "Guest");
            ctx.putIfAbsent("count", 0);
            String out = ChtmlEngine.render(tplName, ctx);

            HttpResponseCreator creator = new TemplateResponseCreator();
            return creator.createResponse(200, out);
        } catch (Exception e) {
            ErrorResponseCreator err = new ErrorResponseCreator();
            String msg = "<h1>Template render error</h1><pre>" + escapeHtml(e.getMessage()) + "</pre>";
            return err.createResponse(500, msg);
        }
    }
}