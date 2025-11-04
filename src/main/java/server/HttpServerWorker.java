package server;

import factory.ErrorResponseCreator;
import http.HttpRequestParser;
import http.HttpResponseSerializer;
import model.HttpRequest;
import model.HttpResponse;
import events.EventBus;
import events.RequestEvent;
import events.ResponseEvent;
import io.github.cdimascio.dotenv.Dotenv;

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

    private static final int FALLBACK_KEEP_ALIVE_TIMEOUT_MS = 10_000;
    private static final int FALLBACK_MAX_REQUESTS = 100;

    private static final String ENV_KEEP_ALIVE_MS = "KEEP_ALIVE_TIMEOUT_MS";
    private static final String ENV_MAX_REQUESTS = "MAX_REQUESTS_PER_CONNECTION";

    private static final int DEFAULT_KEEP_ALIVE_TIMEOUT_MS;
    private static final int DEFAULT_MAX_REQUESTS;

    static {
        int keepAlive = FALLBACK_KEEP_ALIVE_TIMEOUT_MS;
        int maxReq = FALLBACK_MAX_REQUESTS;

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String ka = dotenv.get(ENV_KEEP_ALIVE_MS);
            String mr = dotenv.get(ENV_MAX_REQUESTS);

            if (ka != null && !ka.isBlank()) {
                try {
                    int parsed = Integer.parseInt(ka.trim());
                    if (parsed > 0) keepAlive = parsed;
                    else System.err.println("Invalid " + ENV_KEEP_ALIVE_MS + " , using fallback " + FALLBACK_KEEP_ALIVE_TIMEOUT_MS);
                } catch (NumberFormatException nfe) {
                    System.err.println("Failed to parse " + ENV_KEEP_ALIVE_MS + ": " + nfe.getMessage() + " — using fallback " + FALLBACK_KEEP_ALIVE_TIMEOUT_MS);
                }
            } else {
                System.out.println(ENV_KEEP_ALIVE_MS + " not set, using default " + FALLBACK_KEEP_ALIVE_TIMEOUT_MS);
            }

            if (mr != null && !mr.isBlank()) {
                try {
                    int parsed = Integer.parseInt(mr.trim());
                    if (parsed > 0) maxReq = parsed;
                    else System.err.println("Invalid " + ENV_MAX_REQUESTS + ", using fallback " + FALLBACK_MAX_REQUESTS);
                } catch (NumberFormatException nfe) {
                    System.err.println("Failed to parse " + ENV_MAX_REQUESTS + ": " + nfe.getMessage() + " — using fallback " + FALLBACK_MAX_REQUESTS);
                }
            } else {
                System.out.println(ENV_MAX_REQUESTS + " not set, using default " + FALLBACK_MAX_REQUESTS);
            }
        } catch (Throwable t) {
            System.err.println("Failed to load .env for HttpServerWorker: " + t.getMessage() + " — using fallbacks");
        }

        DEFAULT_KEEP_ALIVE_TIMEOUT_MS = keepAlive;
        DEFAULT_MAX_REQUESTS = maxReq;
    }

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

                EventBus bus = EventBus.getInstance();

                try {
                    bus.publish(new RequestEvent(request));
                } catch (Throwable t) {
                    System.err.println("EventBus publish Request failed: " + t.getMessage());
                }

                long start = System.nanoTime();
                HttpResponse response;
                try {
                    response = server.HandleRequest(request);
                } catch (Throwable t) {
                    ErrorResponseCreator err = new ErrorResponseCreator();
                    response = err.createResponse(500, "<h1>Internal Server Error</h1>");
                }
                long duration = Math.max(0L, System.nanoTime() - start);

                if (response == null) {
                    ErrorResponseCreator err = new ErrorResponseCreator();
                    response = err.createResponse(500, "<h1>Internal Server Error</h1>");
                }

                try {
                    bus.publish(new ResponseEvent(request, response, duration));
                } catch (Throwable t) {
                    System.err.println("EventBus publish Response failed: " + t.getMessage());
                }

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

                try {
                    byte[] respBytes = HttpResponseSerializer.serialize(responseToSend);
                    out.write(respBytes);
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Failed to serialize response: " + e.getMessage());
                    break;
                }


                if (!willKeepAlive) {
                    break;
                }

                clientSocket.setSoTimeout(keepAliveTimeout);
            }
        } catch (SocketTimeoutException ste) {
            // Таймаут очікування наступного запиту — закриваємо з'єднання
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}