package test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import test.RequestParser.RequestInfo;

public class TopicDisplayer implements Servlet {

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        System.out.println("in TopicDisplayer handle");
        System.out.println("Debug: Handling new request."); // Debug
        Map<String, String> params = ri.getParameters();
        String topicName = params.get("topic");
        String value = params.get("value");
        System.out.println("Debug: Extracted parameters - topicName: " + topicName + ", value: " + value); // Debug
        
        // Handle refresh request
        if ("refresh".equals(topicName)) {
            // Just show the current values without publishing
            showTopicsTable(toClient);
            return;
        }
        
        if (topicName == null || value == null) {
            System.out.println("Debug: Missing topic or value parameter. Sending 400 Bad Request."); // Debug
            String response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n" +
                "<html><body><h2>Missing topic or value parameter.</h2></body></html>";
            toClient.write(response.getBytes(StandardCharsets.UTF_8));
            return;
        }
        // Decode in case of URL encoding
        //topicName = URLDecoder.decode(topicName, "UTF-8");
        //value = URLDecoder.decode(value, "UTF-8");
        // Publish the value to the topic
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        Topic topic = tm.getTopic(topicName);
        Message msg = new Message(value);
        topic.publish(msg);
        System.out.println("Debug: Message '" + value + "' published to topic '" + topicName + "'."); // Debug
        
        // Show the updated topics table
        showTopicsTable(toClient);
    }
    
    private void showTopicsTable(OutputStream toClient) throws IOException {
        TopicManagerSingleton.TopicManager tm = TopicManagerSingleton.get();
        // Show a table with all topics and their latest value
        StringBuilder tableRows = new StringBuilder();
        for (Topic t : tm.getTopics()) {
            String latestValue = "0";
            String cssClass = "empty-value";
            Message latestMsg = t.getLatestMessage();
            if (latestMsg != null && latestMsg.asText != null) {
                latestValue = escapeHtml(latestMsg.asText);
                cssClass = "value-cell";
            }
            tableRows.append("<tr><td>")
                .append(escapeHtml(t.name))
                .append("</td><td class='")
                .append(cssClass)
                .append("'>")
                .append(latestValue)
                .append("</td></tr>");
        }
        String html = "<html><head><style>"
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
            + tableRows.toString()
            + "</table></div>"
            + "<div class='status-bar'>"
            + "<span class='status-indicator'></span>"
            + "Real-time topic values updated successfully"
            + "</div>"
            + "</div></body></html>";
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html;
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