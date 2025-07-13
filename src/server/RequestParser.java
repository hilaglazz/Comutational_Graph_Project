package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RequestParser {
    private static final Logger LOGGER = Logger.getLogger(RequestParser.class.getName());
    private static final int MAX_HEADER_LENGTH = 8192; // 8KB limit for headers
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024; // 10MB limit for content
    private static final int MAX_URI_LENGTH = 2048; // 2KB limit for URI
    private static final int MAX_PARAMETERS = 100; // Limit number of parameters

    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {  
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        
        LOGGER.fine("Starting request parsing");
        System.out.println("in parseRequest");
        
        // Read and validate the first line (HTTP command)
        String httpCommand = reader.readLine();
        System.out.println("httpCommand: " + httpCommand);
        
        if (httpCommand == null) {
            LOGGER.warning("End of stream reached while reading HTTP command");
            return null; // End of stream
        }
        
        if (httpCommand.isEmpty()) {
            LOGGER.warning("Empty HTTP command line received");
            return null;
        }
        
        if (httpCommand.length() > MAX_HEADER_LENGTH) {
            throw new IOException("HTTP command line too long: " + httpCommand.length() + " characters");
        }

        // Parse HTTP command line
        String[] commandParts = httpCommand.split(" ");
        if (commandParts.length < 2) {
            throw new IOException("Invalid HTTP request line (insufficient parts): " + httpCommand);
        }
        
        String method = commandParts[0];
        if (method == null || method.trim().isEmpty()) {
            throw new IOException("Invalid HTTP method: " + method);
        }
        
        // Validate HTTP method
        String upperMethod = method.toUpperCase();
        if (!upperMethod.equals("GET") && !upperMethod.equals("POST") && 
            !upperMethod.equals("DELETE") && !upperMethod.equals("PUT") && 
            !upperMethod.equals("HEAD") && !upperMethod.equals("OPTIONS")) {
            LOGGER.warning("Unsupported HTTP method: " + method);
        }
        
        // Extract and validate URI
        String uri = commandParts[1];
        System.out.println("uri: " + uri);
        
        if (uri == null || uri.trim().isEmpty()) {
            throw new IOException("Invalid URI: " + uri);
        }
        
        if (uri.length() > MAX_URI_LENGTH) {
            throw new IOException("URI too long: " + uri.length() + " characters");
        }
        
        // Parse URI segments and parameters
        String[] uriSegments;
        Map<String, String> parameters = new HashMap<>();
        
        try {
            if (uri.contains("?")) {
                String[] splitUri = uri.split("\\?", 2); // Split only on first ?
                if (splitUri.length != 2) {
                    throw new IOException("Invalid URI format with query string: " + uri);
                }
                
                String pathPart = splitUri[0];
                String queryPart = splitUri[1];
                
                // Parse path segments
                uriSegments = pathPart.split("/");
                uriSegments = Arrays.copyOfRange(uriSegments, 1, uriSegments.length);
                
                // Parse query parameters
                parseQueryParameters(queryPart, parameters);
            } else {
                uriSegments = uri.split("/");
                uriSegments = Arrays.copyOfRange(uriSegments, 1, uriSegments.length);
            }
        } catch (Exception e) {
            throw new IOException("Error parsing URI: " + uri, e);
        }

        // Parse headers
        Map<String, String> headers = new HashMap<>();
        String line;
        int headerCount = 0;
        
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            headerCount++;
            if (headerCount > 100) { // Limit number of headers
                throw new IOException("Too many headers in request");
            }
            
            if (line.length() > MAX_HEADER_LENGTH) {
                throw new IOException("Header line too long: " + line.length() + " characters");
            }
            
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                String headerName = headerParts[0].trim();
                String headerValue = headerParts[1].trim();
                
                if (headerName.isEmpty()) {
                    LOGGER.warning("Empty header name found, skipping");
                    continue;
                }
                
                headers.put(headerName, headerValue);
                
                // Parse additional parameters from headers (for multipart)
                if (headerValue.contains(";")) {
                    parseHeaderParameters(headerValue, parameters);
                }
            } else {
                LOGGER.warning("Invalid header format: " + line);
            }
        }

        // Parse content
        byte[] content = null;
        String filename = null;
        String contentType = headers.get("Content-Type");
        
        if (headers.containsKey("Content-Length")) {
            String contentLengthStr = headers.get("Content-Length");
            int contentLength;
            
            try {
                contentLength = Integer.parseInt(contentLengthStr);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Content-Length header: " + contentLengthStr);
            }
            
            if (contentLength < 0) {
                throw new IOException("Negative Content-Length: " + contentLength);
            }
            
            if (contentLength > MAX_CONTENT_LENGTH) {
                throw new IOException("Content too large: " + contentLength + " bytes (max: " + MAX_CONTENT_LENGTH + ")");
            }
            
            if (contentLength > 0) {
                try {
                    char[] contentBuffer = new char[contentLength];
                    int bytesRead = reader.read(contentBuffer, 0, contentLength);
                    
                    if (bytesRead == -1) {
                        throw new IOException("Unexpected end of stream while reading content");
                    }
                    
                    if (bytesRead != contentLength) {
                        LOGGER.warning("Incomplete content read: expected " + contentLength + ", got " + bytesRead);
                    }
                    
                    content = new String(contentBuffer, 0, bytesRead).getBytes(StandardCharsets.UTF_8);
                    String rawContent = new String(contentBuffer, 0, bytesRead);

                    // Enhanced multipart handling
                    if (contentType != null && contentType.startsWith("multipart/form-data")) {
                        try {
                            String[] multipartResult = parseMultipartContent(contentType, rawContent);
                            filename = multipartResult[0];
                            if (multipartResult[1] != null) {
                                content = multipartResult[1].getBytes(StandardCharsets.UTF_8);
                            }
                            if (filename != null) {
                                parameters.put("filename", filename);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error parsing multipart content", e);
                            // Continue with original content
                        }
                    }
                    
                    LOGGER.fine("Content length: " + content.length);
                    System.out.println("content: " + new String(content));
                    
                } catch (IOException e) {
                    throw new IOException("Failed to read content: " + e.getMessage(), e);
                }
            }
        }
        
        LOGGER.fine("Request parsing completed successfully");
        System.out.println("exit parseRequest");
        
        // Create and return the RequestInfo object
        return new RequestInfo(method, uri, uriSegments, parameters, content);
    }
    
    private static void parseQueryParameters(String queryString, Map<String, String> parameters) throws IOException {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }
        
        if (parameters.size() >= MAX_PARAMETERS) {
            throw new IOException("Too many parameters in request");
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                    parameters.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.warning("Failed to decode parameter: " + pair);
                    parameters.put(keyValue[0], keyValue[1]);
                }
            } else if (keyValue.length == 1) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                    parameters.put(key, "");
                } catch (UnsupportedEncodingException e) {
                    parameters.put(keyValue[0], "");
                }
            }
        }
    }
    
    private static void parseHeaderParameters(String headerValue, Map<String, String> parameters) {
        if (headerValue == null || headerValue.isEmpty()) {
            return;
        }
        
        String[] paramParts = headerValue.split("; ");
        for (String paramPart : paramParts) {
            if (paramPart.isEmpty()) {
                continue;
            }
            
            String[] keyValue = paramPart.split("=", 2);
            if (keyValue.length == 2) {
                // Remove quotes if present
                String value = keyValue[1];
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                parameters.put(keyValue[0], value);
            } else if (keyValue.length == 1) {
                parameters.put(paramPart, paramPart);
            }
        }
    }
    
    private static String[] parseMultipartContent(String contentType, String rawContent) {
        String[] result = new String[2]; // [filename, content]
        
        try {
            String boundary = "--" + contentType.split("boundary=")[1];
            String[] parts = rawContent.split(boundary);
            
            for (String part : parts) {
                if (part.contains("filename=")) {
                    // Extract filename
                    String[] filenameParts = part.split("filename=");
                    if (filenameParts.length > 1) {
                        String filenamePart = filenameParts[1];
                        String[] quoteParts = filenamePart.split("\"");
                        if (quoteParts.length > 1) {
                            result[0] = quoteParts[1]; // filename
                        }
                    }
                    
                    // Extract content
                    String[] contentParts = part.split("\r\n\r\n");
                    if (contentParts.length > 1) {
                        result[1] = contentParts[1].trim(); // content
                    }
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing multipart content", e);
        }
        
        return result;
    }
	
	// RequestInfo given internal class
    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        public RequestInfo(String httpCommand, String uri, String[] uriSegments, Map<String, String> parameters, byte[] content) {
            // Validate constructor parameters
            if (httpCommand == null) {
                throw new IllegalArgumentException("HTTP command cannot be null");
            }
            if (uri == null) {
                throw new IllegalArgumentException("URI cannot be null");
            }
            if (uriSegments == null) {
                throw new IllegalArgumentException("URI segments cannot be null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }
            
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments.clone(); // Defensive copy
            this.parameters = new HashMap<>(parameters); // Defensive copy
            this.content = content != null ? content.clone() : null; // Defensive copy
        }

        public String getHttpCommand() {
            return httpCommand;
        }

        public String getUri() {
            return uri;
        }

        public String[] getUriSegments() {
            return uriSegments.clone(); // Return defensive copy
        }

        public Map<String, String> getParameters() {
            return new HashMap<>(parameters); // Return defensive copy
        }

        public byte[] getContent() {
            return content != null ? content.clone() : null; // Return defensive copy
        }
        
        @Override
        public String toString() {
            return "RequestInfo{" +
                   "httpCommand='" + httpCommand + '\'' +
                   ", uri='" + uri + '\'' +
                   ", parameters=" + parameters +
                   ", contentLength=" + (content != null ? content.length : 0) +
                   '}';
        }
    }
}
