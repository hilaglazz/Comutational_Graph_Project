package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.RequestParser.RequestInfo;

/*
 * TopicDisplayer is a servlet that handles the display of topics.
 * It is used to display the topics in a table.
 * It is also used to publish messages to the topics.
 */
public class TopicDisplayer implements Servlet {
    private static final int MAX_TOPICS_DISPLAY = 100; // Maximum number of topics to display

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
        
        if ("/topic-values".equals(ri.getUri())) {
            TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Topic t : tm.getTopics()) {
                String value = t.getLatestMessage() != null ? t.getLatestMessage().asText : "0";
                if (!first) sb.append(",");
                sb.append("\"").append(t.name).append("\":");
                sb.append("\"").append(value).append("\"");
                first = false;
            }
            sb.append("}");
            String response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + sb.toString();
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            toClient.flush();
            return;
        }

        try {
            Map<String, String> params = ri.getParameters();
            if (params == null) {
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid request parameters");
                return;
            }
            
            String topicName = params.get("topic");
            String value = params.get("value");
            
            
            // Validate parameters for publish request
            if (topicName == null || value == null) {
                sendErrorResponse(toClient, 400, "Bad Request", "Missing topic or value parameter");
                return;
            }
            
            
            TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
            if (tm.getTopics().isEmpty()) {
                showTopicsTable(toClient, "Please load a configuration (graph) before publishing messages.");
                return;
            }
            
            if (!tm.hasTopic(topicName)) {
                showTopicsTable(toClient, "Topic does not exist: " + escapeHtml(topicName));
                return;
            }
            
            // Publish the value to the topic
            try {
                
                Topic topic = tm.getTopic(topicName);
                Message msg = new Message(value);
                topic.publish(msg);
                
                // Show the updated topics table
                showTopicsTable(toClient);
                
            } catch (Exception e) {
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to publish message");
            }
            
        } catch (Exception e) {
            sendErrorResponse(toClient, 500, "Internal Server Error", "Unexpected server error");
        }
    }
    
    // Show the topics table
    private void showTopicsTable(OutputStream toClient, String message) throws IOException {
        try {
            TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
            if (tm == null) {
                sendErrorResponse(toClient, 500, "Internal Server Error", "Topic manager not available");
                return;
            }
            
            // Show a table with all topics and their latest value
            StringBuilder tableRows = new StringBuilder();
            int topicCount = 0;
            
            for (Topic t : tm.getTopics()) {
                // Skip if the topic is null
                if (t == null) {
                    continue;
                }
                // Skip if the topic count is greater than the maximum number of topics to display
                if (topicCount >= MAX_TOPICS_DISPLAY) {
                    break;
                }
                
                String latestValue = "0"; // Default value
                String cssClass = "empty-value"; // Default CSS class
                Message latestMsg = t.getLatestMessage(); // Get the latest message
                
                // If the latest message is not null and the text is not null, set the latest value and CSS class
                if (latestMsg != null && latestMsg.asText != null) {
                    latestValue = escapeHtml(latestMsg.asText);
                    cssClass = "value-cell";
                }
                
                String topicName = t.name != null ? escapeHtml(t.name) : "Unknown"; // Get the topic name and escape it
                
                // Add the topic name and latest value to the table rows
                tableRows.append("<tr><td>")
                    .append(topicName)
                    .append("</td><td class='")
                    .append(cssClass)
                    .append("'>")
                    .append(latestValue)
                    .append("</td></tr>");
                
                topicCount++; // Increment the topic count
            }
            
            // Generate the HTML for the topics table
            String html = generateTopicsTableHtml(tableRows.toString()); // Generate the HTML for the topics table
            if (message != null && !message.isEmpty()) { // If there is a message, replace the default status bar with the error message
                // Replace the default status bar with the error message if there is a message
                html = html.replace(
                    "Real-time topic values updated successfully",
                    "<span style='color:red;'>" + escapeHtml(message) + "</span>"
                );
            }
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html; // Create the response   
            toClient.write(response.getBytes(StandardCharsets.UTF_8)); // Write the response to the client
            toClient.flush(); // Flush the client
            
        } catch (Exception e) {
            sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate topics table"); // Send an error response
        }
    }

    // Show the topics table without a message
    private void showTopicsTable(OutputStream toClient) throws IOException {
        showTopicsTable(toClient, null); // Show the topics table without a message
    }
    
    // Generate the topics table HTML
    private String generateTopicsTableHtml(String tableRows) {
        // Generate the HTML for the topics table with the given table rows
        return "<html><head><style>"
            + "html, body { height: 100%; }"
            + "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: none; height: 100%; min-height: 100%; }"
            + ".container { background: rgba(255,255,255,0.95); backdrop-filter: blur(10px); border-radius: 15px; padding: 10px 10px 0 10px; box-shadow: 0 8px 32px rgba(0,0,0,0.1); border: 1px solid rgba(255,255,255,0.2); height: 100%; }"
            + ".table-container { overflow-x: auto; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }"
            + "table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; }"
            + "th { background: linear-gradient(135deg, #3498db 0%, #2980b9 100%); color: white; padding: 15px; text-align: left; font-weight: 600; font-size: 14px; }"
            + "td { padding: 12px 15px; border-bottom: 1px solid #e9ecef; font-size: 14px; }"
            + "tr:nth-child(even) { background-color: #f8f9fa; }"
            + "tr:hover { background-color: #e8f4fd; transition: background-color 0.3s ease; }"
            + ".value-cell { font-weight: 600; color: #27ae60; }"
            + ".empty-value { color: #95a5a6; font-style: italic; }"
            + ".status-indicator { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #27ae60; margin-right: 8px; }"
            + ".status-bar { margin-top: 10px; padding: 8px; background: #e8f4fd; border-radius: 8px; border-left: 4px solid #3498db; font-size: 12px; color: #2c3e50; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='table-container'>"
            + "<table><tr><th>Topic Name</th><th>Current Value</th></tr>"
            + tableRows
            + "</table></div>"
            + "<div class='status-bar'>"
            + "<span class='status-indicator'></span>"
            + "Real-time topic values updated successfully"
            + "</div>"
            + "</div></body></html>";
    }

    // Escape the HTML
    private String escapeHtml(String s) {
        if (s == null) return "";
        
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
                "<html><body><h1>%d %s</h1><p>%s</p></body></html>",
                statusCode, statusText, escapeHtml(message)
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
            
        } catch (IOException e) {
            throw e;
        }
    }

    // Close the servlet
    @Override
    public void close() throws IOException {
        // Nothing to close
    }
} 