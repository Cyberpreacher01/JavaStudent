import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * StaticFileHandler.java
 * Sends the front-end files (index.html, style.css, app.js) from the web folder
 * to the browser.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path webRoot;

    public StaticFileHandler(String folder) {
        this.webRoot = Paths.get(folder).toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        String requested = exchange.getRequestURI().getPath();
        if (requested.equals("/")) {
            requested = "/index.html";
        }

        // Resolve the file inside the web folder and refuse anything outside it
        // (blocks tricks like /../src/ApiHandler.java)
        Path file = webRoot.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Page not found");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file.toString()));
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    private void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
