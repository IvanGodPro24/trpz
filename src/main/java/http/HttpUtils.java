package http;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Утилітні методи для роботи з HTTP-потоками:
 * - readLineCRLF: читає рядок до CRLF або LF з InputStream (повертає ISO-8859-1 рядок)
 * - readExactly: читає точно N байт
 * - readChunked: читає chunked-encoded body (повертає byte[])
 * - toPushback: обгортає InputStream в PushbackInputStream якщо потрібно
 * - getHeaderIgnoreCase: пошук заголовка у Map незалежно від регістру
 * Використовувати ці методи і у PeerClient, і у HttpRequestParser.
 */
public final class HttpUtils {
    private static final int DEFAULT_PUSHBACK = 1;

    private HttpUtils() {}

    public static InputStream toPushback(InputStream in) {
        if (in instanceof PushbackInputStream) return in;
        return new PushbackInputStream(in, DEFAULT_PUSHBACK);
    }

    /**
     * Читає рядок до CRLF або LF з InputStream. Повертає null на EOF.
     * Декодує байти як ISO_8859_1 (RFC для заголовків).
     */
    public static String readLineCRLF(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b == '\r') {
                int nxt = in.read();
                if (nxt != '\n' && nxt != -1) {
                    InputStream pis = toPushback(in);
                    if (pis instanceof PushbackInputStream) ((PushbackInputStream) pis).unread(nxt);
                }
                break;
            }
            baos.write(b);
        }
        if (b == -1 && baos.size() == 0) return null;
        return baos.toString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Читає точно len байт з InputStream. Кидає IOException при EOF до завершення.
     */
    public static byte[] readExactly(InputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int pos = 0;
        while (pos < len) {
            int r = in.read(buf, pos, len - pos);
            if (r == -1) throw new IOException("Unexpected end of stream while reading body");
            pos += r;
        }
        return buf;
    }

    /**
     * Прочитати chunked-encoded body з InputStream. Повертає масив байт (не строку).
     * Реалізація читає chunk-size lines використовуючи readLineCRLF.
     */
    public static byte[] readChunked(InputStream inRaw) throws IOException {
        InputStream in = toPushback(inRaw);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (true) {
            String sizeLine = readLineCRLF(in);
            if (sizeLine == null) throw new IOException("Unexpected EOF reading chunk size");
            sizeLine = sizeLine.trim();
            int semi = sizeLine.indexOf(';');
            if (semi > 0) sizeLine = sizeLine.substring(0, semi).trim();
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeLine.isEmpty() ? "0" : sizeLine, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + sizeLine);
            }
            if (chunkSize == 0) {
                // trailing headers (optional) read until blank line
                String line;
                while ((line = readLineCRLF(in)) != null && !line.isEmpty()) {
                    // ignore for now
                }
                break;
            }

            byte[] chunk = readExactly(in, chunkSize);
            out.write(chunk);

            // after chunk data there should be CRLF; consume it (may be \r\n or \n)
            int c1 = in.read();
            if (c1 == '\r') {
                int c2 = in.read();
                if (c2 != '\n' && c2 != -1) {
                    if (in instanceof PushbackInputStream) ((PushbackInputStream) in).unread(c2);
                }
            } else if (c1 == '\n') {
                // ok
            } else if (c1 != -1) {
                if (in instanceof PushbackInputStream) ((PushbackInputStream) in).unread(c1);
            }
        }
        return out.toByteArray();
    }

    public static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null) return null;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    public static Map<String,String> readHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLineCRLF(in)) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                headers.put(name, val);
            }
        }
        return headers;
    }


    public static byte[] readBodyBytes(InputStream in, Map<String,String> headers) throws IOException {
        String cl = getHeaderIgnoreCase(headers, "Content-Length");
        String te = getHeaderIgnoreCase(headers, "Transfer-Encoding");
        if (cl != null) {
            int len = 0;
            try {
                len = Integer.parseInt(cl.trim());
            } catch (NumberFormatException ignored) {}
            if (len > 0) {
                return readExactly(in, len);
            } else {
                return new byte[0];
            }
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            return readChunked(in);
        } else {
            // read until EOF (Connection: close)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int r;
            while ((r = in.read(tmp)) != -1) {
                baos.write(tmp, 0, r);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Читати тіло **HTTP-запиту** (серверна сторона).
     * Читає тільки коли:
     *  - є Content-Length > 0 (тоді читає рівно len байт)
     *  - або Transfer-Encoding: chunked (тоді читає chunked)
     * В іншому випадку повертає null (означає — тіла немає / не читати до EOF).
     */
    public static byte[] readRequestBodyBytes(InputStream in, Map<String,String> headers) throws IOException {
        String cl = getHeaderIgnoreCase(headers, "Content-Length");
        String te = getHeaderIgnoreCase(headers, "Transfer-Encoding");
        if (cl != null) {
            int len = 0;
            try {
                len = Integer.parseInt(cl.trim());
            } catch (NumberFormatException ignored) {}
            if (len > 0) {
                return readExactly(in, len);
            } else {
                return new byte[0];
            }
        } else if (te != null && te.equalsIgnoreCase("chunked")) {
            return readChunked(in);
        } else {
            // No explicit length and not chunked — for requests we must NOT read until EOF.
            return null;
        }
    }

}
