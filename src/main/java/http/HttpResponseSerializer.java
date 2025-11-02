package http;

import model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpResponseSerializer {
    private HttpResponseSerializer() {}

    public static byte[] serialize(HttpResponse resp) throws IOException {
        if (resp == null) return new byte[0];

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] bodyBytes = resp.bodyBytes();
        if (bodyBytes == null) {
            String body = resp.body();
            bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        }

        StringBuilder sb = new StringBuilder();

        String statusMsg = resp.statusMessage() == null ? "" : resp.statusMessage();
        sb.append("HTTP/1.1 ").append(resp.statusCode()).append(" ").append(statusMsg).append("\r\n");

        boolean hasContentLength = false;
        Map<String, String> headers = resp.headers();
        if (headers != null) {
            for (var e : headers.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                if (k == null) continue;
                if (k.equalsIgnoreCase("Content-Length")) hasContentLength = true;
                sb.append(k).append(": ").append(v).append("\r\n");
            }
        }

        if (!hasContentLength) {
            sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        }

        sb.append("\r\n");

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        if (bodyBytes.length > 0) out.write(bodyBytes);

        return out.toByteArray();
    }
}
