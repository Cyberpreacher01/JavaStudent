import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ApiHandler.java
 * Receives requests from the browser at /api/... , calls the matching
 * CourseManager method, and sends back a JSON reply.
 *
 *   GET    /api/courses              -> View All Courses
 *   POST   /api/courses              -> Add Course        (code, title, units)
 *   PUT    /api/courses/{code}       -> Edit Course        (code, title, units)
 *   DELETE /api/courses/{code}       -> Delete Course
 *   GET    /api/courses/search?code= -> Search Course by Code
 *   GET  /api/total                -> Compute Total Units
 *   POST /api/save                 -> Save to File      (filename)
 *   POST /api/load                 -> Load from File    (filename)
 *   POST /api/exit                 -> Exit Program
 */
public class ApiHandler implements HttpHandler {

    private static final String DEFAULT_FILE = "courses.json";
    private static final int MAX_BODY_BYTES = 10_000;

    private final CourseManager manager;
    private final HttpServer server;

    public ApiHandler(CourseManager manager, HttpServer server) {
        this.manager = manager;
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            // Decision-making: choose the operation from the path and method
            if (path.equals("/api/courses")) {
                if (method.equals("GET")) {
                    viewAllCourses(exchange);
                } else if (method.equals("POST")) {
                    addCourse(exchange);
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } else if (path.equals("/api/courses/search") && method.equals("GET")) {
                searchCourse(exchange);
            } else if (path.startsWith("/api/courses/")) {
                String code = URLDecoder.decode(path.substring("/api/courses/".length()), StandardCharsets.UTF_8);
                if (method.equals("PUT")) {
                    editCourse(exchange, code);
                } else if (method.equals("DELETE")) {
                    deleteCourse(exchange, code);
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } else if (path.equals("/api/total") && method.equals("GET")) {
                computeTotal(exchange);
            } else if (path.equals("/api/save") && method.equals("POST")) {
                saveCourses(exchange);
            } else if (path.equals("/api/load") && method.equals("POST")) {
                loadCourses(exchange);
            } else if (path.equals("/api/exit") && method.equals("POST")) {
                exitProgram(exchange);
            } else {
                sendJson(exchange, 404, JsonUtil.response(false, "Unknown request: " + method + " " + path, null));
            }
        } catch (IllegalArgumentException e) {
            // Invalid input from the user (bad code, units, file name, ...)
            sendJson(exchange, 400, JsonUtil.response(false, e.getMessage(), null));
        } catch (IOException e) {
            // File problems
            sendJson(exchange, 500, JsonUtil.response(false, e.getMessage(), null));
        } catch (Exception e) {
            // Safety net: the server keeps running whatever happens
            sendJson(exchange, 500, JsonUtil.response(false, "Unexpected server error: " + e.getMessage(), null));
        }
    }

    // ---------- Operations ----------

    private void viewAllCourses(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, JsonUtil.response(true, manager.getCourseCount() + " course(s) recorded.", listFields()));
    }

    private void addCourse(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        Course course = manager.addCourse(
                form.getOrDefault("code", ""),
                form.getOrDefault("title", ""),
                form.getOrDefault("units", ""));
        String message = "Course " + course.getCode() + " - " + course.getTitle()
                + " (" + course.getUnits() + " units) added successfully.";
        sendJson(exchange, 201, JsonUtil.response(true, message, listFields()));
    }

    private void editCourse(HttpExchange exchange, String code) throws IOException {
        Map<String, String> form = readForm(exchange);
        Course updated = manager.updateCourse(
                code,
                form.getOrDefault("code", ""),
                form.getOrDefault("title", ""),
                form.getOrDefault("units", ""));
        String message = "Course " + updated.getCode() + " - " + updated.getTitle()
                + " (" + updated.getUnits() + " units) updated successfully.";
        sendJson(exchange, 200, JsonUtil.response(true, message, listFields()));
    }

