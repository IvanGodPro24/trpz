package p2p;

import model.HttpResponse;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static http.HttpUtils.*;

public class PeerClient {

    public HttpResponse sendGet(String peerAddress, String path) throws IOException, URISyntaxException {
        Target t = resolveTarget(peerAddress, path);

        try (Socket socket = new Socket(t.uri.getHost(), t.port)) {
            socket.setSoTimeout(10_000);
            OutputStream rawOut = socket.getOutputStream();
            InputStream rawIn = toPushback(socket.getInputStream());

            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.US_ASCII));
            w.write("GET " + t.path + " HTTP/1.1\r\n");
            w.write("Host: " + t.hostHeader + "\r\n");
            w.write("Connection: close\r\n");
            w.write("\r\n");
            w.flush();

            return readHttpResponse(rawIn);
        }
    }

    public HttpResponse sendPost(String peerAddress, String path, String body, String contentType) throws IOException, URISyntaxException {
        Target t = resolveTarget(peerAddress, path);

        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String ct = contentType == null ? "application/json; charset=utf-8" : contentType;

        try (Socket socket = new Socket(t.uri.getHost(), t.port)) {
            socket.setSoTimeout(10_000);
            OutputStream rawOut = socket.getOutputStream();
            InputStream rawIn = toPushback(socket.getInputStream());

            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.US_ASCII));

            w.write("POST " + t.path + " HTTP/1.1\r\n");
            w.write("Host: " + t.hostHeader + "\r\n");
            w.write("Connection: close\r\n");
            w.write("Content-Type: " + ct + "\r\n");
            w.write("Content-Length: " + bodyBytes.length + "\r\n");
            w.write("\r\n");
            w.flush();

            if (bodyBytes.length > 0) {
                rawOut.write(bodyBytes);
                rawOut.flush();
            }

            return readHttpResponse(rawIn);
        }
    }

        private record Target(URI uri, String path, int port, String hostHeader) {}


    private Target resolveTarget(String peerAddress, String path) throws URISyntaxException {
        if (path == null || path.isEmpty()) path = "/";
        if (!path.startsWith("/")) path = "/" + path;

        URI uri = peerAddress.contains("://") ? new URI(peerAddress) : new URI("http://" + peerAddress);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        String hostHeader = host + (uri.getPort() == -1 || uri.getPort() == 80 ? "" : ":" + uri.getPort());

        return new Target(uri, path, port, hostHeader);
    }

    private HttpResponse readHttpResponse(InputStream rawIn) throws IOException {
        // read status line
        String statusLine = readLineCRLF(rawIn);
        if (statusLine == null) throw new IOException("No response from peer");

        // приклад: HTTP/1.1 200 OK
        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = 0;
        String statusMsg = "";
        if (statusParts.length >= 2) {
            try {
                statusCode = Integer.parseInt(statusParts[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (statusParts.length >= 3) statusMsg = statusParts[2];

        // headers
        Map<String, String> headers = readHeaders(rawIn);

        // body bytes
        byte[] bodyBytes = readBodyBytes(rawIn, headers);
        String body = bodyBytes == null ? null : new String(bodyBytes, StandardCharsets.UTF_8);

        return new HttpResponse(statusCode, statusMsg, headers, body, bodyBytes);
    }
}