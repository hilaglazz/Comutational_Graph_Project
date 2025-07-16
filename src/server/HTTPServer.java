package server;

import servlets.Servlet;

/**
 * Represents a HTTP server that can handle HTTP requests.
 */
public interface HTTPServer extends Runnable{
    // Add a servlet to the server
    public void addServlet(String httpCommanmd, String uri, Servlet s);
    // Remove a servlet from the server
    public void removeServlet(String httpCommanmd, String uri);
    // Start the server
    public void start();
    // Close the server
    public void close();
}
