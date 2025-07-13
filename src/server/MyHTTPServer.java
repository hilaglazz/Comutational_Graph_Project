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


public class MyHTTPServer extends Thread implements HTTPServer{
    private static final Logger LOGGER = Logger.getLogger(MyHTTPServer.class.getName());
    private static final int DEFAULT_TIMEOUT = 1000;
    private static final int MAX_PORT = 65535;
    private static final int MIN_PORT = 1;
    private static final int MAX_THREADS = 100;
    private static final int MIN_THREADS = 1;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    
    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService pool;
    private final String serverName;

    private final Map<String, Servlet> getMap    = new ConcurrentHashMap<>();
    private final Map<String, Servlet> postMap   = new ConcurrentHashMap<>();
    private final Map<String, Servlet> deleteMap = new ConcurrentHashMap<>();

    public MyHTTPServer(int port, int nThreads){
        // Validate port number
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be between " + MIN_PORT + " and " + MAX_PORT + ", got: " + port);
        }
        
        // Validate thread count
        if (nThreads < MIN_THREADS || nThreads > MAX_THREADS) {
            throw new IllegalArgumentException("Thread count must be between " + MIN_THREADS + " and " + MAX_THREADS + ", got: " + nThreads);
        }
        
        this.port = port;
        this.pool = Executors.newFixedThreadPool(nThreads);
        this.serverName = "MyHTTPServer-" + port;
        
        LOGGER.info("HTTP Server initialized on port " + port + " with " + nThreads + " threads");
    }

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
        
        try {
            getServletMap(httpCommand).put(uri, s);
            LOGGER.info("Servlet added: " + httpCommand + " " + uri);
            System.out.println("Servlet added: " + httpCommand + " " + uri);
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Failed to add servlet: " + e.getMessage());
            throw e;
        }
    }

    public void removeServlet(String httpCommand, String uri){
        // Validate parameters
        if (httpCommand == null || httpCommand.trim().isEmpty()) {
            LOGGER.warning("Cannot remove servlet: HTTP command is null or empty");
            return;
        }
        if (uri == null || uri.trim().isEmpty()) {
            LOGGER.warning("Cannot remove servlet: URI is null or empty");
            return;
        }
        
        try {
            Servlet removed = getServletMap(httpCommand).remove(uri);
            if (removed != null) {
                LOGGER.info("Servlet removed: " + httpCommand + " " + uri);
            } else {
                LOGGER.warning("Servlet not found for removal: " + httpCommand + " " + uri);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Failed to remove servlet: " + e.getMessage());
        }
    }

    private Map<String, Servlet> getServletMap(String httpCommand) {
        if (httpCommand == null) {
            throw new IllegalArgumentException("HTTP command cannot be null");
        }
        
        switch (httpCommand.toUpperCase()) {
            case "GET":
                return getMap;
            case "POST":
                return postMap;
            case "DELETE":
                return deleteMap;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + httpCommand);
        }
    }

    private Servlet findServlet(String httpCommand, String uri) {
        // Validate parameters
        if (httpCommand == null || uri == null) {
            LOGGER.warning("Cannot find servlet: HTTP command or URI is null");
            return null;
        }
        
        try {
            Map<String, Servlet> map = getServletMap(httpCommand);
            if (map == null || map.isEmpty()) {
                return null;
            }
            
            // Find the servlet with the longest matching URI prefix
            Servlet bestMatch = null;
            String bestMatchUri = "";
            
            for (Map.Entry<String, Servlet> entry : map.entrySet()) {
                String registeredUri = entry.getKey();
                Servlet servlet = entry.getValue();
                
                if (registeredUri == null || servlet == null) {
                    LOGGER.warning("Found null URI or servlet in map, skipping");
                    continue;
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
            
            LOGGER.fine("Best match URI: " + bestMatchUri);
            System.out.println("bestMatchUri: " + bestMatchUri);
            return bestMatch;
            
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Error finding servlet: " + e.getMessage());
            return null;
        }
    }

    public void run(){
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            // Set timeout for accepting connections
            ss.setSoTimeout(DEFAULT_TIMEOUT);
            running = true;
            LOGGER.info("Server started and listening on port " + port);
            
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // Accept new connections
                    Socket client = ss.accept();
                    if (client != null) {
                        // Handle the connection in a new thread
                        pool.execute(() -> handleClient(client));
                    }
                } catch (SocketTimeoutException ignored) {
                    // This is expected behavior, continue polling
                } catch (IOException e) {
                    if (running) {
                        LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.SEVERE, "Failed to create server socket on port " + port, e);
                e.printStackTrace();
            }
        } finally {
            close();
        }
    }

    private void handleClient(Socket client) {
        if (client == null) {
            LOGGER.warning("Cannot handle null client socket");
            return;
        }
        
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {
            
            // Parse the request
            RequestInfo ri = RequestParser.parseRequest(reader);
            if (ri == null) {
                LOGGER.warning("Failed to parse request from client");
                sendErrorResponse(out, 400, "Bad Request", "Failed to parse request");
                return;
            }
            
            LOGGER.fine("Handling request: " + ri.getHttpCommand() + " " + ri.getUri());
            System.out.println("in MyHTTPServer handleClient");
            
            Servlet servlet = findServlet(ri.getHttpCommand(), ri.getUri());
            if (servlet != null) {
                // Handle the request
                LOGGER.fine("Found servlet: " + servlet.getClass().getSimpleName());
                System.out.println("servlet: " + servlet);
                try {
                    servlet.handle(ri, out);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in servlet handling", e);
                    sendErrorResponse(out, 500, "Internal Server Error", "Servlet processing error");
                }
            } else {
                LOGGER.warning("No servlet found for: " + ri.getHttpCommand() + " " + ri.getUri());
                sendErrorResponse(out, 404, "Not Found", "No handler found for this request");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling client connection", e);
            // Try to send error response if possible
            try (OutputStream out = client.getOutputStream()) {
                sendErrorResponse(out, 500, "Internal Server Error", "Connection handling error");
            } catch (IOException ignored) {
                // Cannot send error response, client connection is broken
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error handling client", e);
            e.printStackTrace();
        }
    }
    
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

    private void closeServlets(){
        Set<Servlet> allServlets = new HashSet<>();
        allServlets.addAll(getMap.values());
        allServlets.addAll(postMap.values());
        allServlets.addAll(deleteMap.values());
        
        for (Servlet s : allServlets) {
            if (s != null) {
                try {
                    s.close();
                    LOGGER.fine("Closed servlet: " + s.getClass().getSimpleName());
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing servlet: " + s.getClass().getSimpleName(), e);
                }
            }
        }
    }

    public void close(){
        LOGGER.info("Shutting down server...");
        
        // Stop accepting new connections
        running = false;
        
        // Close the server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                LOGGER.info("Server socket closed");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing server socket", e);
        }
        
        // Shutdown the thread pool gracefully
        try {
            pool.shutdown();
            LOGGER.info("Thread pool shutdown initiated");
            
            // Wait for existing tasks to complete (with timeout)
            if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warning("Thread pool did not terminate within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds, forcing shutdown");
                // Force shutdown if tasks don't complete in time
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
        
        LOGGER.info("Server shutdown complete");
    }

    public void start(){
        if (running) {
            LOGGER.warning("Server is already running");
            return;
        }
        
        try {
            new Thread(this, serverName).start();
            LOGGER.info("Server thread started");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start server thread", e);
            throw new RuntimeException("Failed to start server", e);
        }
    }
    
    // Getters for monitoring
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getActiveThreadCount() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getActiveCount();
    }
    
    public int getQueueSize() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getQueue().size();
    }
}
