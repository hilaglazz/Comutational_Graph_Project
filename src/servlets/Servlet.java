package servlets;

import java.io.IOException;
import java.io.OutputStream;

import server.RequestParser.RequestInfo;

/*
 * Servlet is an interface that defines the methods that a servlet must implement.
 * It is used to handle requests and responses from the client.
 * It is also used to close the servlet.
 */
public interface Servlet {
    void handle(RequestInfo ri, OutputStream toClient) throws IOException; // Handle the request
    void close() throws IOException; // Close the servlet
}
