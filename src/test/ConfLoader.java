package test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import test.RequestParser.RequestInfo;

public class ConfLoader implements Servlet {
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        // Assume the uploaded file is in the content as bytes, and the filename is in the parameters
        Map<String, String> params = ri.getParameters();
        String filename = params.get("filename");
        byte[] fileContent = ri.getContent();
        if (fileContent == null || fileContent.length == 0) {
            String response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n" +
                "<html><body><h2>No file uploaded.</h2></body></html>";
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            return;
        }
        // Save the file
        Files.write(Paths.get(filename), fileContent);
        // Load into GenericConfig
        GenericConfig config = new GenericConfig();
        config.setConfFile(filename);
        config.create();
        // Create the graph
        Graph graph = new Graph();
        graph.createFromTopics();
        // Generate a simple HTML representation of the graph
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h2>Graph Visualization</h2><table border='1'><tr><th>Node</th><th>Edges</th></tr>");
        for (Node node : graph) {
            html.append("<tr><td>").append(escapeHtml(node.getName())).append("</td><td>");
            for (Node edge : node.getEdges()) {
                html.append(escapeHtml(edge.getName())).append(" ");
            }
            html.append("</td></tr>");
        }
        html.append("</table></body></html>");
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html.toString();
        toClient.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public void close() throws IOException {
        // Nothing to close
    }
} 