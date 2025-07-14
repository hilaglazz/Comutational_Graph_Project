package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import server.RequestParser.RequestInfo;

public class TopicDisplayer implements Servlet {
    private static final Logger LOGGER = Logger.getLogger(TopicDisplayer.class.getName());
    //private static final int MAX_TOPIC_NAME_LENGTH = 100;
    //private static final int MAX_VALUE_LENGTH = 1000;
    private static final int MAX_TOPICS_DISPLAY = 100;

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null) {
            LOGGER.severe("RequestInfo is null");
            sendErrorResponse(toClient, 400, "Bad Request", "Invalid request");
            return;
        }
        
        if (toClient == null) {
            LOGGER.severe("OutputStream is null");
            throw new IllegalArgumentException("OutputStream cannot be null");
        }
        
        LOGGER.fine("TopicDisplayer handling request: " + ri.getHttpCommand() + " " + ri.getUri());
        
        try {
            Map<String, String> params = ri.getParameters();
            if (params == null) {
                LOGGER.warning("Parameters map is null");
                sendErrorResponse(toClient, 400, "Bad Request", "Invalid request parameters");
                return;
            }
            
            String topicName = params.get("topic");
            String value = params.get("value");
            
            LOGGER.fine("Extracted parameters - topicName: " + topicName + ", value: " + value);
            
            // Handle refresh request
            //if ("refresh".equals(topicName)) {
            //    LOGGER.fine("Handling refresh request");
            //    showTopicsTable(toClient);
            //    return;
            //}
            
            // Validate parameters for publish request
            if (topicName == null || value == null) {
                LOGGER.warning("Missing topic or value parameter");
                sendErrorResponse(toClient, 400, "Bad Request", "Missing topic or value parameter");
                return;
            }
            
            // Validate parameter lengths
            //if (topicName.length() > MAX_TOPIC_NAME_LENGTH) {
            //    LOGGER.warning("Topic name too long: " + topicName.length());
            //    sendErrorResponse(toClient, 400, "Bad Request", "Topic name too long (max " + MAX_TOPIC_NAME_LENGTH + " characters)");
             //   return;
            //}
            
            //if (value.length() > MAX_VALUE_LENGTH) {
            //    LOGGER.warning("Value too long: " + value.length());
            //    sendErrorResponse(toClient, 400, "Bad Request", "Value too long (max " + MAX_VALUE_LENGTH + " characters)");
            //    return;
            //}
            
            // Validate topic name format
            //if (!isValidTopicName(topicName)) {
             //   LOGGER.warning("Invalid topic name format: " + topicName);
             //   sendErrorResponse(toClient, 400, "Bad Request", "Invalid topic name format");
             //   return;
            //}
            TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
            // Add this block:
            if (tm.getTopics().isEmpty()) {
                showTopicsTable(toClient, "Please load a configuration (graph) before publishing messages.");
                return;
            }
            
            if (!tm.hasTopic(topicName)) {
                LOGGER.warning("Topic does not exist: " + topicName);
                showTopicsTable(toClient, "Topic does not exist: " + escapeHtml(topicName));
                return;
            }
            
            // Publish the value to the topic
            try {
               // TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
               // if (tm == null) {
               //     LOGGER.severe("TopicManager is null");
                //    sendErrorResponse(toClient, 500, "Internal Server Error", "Topic manager not available");
               //     return;
                //}
                
                // Get or create the topic (handles topics that don't appear in the graph)
                //Topic topic = tm.getTopic(topicName);
                //if (topic == null) {
                //    LOGGER.warning("Failed to get or create topic: " + topicName);
                //    sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to get or create topic: " + escapeHtml(topicName));
                //    return;
                //}
                Topic topic = tm.getTopic(topicName);
                Message msg = new Message(value);
                topic.publish(msg);
                LOGGER.info("Message '" + value + "' published to topic '" + topicName + "'");
                
                // Show the updated topics table
                showTopicsTable(toClient);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error publishing message to topic: " + topicName, e);
                sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to publish message");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in TopicDisplayer", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Unexpected server error");
        }
    }
    
    //private boolean isValidTopicName(String topicName) {
    //    if (topicName == null || topicName.trim().isEmpty()) {
    //        return false;
    //    }
    //    
     //   // Check for valid characters (alphanumeric, underscore, hyphen)
    //    return topicName.matches("^[a-zA-Z0-9_-]+$");
    //}
    
    private void showTopicsTable(OutputStream toClient, String message) throws IOException {
        try {
            TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
            if (tm == null) {
                LOGGER.severe("TopicManager is null in showTopicsTable");
                sendErrorResponse(toClient, 500, "Internal Server Error", "Topic manager not available");
                return;
            }
            
            // Show a table with all topics and their latest value
            StringBuilder tableRows = new StringBuilder();
            int topicCount = 0;
            
            for (Topic t : tm.getTopics()) {
                if (t == null) {
                    LOGGER.warning("Found null topic, skipping");
                    continue;
                }
                
                if (topicCount >= MAX_TOPICS_DISPLAY) {
                    LOGGER.warning("Too many topics, limiting display to " + MAX_TOPICS_DISPLAY);
                    break;
                }
                
                String latestValue = "0";
                String cssClass = "empty-value";
                Message latestMsg = t.getLatestMessage();
                
                if (latestMsg != null && latestMsg.asText != null) {
                    latestValue = escapeHtml(latestMsg.asText);
                    cssClass = "value-cell";
                }
                
                String topicName = t.name != null ? escapeHtml(t.name) : "Unknown";
                
                tableRows.append("<tr><td>")
                    .append(topicName)
                    .append("</td><td class='")
                    .append(cssClass)
                    .append("'>")
                    .append(latestValue)
                    .append("</td></tr>");
                
                topicCount++;
            }
            
            String html = generateTopicsTableHtml(tableRows.toString());
            if (message != null && !message.isEmpty()) {
                // Replace the default status bar with the error message
                html = html.replace(
                    "Real-time topic values updated successfully",
                    "<span style='color:red;'>" + escapeHtml(message) + "</span>"
                );
            }
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html;
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            toClient.flush();
            
            LOGGER.fine("Topics table displayed with " + topicCount + " topics");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating topics table", e);
            sendErrorResponse(toClient, 500, "Internal Server Error", "Failed to generate topics table");
        }
    }

    private void showTopicsTable(OutputStream toClient) throws IOException {
        showTopicsTable(toClient, null);
    }
    
    private String generateTopicsTableHtml(String tableRows) {
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

    private String escapeHtml(String s) {
        if (s == null) return "";
        
        // Limit length to prevent XSS attacks
        //if (s.length() > MAX_VALUE_LENGTH) {
        //    s = s.substring(0, MAX_VALUE_LENGTH) + "...";
        //}
        
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
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
            
            LOGGER.fine("Error response sent: " + statusCode + " " + statusText);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send error response", e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.fine("TopicDisplayer servlet closed");
        // Nothing to close
    }
} 