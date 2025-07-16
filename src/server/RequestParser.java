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

/*
 * This class is used to parse the request from the client.
 * It is used to parse the request and return a RequestInfo object.
 * The RequestInfo object contains the HTTP command, URI, URI segments, parameters, and content.
 */
public class RequestParser {
    private static final Logger LOGGER = Logger.getLogger(RequestParser.class.getName());
    private static final int MAX_HEADER_LENGTH = 8192; // 8KB limit for headers
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024; // 10MB limit for content
    private static final int MAX_URI_LENGTH = 2048; // 2KB limit for URI
    private static final int MAX_PARAMETERS = 100; // Limit number of parameters

    // Parse the request from the client
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {  
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        
        // Read and validate the first line (HTTP command)
        String httpCommand = reader.readLine();
        
        if (httpCommand == null) {
            return null; // Return null if the HTTP command is null
        }
        
        if (httpCommand.isEmpty()) {
            return null; // Return null if the HTTP command is empty
        }
        
        if (httpCommand.length() > MAX_HEADER_LENGTH) { // If the HTTP command is too long
            throw new IOException("HTTP command line too long: " + httpCommand.length() + " characters");
        }

        // Parse HTTP command line
        String[] commandParts = httpCommand.split(" ");
        if (commandParts.length < 2) { // If the command parts are less than 2
            throw new IOException("Invalid HTTP request line (insufficient parts): " + httpCommand);
        }
        
        String method = commandParts[0]; // Get the method
        if (method == null || method.trim().isEmpty()) { // If the method is null or empty
            throw new IOException("Invalid HTTP method: " + method); // Throw an exception
        }
        
        // Validate HTTP method
        String upperMethod = method.toUpperCase();

        if (!upperMethod.equals("GET") && !upperMethod.equals("POST") && 
            !upperMethod.equals("DELETE") && !upperMethod.equals("PUT") && 
            !upperMethod.equals("HEAD") && !upperMethod.equals("OPTIONS")) {
            throw new IOException("Unsupported HTTP method: " + method);
        }
        
        // Extract and validate URI
        String uri = commandParts[1];
        
        if (uri == null || uri.trim().isEmpty()) {
            throw new IOException("Invalid URI: " + uri);
        }
        
        if (uri.length() > MAX_URI_LENGTH) {
            throw new IOException("URI too long: " + uri.length() + " characters");
        }
        
        // Parse URI segments and parameters
        String[] uriSegments;
        Map<String, String> parameters = new HashMap<>();
        
        try { // Try to parse the URI
            if (uri.contains("?")) { // If the URI contains a query string
                String[] splitUri = uri.split("\\?", 2); // Split only on first ?
                if (splitUri.length != 2) { // If the URI is not split into two parts
                    throw new IOException("Invalid URI format with query string: " + uri); // Throw an exception
                }
                
                String pathPart = splitUri[0]; // Get the path part
                String queryPart = splitUri[1]; // Get the query part
                
                // Parse path segments
                uriSegments = pathPart.split("/"); // Split the path part into segments
                uriSegments = Arrays.copyOfRange(uriSegments, 1, uriSegments.length); // Copy the path segments
                
                // Parse query parameters
                parseQueryParameters(queryPart, parameters); // Parse the query parameters
            } else {
                uriSegments = uri.split("/"); // Split the URI into segments
                uriSegments = Arrays.copyOfRange(uriSegments, 1, uriSegments.length); // Copy the URI segments
            }
        } catch (Exception e) { // If there is an error parsing the URI
            throw new IOException("Error parsing URI: " + uri, e); // Throw an exception
        }

        // Parse headers
        Map<String, String> headers = new HashMap<>(); // Create a new map for the headers
        String line; // Create a new string for the line
        int headerCount = 0; // Create a new integer for the header count
        
        // Parse the headers
        while ((line = reader.readLine()) != null && !line.isEmpty()) { // While the line is not null and not empty
            headerCount++; // Increment the header count
            if (headerCount > 100) { // Limit number of headers
                throw new IOException("Too many headers in request"); // Throw an exception
            }
            
            if (line.length() > MAX_HEADER_LENGTH) { // If the line is too long
                throw new IOException("Header line too long: " + line.length() + " characters"); // Throw an exception
            }
            
            String[] headerParts = line.split(": ", 2); // Split the line into two parts
            if (headerParts.length == 2) { // If the header parts are equal to 2
                String headerName = headerParts[0].trim(); // Get the header name
                String headerValue = headerParts[1].trim(); // Get the header value
                
                if (headerName.isEmpty()) { // If the header name is empty
                    continue; // Skip the header
                }
                
                headers.put(headerName, headerValue); // Put the header name and value into the map
                
                // Parse additional parameters from headers (for multipart)
                if (headerValue.contains(";")) { // If the header value contains a semicolon
                    parseHeaderParameters(headerValue, parameters); // Parse the header parameters
                }
            } else {
                throw new IOException("Invalid header format: " + line); // Throw an exception
            }
        }

        // Parse content
        byte[] content = null; // Create a new byte array for the content
        String filename = null; // Create a new string for the filename
        String contentType = headers.get("Content-Type"); // Get the content type
        
        if (headers.containsKey("Content-Length")) { // If the headers contain a content length
            String contentLengthStr = headers.get("Content-Length"); // Get the content length
            int contentLength; // Create a new integer for the content length
            
            try { // Try to parse the content length
                contentLength = Integer.parseInt(contentLengthStr);
            } catch (NumberFormatException e) { // If there is an error parsing the content length
                throw new IOException("Invalid Content-Length header: " + contentLengthStr); // Throw an exception
            }
            
            if (contentLength < 0) { // If the content length is less than 0
                throw new IOException("Negative Content-Length: " + contentLength); // Throw an exception
            }
            
            if (contentLength > MAX_CONTENT_LENGTH) { // If the content length is greater than the maximum content length
                throw new IOException("Content too large: " + contentLength + " bytes (max: " + MAX_CONTENT_LENGTH + ")"); // Throw an exception
            }
            
            if (contentLength > 0) { // If the content length is greater than 0
                try { // Try to read the content
                    char[] contentBuffer = new char[contentLength]; // Create a new character array for the content
                    int bytesRead = reader.read(contentBuffer, 0, contentLength); // Read the content
                    
                    if (bytesRead == -1) { // If the bytes read is -1
                        throw new IOException("Unexpected end of stream while reading content"); // Throw an exception
                    }
                    
                    if (bytesRead != contentLength) { // If the bytes read is not equal to the content length
                        throw new IOException("Incomplete content read: expected " + contentLength + ", got " + bytesRead); // Throw an exception
                    }
                    
                    content = new String(contentBuffer, 0, bytesRead).getBytes(StandardCharsets.UTF_8); // Convert the content to a byte array
                    String rawContent = new String(contentBuffer, 0, bytesRead);

                    // Enhanced multipart handling
                    if (contentType != null && contentType.startsWith("multipart/form-data")) { // If the content type starts with multipart/form-data
                        try { // Try to parse the multipart content
                            String[] multipartResult = parseMultipartContent(contentType, rawContent); // Parse the multipart content
                            filename = multipartResult[0]; // Get the filename
                            if (multipartResult[1] != null) { // If the multipart result is not null
                                content = multipartResult[1].getBytes(StandardCharsets.UTF_8); // Convert the content to a byte array
                            }
                            if (filename != null) { // If the filename is not null
                                parameters.put("filename", filename); // Put the filename into the parameters
                            }
                        } catch (Exception e) { // If there is an error parsing the multipart content
                            throw new IOException("Error parsing multipart content", e); // Throw an exception
                        }
                    }
                    
                } catch (IOException e) { // If there is an error reading the content
                    throw new IOException("Failed to read content: " + e.getMessage(), e); // Throw an exception
                }
            }
        }
        
        // Create and return the RequestInfo object
        return new RequestInfo(method, uri, uriSegments, parameters, content);
    }
    
   
    // Parse the query parameters
    private static void parseQueryParameters(String queryString, Map<String, String> parameters) throws IOException {
        if (queryString == null || queryString.isEmpty()) { // If the query string is null or empty
            return; // Return
        }
        
        if (parameters.size() >= MAX_PARAMETERS) { // If the parameters are greater than the maximum parameters
            throw new IOException("Too many parameters in request"); // Throw an exception
        }
        
        String[] pairs = queryString.split("&"); // Split the query string into pairs
        for (String pair : pairs) { // For each pair
            if (pair.isEmpty()) { // If the pair is empty
                continue; // Skip the pair
            }
            
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) { // If the key value is equal to 2
                try { // Try to decode the parameter
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()); // Decode the key
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()); // Decode the value
                    parameters.put(key, value); // Put the key and value into the parameters
                } catch (UnsupportedEncodingException e) { // If there is an error decoding the parameter
                    parameters.put(keyValue[0], keyValue[1]); // Put the key and value into the parameters
                }
            } else if (keyValue.length == 1) { // If the key value is equal to 1
                try { // Try to decode the parameter
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()); // Decode the key
                    parameters.put(key, ""); // Put the key and value into the parameters
                } catch (UnsupportedEncodingException e) {
                    parameters.put(keyValue[0], ""); // Put the key and value into the parameters
                }
            }
        }
    }
    
    // Parse the header parameters
    private static void parseHeaderParameters(String headerValue, Map<String, String> parameters) {
        if (headerValue == null || headerValue.isEmpty()) { // If the header value is null or empty
            return; // Return
        }
        
        String[] paramParts = headerValue.split("; "); // Split the header value into parts
        for (String paramPart : paramParts) { // For each parameter part
            if (paramPart.isEmpty()) { // If the parameter part is empty
                continue; // Skip the parameter part
            }
            
            String[] keyValue = paramPart.split("=", 2); // Split the parameter part into two parts
            if (keyValue.length == 2) { // If the key value is equal to 2
                // Remove quotes if present
                String value = keyValue[1]; // Get the value
                if (value.startsWith("\"") && value.endsWith("\"")) { // If the value starts with a quote and ends with a quote
                    value = value.substring(1, value.length() - 1); // Remove the quotes
                }
                parameters.put(keyValue[0], value); // Put the key and value into the parameters
            } else if (keyValue.length == 1) { // If the key value is equal to 1
                parameters.put(paramPart, paramPart); // Put the parameter part into the parameters
            }
        }
    }
    
    // Parse the multipart content
    private static String[] parseMultipartContent(String contentType, String rawContent) {
        String[] result = new String[2]; // [filename, content]
        
        try {
            String boundary = "--" + contentType.split("boundary=")[1]; // Get the boundary
            String[] parts = rawContent.split(boundary); // Split the raw content into parts
            
            for (String part : parts) { // For each part
                if (part.contains("filename=")) { // If the part contains a filename
                    // Extract filename
                    String[] filenameParts = part.split("filename="); // Split the part into filename parts
                    if (filenameParts.length > 1) { // If the filename parts are greater than 1
                        String filenamePart = filenameParts[1]; // Get the filename part
                        String[] quoteParts = filenamePart.split("\""); // Split the filename part into quote parts
                        if (quoteParts.length > 1) { // If the quote parts are greater than 1
                            result[0] = quoteParts[1]; // filename
                        }
                    }
                    
                    // Extract content
                    String[] contentParts = part.split("\r\n\r\n"); // Split the part into content parts
                    if (contentParts.length > 1) { // If the content parts are greater than 1
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
        private final String httpCommand; // HTTP command
        private final String uri; // URI
        private final String[] uriSegments; // URI segments
        private final Map<String, String> parameters; // Parameters
        private final byte[] content; // Content

        // Constructor
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

        // Getters:
        
        // Get the HTTP command
        public String getHttpCommand() {
            return httpCommand; // Return the HTTP command
        }

        // Get the URI
        public String getUri() {
            return uri; // Return the URI
        }

        // Get the URI segments
        public String[] getUriSegments() {
            return uriSegments.clone(); // Return defensive copy
        }

        // Get the parameters
        public Map<String, String> getParameters() {
            return new HashMap<>(parameters); // Return defensive copy
        }

        // Get the content
        public byte[] getContent() {
            return content != null ? content.clone() : null; // Return defensive copy
        }
        
        // Return the string representation of the request info
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
