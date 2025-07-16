package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import server.RequestParser.RequestInfo;
import servlets.Servlet;
import servlets.TopicDisplayer;

/**
 * Represents a HTTP server that can handle HTTP requests.
 */
public class MyHTTPServer extends Thread implements HTTPServer{
    private static final Logger LOGGER = Logger.getLogger(MyHTTPServer.class.getName()); // Logger for the server
    private static final int DEFAULT_TIMEOUT = 1000; // Default timeout for the server
    private static final int MAX_PORT = 65535; // Maximum port number
    private static final int MIN_PORT = 1; // Minimum port number
    private static final int MAX_THREADS = 100; // Maximum number of threads
    private static final int MIN_THREADS = 1; // Minimum number of threads
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5; // Shutdown timeout in seconds
    
    private final int port; // Port number
    private volatile boolean running = false; // Whether the server is running
    private ServerSocket serverSocket; // Server socket
    private final ExecutorService pool; // Thread pool
    private final String serverName; // Server name

    private final Map<String, Servlet> getMap    = new ConcurrentHashMap<>(); // Map of GET requests
    private final Map<String, Servlet> postMap   = new ConcurrentHashMap<>(); // Map of POST requests
    private final Map<String, Servlet> deleteMap = new ConcurrentHashMap<>(); // Map of DELETE requests

    // Constructor
    public MyHTTPServer(int port, int nThreads){
        // Validate port number
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be between " + MIN_PORT + " and " + MAX_PORT + ", got: " + port);
        }
        
        // Validate thread count
        if (nThreads < MIN_THREADS || nThreads > MAX_THREADS) {
            throw new IllegalArgumentException("Thread count must be between " + MIN_THREADS + " and " + MAX_THREADS + ", got: " + nThreads);
        }
        
        this.port = port; // Set the port number
        this.pool = Executors.newFixedThreadPool(nThreads); // Create a new fixed thread pool
        this.serverName = "MyHTTPServer-" + port; // Set the server name
        
