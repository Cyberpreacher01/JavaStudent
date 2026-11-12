import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

/**
 * CourseServer.java
 * Entry point of the Student Course Management System (web version).
 * Starts a small web server using Java's built-in HttpServer:
 *   - /api/...  is handled by ApiHandler (the backend operations)
 *   - /         is handled by StaticFileHandler (sends the HTML, CSS and JS)
 *
 * Compile:  javac -d out src/*.java
 * Run:      java -cp out CourseServer
 * Then open http://localhost:8080 in a browser.
 */
public class CourseServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String WEB_FOLDER = "web";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Optional: a different port can be given, e.g.  java -cp out CourseServer 9090
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0] + "'. Using " + DEFAULT_PORT + " instead.");
            }
        }

        try {
            // Bind to 127.0.0.1 so only this computer can reach the server
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            CourseManager manager = new CourseManager();

            server.createContext("/api/", new ApiHandler(manager, server));
            server.createContext("/", new StaticFileHandler(WEB_FOLDER));
            server.setExecutor(null);   // one request at a time keeps the course list safe
            server.start();

            System.out.println("=============================================");
            System.out.println("  STUDENT COURSE MANAGEMENT SYSTEM");
            System.out.println("  Server running at http://localhost:" + port);
            System.out.println("  Open that address in your browser.");
            System.out.println("  Click 'Exit Program' on the page (or press Ctrl+C) to stop.");
            System.out.println("=============================================");
        } catch (BindException e) {
            System.out.println("Error: Port " + port + " is already in use.");
            System.out.println("Close the other program, or run:  java -cp out CourseServer 9090");
        } catch (IOException e) {
            System.out.println("Error: Could not start the server - " + e.getMessage());
        }
    }
}
