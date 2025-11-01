package server;

import factory.ErrorResponseCreator;
import http.HttpRequestParser;
import model.HttpRequest;
import model.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static http.HttpUtils.getHeaderIgnoreCase;

public class HttpServerWorker implements Runnable {
    private final Socket clientSocket;
    private final HttpServer server;

    private static final int DEFAULT_KEEP_ALIVE_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_MAX_REQUESTS = 100;

    public HttpServerWorker(Socket clientSocket, HttpServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        int requestsHandled = 0;
        int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT_MS;
        int maxRequests = DEFAULT_MAX_REQUESTS;

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            clientSocket.setSoTimeout(keepAliveTimeout);

            while (requestsHandled < maxRequests && !clientSocket.isClosed()) {
                HttpRequest request;
                try {
                    request = HttpRequestParser.parse(in);
                } catch (SocketTimeoutException ste) {
                    break;
                }

                if (request == null) {
                    break;
                }

                requestsHandled++;

                HttpResponse response = server.HandleRequest(request);
                if (response == null) {
                    ErrorResponseCreator err = new ErrorResponseCreator();
                    response = err.createResponse(500, "<h1>Internal Server Error</h1>");
                }

                // Визначаємо політику keep-alive по запиту та версії HTTP
                String reqConnHeader = getHeaderIgnoreCase(request.headers(), "Connection");
                String reqVersion = request.version() == null ? "HTTP/1.1" : request.version();

                boolean clientWantsClose = false;
                boolean clientWantsKeepAlive = false;

                if (reqConnHeader != null) {
                    if (reqConnHeader.equalsIgnoreCase("close")) clientWantsClose = true;
                    if (reqConnHeader.equalsIgnoreCase("keep-alive")) clientWantsKeepAlive = true;
                }

                boolean defaultPersistent = reqVersion.equalsIgnoreCase("HTTP/1.1");

                boolean willKeepAlive;
                if (clientWantsClose) {
                    willKeepAlive = false;
                } else if (clientWantsKeepAlive) {
                    willKeepAlive = true;
                } else {
                    willKeepAlive = defaultPersistent;
                }

                if (requestsHandled >= maxRequests) {
                    willKeepAlive = false;
                }

                Map<String, String> newHeaders = new HashMap<>();
                if (response.headers() != null) {
                    newHeaders.putAll(response.headers());
                }

                if (willKeepAlive) {
                    newHeaders.put("Connection", "keep-alive");
                    int timeoutSeconds = keepAliveTimeout / 1000;
                    newHeaders.put("Keep-Alive", "timeout=" + timeoutSeconds + ", max=" + (maxRequests - requestsHandled));
                } else {
                    newHeaders.put("Connection", "close");
                }

                if (!newHeaders.containsKey("Content-Length")) {
                    int len;
                    if (response.bodyBytes() != null) {
                        len = response.bodyBytes().length;
                    } else if (response.body() != null) {
                        len = response.body().getBytes(StandardCharsets.UTF_8).length;
                    } else {
                        len = 0;
                    }
                    newHeaders.put("Content-Length", String.valueOf(len));
                }

                HttpResponse responseToSend = new HttpResponse(
                        response.statusCode(),
                        response.statusMessage(),
                        newHeaders,
                        response.body(),
                        response.bodyBytes()
                );

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("HTTP/1.1 ").append(responseToSend.statusCode())
                        .append(" ").append(responseToSend.statusMessage()).append("\r\n");
                for (var e : responseToSend.headers().entrySet()) {
                    headerBuilder.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
                }
                headerBuilder.append("\r\n");

                byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.UTF_8);
                out.write(headerBytes);

                if (responseToSend.bodyBytes() != null) {
                    out.write(responseToSend.bodyBytes());
                } else {
                    String bodyText = responseToSend.body();
                    if (bodyText != null && !bodyText.isEmpty()) {
                        out.write(bodyText.getBytes(StandardCharsets.UTF_8));
                    }
                }

                out.flush();

                if (!willKeepAlive) {
                    break;
                }

                clientSocket.setSoTimeout(keepAliveTimeout);
            }
        } catch (SocketTimeoutException ste) {
            // Таймаут очікування наступного запиту — просто закриваємо з'єднання
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}