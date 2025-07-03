package test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import test.RequestParser.RequestInfo;

public class HtmlLoader implements Servlet {
    private final String baseDir;
    
    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        String uri = ri.getUri();
        String filePath = uri.substring(uri.indexOf("/app/") + 5); // everything after /app/
        // Default to index.html if empty
        if (filePath.isEmpty() || filePath.equals("/")) {
            filePath = "index.html";
        }
        //if (filePath.isEmpty()) filePath = "index.html";
        // Resolve and validate path
        Path path = Paths.get(baseDir, filePath).normalize();
        Path basePath = Paths.get(baseDir).normalize();
        // Prevent directory traversal
        if (!path.startsWith(basePath)) {
            String response = "HTTP/1.1 403 Forbidden\r\nContent-Type: text/html\r\n\r\n" +
                "<html><body><h2>Access denied.</h2></body></html>";
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            return;
        }
        //Path path = Paths.get(baseDir, filePath);
        // Serve file or 404
        String response;
        if (Files.exists(path) && !Files.isDirectory(path)) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + content;
        } else {
            response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n" +
                "<html><body><h2>File not found: " + escapeHtml(filePath) + "</h2></body></html>";
        }
        toClient.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public void close() throws IOException {
        // Nothing to close
    }
} 