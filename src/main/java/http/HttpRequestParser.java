package http;

import model.HttpRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static http.HttpUtils.*;

public class HttpRequestParser {

    private static final int PUSHBACK_BUFFER = 1;

    public static HttpRequest parse(InputStream rawIn) throws IOException {
        InputStream in = (rawIn instanceof PushbackInputStream) ? rawIn : new PushbackInputStream(rawIn, PUSHBACK_BUFFER);

        String requestLine = readLineCRLF(in);
        if (requestLine == null || requestLine.isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        String method = parts.length > 0 ? parts[0] : "GET";
        String url = parts.length > 1 ? parts[1] : "/";
        String version = parts.length > 2 ? parts[2] : "HTTP/1.1";

        Map<String, String> headers = readHeaders(in);

        byte[] bodyBytes = readRequestBodyBytes(in, headers);
        String body = null;
        if (bodyBytes != null) {
            body = bodyBytes.length == 0 ? "" : new String(bodyBytes, StandardCharsets.UTF_8);
        }


        return new HttpRequest(method, url, version, headers, body);
    }
}
