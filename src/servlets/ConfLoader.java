package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import configs.GenericConfig;
import configs.Graph;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

/*
 * ConfLoader is a servlet that handles the loading of configuration files.
 * It is used to load the configuration file from the client and display the graph.
 * It is also used to save the configuration file to the server.
 * It is also used to display the graph.
 * It is also used to display the configuration file.
 */
public class ConfLoader implements Servlet {
    private static final int MAX_FILENAME_LENGTH = 255; // Maximum filename length
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".conf", ".txt", ".cfg"}; // Allowed file extensions
    private static final String UPLOAD_DIR = "config_files"; // Upload directory
    
    // Handle the request
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null) {
            sendErrorResponse(toClient, 400, "Bad Request", "Invalid request");
            return;
        }
        
        if (toClient == null) {
            throw new IllegalArgumentException("OutputStream cannot be null");
        }
        
        // Serve GET /graph for live graph refresh
        if ("GET".equals(ri.getHttpCommand()) && "/graph".equals(ri.getUri())) {
            handleGraphRequest(toClient);
            return;
        }
        
        try { // Try to handle the request
            // Validate request method
            if (!"POST".equals(ri.getHttpCommand())) {
                sendErrorResponse(toClient, 405, "Method Not Allowed", "Only POST method is allowed for file upload");
                return;
            }
            
            // Extract and validate parameters
            Map<String, String> params = ri.getParameters();
            if (params == null) {
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid request parameters");
                return;
            }
            
            String filename = params.get("filename");
            if (filename == null || filename.trim().isEmpty()) {
                sendErrorResponse(toClient, 400, "Bad Request", "Filename parameter is required");
                return;
            }
            
            // Clean and validate filename
            filename = filename.replace("\"", "").trim();
            if (filename.isEmpty()) {
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid filename");
                return;
            }
            
            if (filename.length() > MAX_FILENAME_LENGTH) {
                sendErrorResponse(toClient, 400, "Bad Request", "Filename too long (max " + MAX_FILENAME_LENGTH + " characters)");
                return;
            }
            
            // Validate file extension
            if (!isValidFileExtension(filename)) {
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
                return;
            }
            
            // Validate filename format (prevent path traversal)
            if (!isValidFilename(filename)) {
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid filename format");
                return;
            }
            
            // Validate content
            byte[] fileContent = ri.getContent();
            if (fileContent == null || fileContent.length == 0) {
                sendErrorResponse(toClient, 400, "Bad Request", "No file uploaded");
                return;
            }
            
            if (fileContent.length > MAX_FILE_SIZE) {
                sendErrorResponse(toClient, 413, "Payload Too Large", "File too large (max " + (MAX_FILE_SIZE / 1024 / 1024) + "MB)");
                return;
            }
            
            // Process the file upload
            processFileUpload(filename, fileContent, toClient);
            
        } catch (Exception e) {
            sendErrorResponse(toClient, 500, "Internal Server Error", "Unexpected server error");
        }
    }
    
    // Handle the graph request
    private void handleGraphRequest(OutputStream toClient) throws IOException {
        try { 
            Graph graph = new Graph(); // Create a new graph    
            graph.createFromTopics(); // Create the graph from the topics
            String html = HtmlGraphWriter.getGraphHTML(graph); // Get the graph HTML
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html; // Create the response
            toClient.write(response.getBytes(StandardCharsets.UTF_8)); // Write the response to the client
            toClient.flush(); // Flush the client
        } catch (Exception e) {
            sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate graph");
        }
    }
    
    // Process the file upload
    private void processFileUpload(String filename, byte[] fileContent, OutputStream toClient) throws IOException {
        try {
            // Create upload directory
            Path uploadDir = Paths.get(UPLOAD_DIR); // Create the upload directory
            Path filePath = uploadDir.resolve(filename).normalize(); // Create the file path
            
            // Prevent path traversal attacks
            if (!filePath.startsWith(uploadDir)) {
                sendErrorResponse(toClient, 403, "Forbidden", "Invalid file path");
                return;
            }
            
            // Create directory if it doesn't exist
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to create upload directory");
                return;
            }
            
            // Save file
            try {
                Files.write(filePath, fileContent);
            } catch (IOException e) {
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to save file");
                return;
            }
            
            // Load configuration
            GenericConfig config = new GenericConfig(); // Create a new generic config
            
            try { // Try to load the configuration
                config.setConfFile(filePath.toString()); // Set the configuration file
                config.create(); // Create the configuration
            } catch (Exception e) {
                // Do NOT escape HTML here, so <br> is rendered as line breaks
                sendErrorResponse(toClient, 400, "Invalid Configuration", "<div id='configError'>Configuration error: " + e.getMessage() + "</div>");
                return;
            }
            
            // Create and display graph
            try { // Try to create and display the graph
                Graph graph = new Graph(); // Create a new graph
                graph.createFromTopics(); // Create the graph from the topics
                if (graph.getNodeCount() == 0) { // If the graph has no nodes
                    sendErrorResponse(toClient, 400, "Invalid Configuration", "<div id='configError'>Configuration error: No valid nodes found in the configuration. Please check your file for missing or invalid agent/topic definitions.</div>");
                    return;
                }
                String html = HtmlGraphWriter.getGraphHTML(graph); // Get the graph HTML
                String successResponse = "HTTP/1.1 200 OK\r\n" + // Create the success response
                    "Content-Type: text/html\r\n\r\n" + // Set the content type
                    html; // Set the HTML
                toClient.write(successResponse.getBytes(StandardCharsets.UTF_8)); // Write the success response to the client
                toClient.flush(); // Flush the client
            } catch (Exception e) { // If there is an error generating the graph visualization
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate graph visualization");
            }
            
        } catch (Exception e) {
            sendErrorResponse(toClient, 500, "Internal Server Error", "Error processing file: " + escapeHtml(Objects.toString(e.getMessage(), "Unknown error")));
        }
    }
    
    // Check if the file extension is valid
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
    
    // Check if the filename is valid
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
    
    // Escape the HTML
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
    
    // Send the error response
    private void sendErrorResponse(OutputStream toClient, int statusCode, String statusText, String message) throws IOException {
        try {
            String html = String.format(
                "<html><body><div id='configError' style='max-width:600px;margin:40px auto;padding:24px;background:#ffeaea;border:1px solid #e57373;border-radius:10px;color:#b71c1c;font-family:sans-serif;box-shadow:0 2px 8px rgba(0,0,0,0.07);'>"
                + "<h2 style='margin-top:0;'>%d %s</h2>"
                + "<div>%s</div>"
                + "</div></body></html>",
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
        } catch (IOException e) { // If there is an error sending the error response
            throw e;
        }
    }

    // Close the servlet
    @Override
    public void close() throws IOException {
        // Nothing to close
    }
} 