    private void deleteCourse(HttpExchange exchange, String code) throws IOException {
        Course removed = manager.deleteCourse(code);
        String message = "Course " + removed.getCode() + " - " + removed.getTitle() + " deleted.";
        sendJson(exchange, 200, JsonUtil.response(true, message, listFields()));
    }

    private void searchCourse(HttpExchange exchange) throws IOException {
        String code = parseForm(exchange.getRequestURI().getRawQuery()).getOrDefault("code", "").trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Please enter a course code to search for.");
        }
        Course course = manager.searchCourse(code);          // recursive search
        if (course == null) {
            sendJson(exchange, 404, JsonUtil.response(false,
                    "No course with code '" + code.toUpperCase() + "' was found.", null));
        } else {
            sendJson(exchange, 200, JsonUtil.response(true, "Course found.",
                    "\"course\":" + JsonUtil.courseToJson(course)));
        }
    }

    private void computeTotal(HttpExchange exchange) throws IOException {
        int total = manager.totalUnits();                    // recursive total
        sendJson(exchange, 200, JsonUtil.response(true,
                "Total credit units: " + total,
                "\"count\":" + manager.getCourseCount() + ",\"totalUnits\":" + total));
    }

    private void saveCourses(HttpExchange exchange) throws IOException {
        if (manager.getCourseCount() == 0) {
            throw new IllegalArgumentException("Nothing to save - add some courses first.");
        }
        String filename = cleanFilename(readForm(exchange).get("filename"));
        int count = manager.saveToFile(filename);
        sendJson(exchange, 200, JsonUtil.response(true, count + " course(s) saved to '" + filename + "'.", null));
    }

    private void loadCourses(HttpExchange exchange) throws IOException {
        String filename = cleanFilename(readForm(exchange).get("filename"));
        List<String> skipped = manager.loadFromFile(filename);
        String message = manager.getCourseCount() + " course(s) loaded from '" + filename + "'.";
        if (!skipped.isEmpty()) {
            message += " " + skipped.size() + " bad line(s) skipped.";
        }
        sendJson(exchange, 200, JsonUtil.response(true, message,
                listFields() + ",\"skipped\":" + JsonUtil.stringListToJson(skipped)));
    }

    private void exitProgram(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, JsonUtil.response(true, "Server stopped. You can close this tab.", null));
        // Stop the server on a separate thread, after the reply has been sent
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            server.stop(0);
            System.out.println("Server stopped. Thank you for using the Course Management System. Goodbye!");
        }).start();
    }

    // ---------- Helpers ----------

    /** "courses":[...],"count":n,"totalUnits":t */
    private String listFields() {
        return "\"courses\":" + JsonUtil.courseListToJson(manager.getCourses())
             + ",\"count\":" + manager.getCourseCount()
             + ",\"totalUnits\":" + manager.totalUnits();
    }

    /** Only simple names like courses.txt are allowed, so a request cannot
     *  read or overwrite files elsewhere on the computer. */
    private String cleanFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return DEFAULT_FILE;
        }
        filename = filename.trim();
        if (!filename.toLowerCase().endsWith(".json")) {
            filename += ".json";
        }
        if (!filename.matches("[A-Za-z0-9_-]{1,50}\\.json")) {
            throw new IllegalArgumentException(
                "File name may only use letters, numbers, - and _ (e.g. courses.json).");
        }
        return filename;
    }

    /** Reads a form-encoded request body, e.g. code=COS201&title=Programming+I&units=3 */
    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] body = in.readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                throw new IllegalArgumentException("Request is too large.");
            }
            return parseForm(new String(body, StandardCharsets.UTF_8));
        }
    }

    /** Splits "a=1&b=2" into a map, decoding characters such as + and %20. */
    private Map<String, String> parseForm(String text) {
        Map<String, String> values = new HashMap<>();
        if (text == null || text.isEmpty()) {
            return values;
        }
        for (String pair : text.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, JsonUtil.response(false, "That action is not allowed here.", null));
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