        addServlet("GET", "/topic-values", new TopicDisplayer()); // Add the topic displayer servlet
    }

    // Add a servlet to the server
    public void addServlet(String httpCommand, String uri, Servlet s){
        // Validate parameters
        if (httpCommand == null || httpCommand.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP command cannot be null or empty");
        }
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("URI cannot be null or empty");
        }
        if (s == null) {
            throw new IllegalArgumentException("Servlet cannot be null");
        }
        
        try { // Try to add the servlet to the map
            getServletMap(httpCommand).put(uri, s); // Add the servlet to the map
        } catch (IllegalArgumentException e) { // If there is an error adding the servlet
            throw e; // Throw the exception
        }
    }

    // Remove a servlet from the server
    public void removeServlet(String httpCommand, String uri){
        // Validate parameters
        if (httpCommand == null || httpCommand.trim().isEmpty()) {
            return; // Return if the HTTP command is null or empty
        }
        if (uri == null || uri.trim().isEmpty()) {
            return; // Return if the URI is null or empty
        }
        
        try { // Try to remove the servlet from the map
            getServletMap(httpCommand).remove(uri); // Remove the servlet from the map
        } catch (IllegalArgumentException e) { // If there is an error removing the servlet
            throw e; // Throw the exception
        }
    }

    // Get the servlet map for the given HTTP command
    private Map<String, Servlet> getServletMap(String httpCommand) {
        if (httpCommand == null) {
            throw new IllegalArgumentException("HTTP command cannot be null"); // Throw an exception if the HTTP command is null
        }
        
        switch (httpCommand.toUpperCase()) { // Switch on the HTTP command
            case "GET":
                return getMap; // Return the GET map
            case "POST":
                return postMap; // Return the POST map
            case "DELETE":
                return deleteMap; // Return the DELETE map
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + httpCommand); // Throw an exception if the HTTP command is not supported
        }
    }

    // Find the servlet for the given HTTP command and URI
    private Servlet findServlet(String httpCommand, String uri) {
        // Validate parameters
        if (httpCommand == null || uri == null) {
            return null; // Return null if the HTTP command or URI is null
        }
        
        try { // Try to find the servlet
            Map<String, Servlet> map = getServletMap(httpCommand); // Get the servlet map for the given HTTP command
            if (map == null || map.isEmpty()) { // If the servlet map is null or empty
                return null; // Return null
            }
            
            // Find the servlet with the longest matching URI prefix
            Servlet bestMatch = null;
            String bestMatchUri = "";
            
            for (Map.Entry<String, Servlet> entry : map.entrySet()) {
                String registeredUri = entry.getKey();
                Servlet servlet = entry.getValue();
                
                if (registeredUri == null || servlet == null) {
                    continue; // Skip if the URI or servlet is null
                }
                
                // Check if the request URI starts with the registered URI
                if (uri.startsWith(registeredUri)) {
                    // If this is a longer match than our current best, update it
                    if (registeredUri.length() > bestMatchUri.length()) {
                        bestMatch = servlet;
                        bestMatchUri = registeredUri;
                    }
                }
            }
            
            return bestMatch; // Return the best match
            
        } catch (IllegalArgumentException e) { // If there is an error finding the servlet
            return null; // Return null
        }
    }

    // Run the server
    public void run(){
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            // Set timeout for accepting connections
            ss.setSoTimeout(DEFAULT_TIMEOUT);
            running = true; // Set the running flag to true
            
            while (running && !Thread.currentThread().isInterrupted()) { // While the server is running and the thread is not interrupted
                try { // Try to accept a new connection
                    Socket client = ss.accept(); // Accept a new connection
                    if (client != null) { // If the client is not null
                        // Handle the connection in a new thread
                        pool.execute(() -> handleClient(client)); // Execute the client in a new thread
                    }
                } catch (SocketTimeoutException ignored) { // If the socket timeout exception is thrown
                    // This is expected behavior, continue polling
                } catch (IOException e) { // If the IO exception is thrown
                    if (running) { // If the server is running
                        LOGGER.log(Level.SEVERE, "Error accepting client connection", e); // Log the error
                    }
                }
            }
        } catch (IOException e) { // If the IO exception is thrown
            if (running) { // If the server is running
                LOGGER.log(Level.SEVERE, "Failed to create server socket on port " + port, e); // Log the error
                e.printStackTrace(); // Print the error
            }
        } finally { // Finally
            close(); // Close the server
        }
    }

    // Handle a client connection
    private void handleClient(Socket client) {
        if (client == null) { // If the client is null
            return; // Return
        }
        
        try (client; // Try to handle the client
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream())); // Create a new buffered reader for the client input stream
             OutputStream out = client.getOutputStream()) { // Create a new output stream for the client
            
            // Parse the request
            RequestInfo ri = RequestParser.parseRequest(reader);
            if (ri == null) { // If the request is null
                sendErrorResponse(out, 400, "Bad Request", "Failed to parse request"); // Send an error response
                return; // Return
            }
            
            Servlet servlet = findServlet(ri.getHttpCommand(), ri.getUri()); // Find the servlet for the given HTTP command and URI
            if (servlet != null) { // If the servlet is not null
                try { // Try to handle the request
                    servlet.handle(ri, out); // Handle the request
                } catch (Exception e) { // If there is an error handling the request
                    sendErrorResponse(out, 500, "Internal Server Error", "Servlet processing error"); // Send an error response
                }
            } else { // If the servlet is null
                sendErrorResponse(out, 404, "Not Found", "No handler found for this request"); // Send an error response
            }
        } catch (IOException e) { // If the IO exception is thrown
            try (OutputStream out = client.getOutputStream()) { // Try to send an error response
                sendErrorResponse(out, 500, "Internal Server Error", "Connection handling error"); // Send an error response
            } catch (IOException ignored) { // If the IO exception is thrown
                // Cannot send error response, client connection is broken
            }
        } catch (Exception e) { // If there is an error handling the client
            LOGGER.log(Level.SEVERE, "Unexpected error handling client", e); // Log the error
            e.printStackTrace(); // Print the error
        }
    }
    
    // Send an error response to the client
    private void sendErrorResponse(OutputStream out, int statusCode, String statusText, String message) {
        try {
            String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "<html><body><h1>%d %s</h1><p>%s</p></body></html>",
                statusCode, statusText, 
                message.length() + 20, // Approximate content length
                statusCode, statusText, message
            );
            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send error response", e);
        }
    }

    // Close all servlets
    private void closeServlets(){
        Set<Servlet> allServlets = new HashSet<>(); // Create a new set of servlets
        allServlets.addAll(getMap.values()); // Add all GET servlets to the set
        allServlets.addAll(postMap.values()); // Add all POST servlets to the set
        allServlets.addAll(deleteMap.values()); // Add all DELETE servlets to the set
        
        for (Servlet s : allServlets) { // For each servlet
            if (s != null) { // If the servlet is not null
                try { // Try to close the servlet
                    s.close(); // Close the servlet
                    LOGGER.fine("Closed servlet: " + s.getClass().getSimpleName()); // Log the servlet
                } catch (IOException e) { // If the IO exception is thrown
                    LOGGER.log(Level.WARNING, "Error closing servlet: " + s.getClass().getSimpleName(), e); // Log the error
                }
            }
        }
    }

    // Close the server
    public void close(){
        running = false; // Set the running flag to false
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) { // If the server socket is not null and not closed
                serverSocket.close(); // Close the server socket
            }
        } catch (IOException e) { // If the IO exception is thrown
            LOGGER.log(Level.SEVERE, "Error closing server socket", e); // Log the error
        }
        
        try {
            pool.shutdown(); // Shutdown the thread pool
            
            if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { // If the thread pool did not terminate within the timeout
                LOGGER.warning("Thread pool did not terminate within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds, forcing shutdown"); // Log the error
                pool.shutdownNow();
                
                // Wait a bit more for forced shutdown
                if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.severe("Thread pool did not terminate even after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warning("Thread pool shutdown interrupted");
            Thread.currentThread().interrupt();
            // Force shutdown
            pool.shutdownNow();
        }
        
        // Close all servlets
        closeServlets();
        
    }

    // Start the server
    public void start(){
        if (running) { // If the server is already running
            return; // Return
        }
        
        try { // Try to start the server
            new Thread(this, serverName).start(); // Start the server thread
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start server thread", e);
            throw new RuntimeException("Failed to start server", e); // Throw an exception
        }
    }
    
    // Getters for monitoring
    public boolean isRunning() {
        return running;
    }
    
    // Get the port number
    public int getPort() {
        return port; // Return the port number
    }
    
    // Get the active thread count
    public int getActiveThreadCount() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getActiveCount(); // Return the active thread count
    }
    
    // Get the queue size
    public int getQueueSize() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getQueue().size(); // Return the queue size
    }
}
