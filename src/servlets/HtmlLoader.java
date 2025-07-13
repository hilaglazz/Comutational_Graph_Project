package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.logging.Level;

import server.RequestParser.RequestInfo;

public class HtmlLoader implements Servlet {
    private static final Logger LOGGER = Logger.getLogger(HtmlLoader.class.getName());
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit for HTML files
    private static final String[] ALLOWED_EXTENSIONS = {".html", ".htm", ".css", ".js", ".txt"};
    private static final String DEFAULT_FILE = "index.html";
    private static final String APP_PREFIX = "/app/";
    
    private final String baseDir;
    
    public HtmlLoader(String baseDir) {
        if (baseDir == null || baseDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Base directory cannot be null or empty");
        }
        this.baseDir = baseDir.trim();
        LOGGER.info("HtmlLoader initialized with base directory: " + this.baseDir);
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null) {
            LOGGER.severe("RequestInfo is null");
            sendErrorResponse(toClient, 400, "Bad Request", "Invalid request");
            return;
        }
        
        if (toClient == null) {
            LOGGER.severe("OutputStream is null");
            throw new IllegalArgumentException("OutputStream cannot be null");
        }
        
        LOGGER.fine("HtmlLoader handling request: " + ri.getHttpCommand() + " " + ri.getUri());
        
        try {
            String uri = ri.getUri();
            if (uri == null) {
                LOGGER.warning("URI is null");
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid URI");
                return;
            }
            
            // Extract file path from URI
            String filePath = extractFilePath(uri);
            if (filePath == null) {
                LOGGER.warning("Could not extract file path from URI: " + uri);
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid file path");
                return;
            }
            
            // Default to index.html if empty
            if (filePath.isEmpty() || filePath.equals("/")) {
                filePath = DEFAULT_FILE;
                LOGGER.fine("Using default file: " + DEFAULT_FILE);
            }
            
            // Validate file extension
            if (!isValidFileExtension(filePath)) {
                LOGGER.warning("Invalid file extension: " + filePath);
                sendErrorResponse(toClient, 403, "Forbidden", "File type not allowed");
                return;
            }
            
            // Resolve and validate path
            Path path = resolveAndValidatePath(filePath);
            if (path == null) {
                LOGGER.warning("Path validation failed for: " + filePath);
                sendErrorResponse(toClient, 403, "Forbidden", "Access denied");
                return;
            }
            
            // Serve file or return 404
            serveFile(path, toClient);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in HtmlLoader", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Unexpected server error");
        }
    }
    
    private String extractFilePath(String uri) {
        if (uri == null || !uri.startsWith(APP_PREFIX)) {
            return null;
        }
        
        try {
            return uri.substring(APP_PREFIX.length());
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warning("Error extracting file path from URI: " + uri);
            return null;
        }
    }
    
    private boolean isValidFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        String lowerFilePath = filePath.toLowerCase();
        for (String extension : ALLOWED_EXTENSIONS) {
            if (lowerFilePath.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    private Path resolveAndValidatePath(String filePath) {
        try {
            // Resolve path
            Path path = Paths.get(baseDir, filePath).normalize();
            Path basePath = Paths.get(baseDir).normalize();
            
            // Prevent directory traversal
            if (!path.startsWith(basePath)) {
                LOGGER.warning("Directory traversal attempt detected: " + filePath);
                return null;
            }
            
            // Check for null bytes or other suspicious characters
            if (filePath.contains("\0") || filePath.contains("..")) {
                LOGGER.warning("Suspicious characters in file path: " + filePath);
                return null;
            }
            
            return path;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error resolving path: " + filePath, e);
            return null;
        }
    }
    
    private void serveFile(Path path, OutputStream toClient) throws IOException {
        try {
            // Check if file exists and is not a directory
            if (!Files.exists(path)) {
                LOGGER.warning("File not found: " + path);
                sendErrorResponse(toClient, 404, "Not Found", "File not found: " + escapeHtml(path.getFileName().toString()));
                return;
            }
            
            if (Files.isDirectory(path)) {
                LOGGER.warning("Path is a directory: " + path);
                sendErrorResponse(toClient, 403, "Forbidden", "Cannot serve directory");
                return;
            }
            
            // Check file size
            long fileSize = Files.size(path);
            if (fileSize > MAX_FILE_SIZE) {
                LOGGER.warning("File too large: " + path + " (" + fileSize + " bytes)");
                sendErrorResponse(toClient, 413, "Payload Too Large", "File too large (max " + (MAX_FILE_SIZE / 1024 / 1024) + "MB)");
                return;
            }
            
            // Read file content
            String content;
            try {
                content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to read file: " + path, e);
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to read file");
                return;
            }
            
            // Determine content type
            String contentType = determineContentType(path);
            
            // Send response
            String response = String.format(
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: %s\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "%s",
                contentType, content.getBytes(StandardCharsets.UTF_8).length, content
            );
            
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            toClient.flush();
            
            LOGGER.fine("File served successfully: " + path + " (" + fileSize + " bytes)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error serving file: " + path, e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to serve file");
        }
    }
    
    private String determineContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "text/plain";
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        
        // Limit length to prevent XSS attacks
        if (s.length() > 100) {
            s = s.substring(0, 100) + "...";
        }
        
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    private void sendErrorResponse(OutputStream toClient, int statusCode, String statusText, String message) throws IOException {
        try {
            String html = String.format(
                "<html><body><h1>%d %s</h1><p>%s</p></body></html>",
                statusCode, statusText, escapeHtml(message)
            );
            
            String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "%s",
                statusCode, statusText, html.getBytes(StandardCharsets.UTF_8).length, html
            );
            
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            toClient.flush();
            
            LOGGER.fine("Error response sent: " + statusCode + " " + statusText);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send error response", e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.fine("HtmlLoader servlet closed");
        // Nothing to close
    }
} 