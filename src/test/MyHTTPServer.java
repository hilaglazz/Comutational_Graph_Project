package test;

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

import test.RequestParser.RequestInfo;


public class MyHTTPServer extends Thread implements HTTPServer{
    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService pool;

    private final Map<String, Servlet> getMap    = new ConcurrentHashMap<>();
    private final Map<String, Servlet> postMap   = new ConcurrentHashMap<>();
    private final Map<String, Servlet> deleteMap = new ConcurrentHashMap<>();

    public MyHTTPServer(int port,int nThreads){
        this.port = port;
        this.pool = Executors.newFixedThreadPool(nThreads);

    }

    public void addServlet(String httpCommanmd, String uri, Servlet s){
        getServletMap(httpCommanmd).put(uri, s);
    }

    public void removeServlet(String httpCommanmd, String uri){
        getServletMap(httpCommanmd).remove(uri);
    }

    private Map<String, Servlet> getServletMap(String httpCommanmd) {
        switch (httpCommanmd.toUpperCase()) {
            case "GET":
                return getMap;
            case "POST":
                return postMap;
            case "DELETE":
                return deleteMap;
            default:
                throw new IllegalArgumentException("Unknown httpcommand: " + httpCommanmd);
        }
    }

    private Servlet findServlet(String httpCommand, String uri) {
        Map<String, Servlet> map = getServletMap(httpCommand);
        if (map == null) {
            return null;
        }
        
        // Find the servlet with the longest matching URI prefix
        Servlet bestMatch = null;
        String bestMatchUri = "";
        
        for (Map.Entry<String, Servlet> entry : map.entrySet()) {
            String registeredUri = entry.getKey();
            Servlet servlet = entry.getValue();
            
            // Check if the request URI starts with the registered URI
            if (uri.startsWith(registeredUri)) {
                // If this is a longer match than our current best, update it
                if (registeredUri.length() > bestMatchUri.length()) {
                    bestMatch = servlet;
                    bestMatchUri = registeredUri;
                }
            }
        }
        
        return bestMatch;
    }

    public void run(){
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            // poll for new connections every 1 second
            ss.setSoTimeout(1000);
            running = true;
            while (running) {
                try {
                    // accept new connections
                    Socket client = ss.accept();
                    // handle the connection in a new thread
                    pool.execute(() -> handleClient(client));
                } catch (SocketTimeoutException ignored) {}
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        } finally {
            close();
        }
    }

    private void handleClient(Socket client) {
        try (client;
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                //read the request from the client
                OutputStream out = client.getOutputStream()) {
            //parse the request
            RequestInfo ri = RequestParser.parseRequest(reader);
            Servlet servlet = findServlet(ri.getHttpCommand(), ri.getUri());
            if (servlet != null) {
                //handle the request
                servlet.handle(ri, out);
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeSevlets(){
        Set<Servlet> allServlets = new HashSet<>();
        allServlets.addAll(getMap.values());
        allServlets.addAll(postMap.values());
        allServlets.addAll(deleteMap.values());
        for (Servlet s : allServlets) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }
    }

    public void close(){
        // Stop accepting new connections
        running = false;
        // Close the server socket
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Shutdown the thread pool gracefully
        try {
            pool.shutdown();
            // Wait for existing tasks to complete (with timeout)
            if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                // Force shutdown if tasks don't complete in time
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Force shutdown
            pool.shutdownNow();
        }
        // Close all servlets
        closeSevlets();
    }

    public void start(){
        new Thread(this).start();
    }

}
