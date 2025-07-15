package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.Level;

import configs.GenericConfig;
import configs.Graph;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

public class ConfLoader implements Servlet {
    private static final Logger LOGGER = Logger.getLogger(ConfLoader.class.getName());
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".conf", ".txt", ".cfg"};
    private static final String UPLOAD_DIR = "config_files";
    
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
        
        LOGGER.fine("ConfLoader handling request: " + ri.getHttpCommand() + " " + ri.getUri());
        
        // --- NEW: Serve GET /graph for live graph refresh ---
        if ("GET".equals(ri.getHttpCommand()) && "/graph".equals(ri.getUri())) {
            handleGraphRequest(toClient);
            return;
        }
        // --- END NEW ---
        
        try {
            // Validate request method
            if (!"POST".equals(ri.getHttpCommand())) {
                LOGGER.warning("Invalid HTTP method: " + ri.getHttpCommand());
                sendErrorResponse(toClient, 405, "Method Not Allowed", "Only POST method is allowed for file upload");
                return;
            }
            
            // Extract and validate parameters
            Map<String, String> params = ri.getParameters();
            if (params == null) {
                LOGGER.warning("Parameters map is null");
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid request parameters");
                return;
            }
            
            String filename = params.get("filename");
            if (filename == null || filename.trim().isEmpty()) {
                LOGGER.warning("Filename parameter is missing or empty");
                sendErrorResponse(toClient, 400, "Bad Request", "Filename parameter is required");
                return;
            }
            
            // Clean and validate filename
            filename = filename.replace("\"", "").trim();
            if (filename.isEmpty()) {
                LOGGER.warning("Filename is empty after cleaning");
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid filename");
                return;
            }
            
            if (filename.length() > MAX_FILENAME_LENGTH) {
                LOGGER.warning("Filename too long: " + filename.length());
                sendErrorResponse(toClient, 400, "Bad Request", "Filename too long (max " + MAX_FILENAME_LENGTH + " characters)");
                return;
            }
            
            // Validate file extension
            if (!isValidFileExtension(filename)) {
                LOGGER.warning("Invalid file extension: " + filename);
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
                return;
            }
            
            // Validate filename format (prevent path traversal)
            if (!isValidFilename(filename)) {
                LOGGER.warning("Invalid filename format: " + filename);
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid filename format");
                return;
            }
            
            // Validate content
            byte[] fileContent = ri.getContent();
            if (fileContent == null || fileContent.length == 0) {
                LOGGER.warning("No file content provided");
                sendErrorResponse(toClient, 400, "Bad Request", "No file uploaded");
                return;
            }
            
            if (fileContent.length > MAX_FILE_SIZE) {
                LOGGER.warning("File too large: " + fileContent.length + " bytes");
                sendErrorResponse(toClient, 413, "Payload Too Large", "File too large (max " + (MAX_FILE_SIZE / 1024 / 1024) + "MB)");
                return;
            }
            
            // Process the file upload
            processFileUpload(filename, fileContent, toClient);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in ConfLoader", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Unexpected server error");
        }
    }
    
    private void handleGraphRequest(OutputStream toClient) throws IOException {
        try {
            Graph graph = new Graph();
            graph.createFromTopics();
            String html = HtmlGraphWriter.getGraphHTML(graph);
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html;
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            toClient.flush();
            LOGGER.fine("Graph request handled successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling graph request", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate graph");
        }
    }
    
    private void processFileUpload(String filename, byte[] fileContent, OutputStream toClient) throws IOException {
        try {
            // Create upload directory
            Path uploadDir = Paths.get(UPLOAD_DIR);
            Path filePath = uploadDir.resolve(filename).normalize();
            
            LOGGER.fine("Processing file upload: " + filename + " (" + fileContent.length + " bytes)");
            
            // Prevent path traversal attacks
            if (!filePath.startsWith(uploadDir)) {
                LOGGER.warning("Path traversal attempt detected: " + filePath);
                sendErrorResponse(toClient, 403, "Forbidden", "Invalid file path");
                return;
            }
            
            // Create directory if it doesn't exist
            try {
                Files.createDirectories(uploadDir);
                LOGGER.fine("Upload directory created/verified: " + uploadDir);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create upload directory", e);
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to create upload directory");
                return;
            }
            
            // Save file
            try {
                Files.write(filePath, fileContent);
                LOGGER.info("File saved successfully: " + filePath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save file: " + filePath, e);
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to save file");
                return;
            }
            
            // Load configuration
            GenericConfig config = new GenericConfig();
            
            try {
                config.setConfFile(filePath.toString());
                config.create();
                LOGGER.info("Configuration loaded successfully from: " + filePath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load configuration from: " + filePath, e);
                // Return a user-friendly error message with a recognizable marker for the frontend
                sendErrorResponse(toClient, 400, "Invalid Configuration", "<div id='configError'>Configuration error: " + escapeHtml(e.getMessage()) + "</div>");
                return;
            }
            
            // Create and display graph
            try {
                Graph graph = new Graph();
                graph.createFromTopics();
                if (graph.getNodeCount() == 0) {
                    sendErrorResponse(toClient, 400, "Invalid Configuration", "<div id='configError'>Configuration error: No valid nodes found in the configuration. Please check your file for missing or invalid agent/topic definitions.</div>");
                    return;
                }
                String html = HtmlGraphWriter.getGraphHTML(graph);
                String successResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    html;
                toClient.write(successResponse.getBytes(StandardCharsets.UTF_8));
                toClient.flush();
                LOGGER.info("Graph visualization generated successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to generate graph visualization", e);
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate graph visualization");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing file upload", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Error processing file: " + escapeHtml(Objects.toString(e.getMessage(), "Unknown error")));
        }
    }
    
    private boolean isValidFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String lowerFilename = filename.toLowerCase();
        for (String extension : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        
        // Check for valid characters
        return filename.matches("^[a-zA-Z0-9._-]+$");
    }
    
    private String escapeHtml(String s) {
        if (s == null) return "";
        
        // Limit length to prevent XSS attacks
        if (s.length() > 1000) {
            s = s.substring(0, 1000) + "...";
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
                statusCode, statusText, message
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
        LOGGER.fine("ConfLoader servlet closed");
        // Nothing to close
    }
} 