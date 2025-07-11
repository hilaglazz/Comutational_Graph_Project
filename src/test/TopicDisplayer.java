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
        // Show a table with all topics and their latest value
        StringBuilder tableRows = new StringBuilder();
        for (Topic t : tm.getTopics()) {
            String latestValue = "";
            Message latestMsg = t.getLatestMessage();
            if (latestMsg != null && latestMsg.asText != null) {
                latestValue = escapeHtml(latestMsg.asText);
            }
            tableRows.append("<tr><td>")
                .append(escapeHtml(t.name))
                .append("</td><td>")
                .append(latestValue)
                .append("</td></tr>");
        }
        String html = "<html><head><style>"
            + "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }"
            + "h2 { color: #333; margin-top: 0; padding-bottom: 10px; border-bottom: 1px solid #ccc; }"
            + "table { background-color: white; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); width: 100%; border-collapse: collapse; }"
            + "th, td { padding: 10px; border: 1px solid #ccc; text-align: left; }"
            + "th { background-color: #3399ff; color: white; }"
            + "</style></head><body>"
            + "<h2>Topics Table</h2>"
            + "<table><tr><th>Topic</th><th>Latest Value</th></tr>"
            + tableRows.toString()
            + "</table></body></html>";
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html;
        System.out.println("Debug: Sending HTTP 200 OK response."); // Debug
        System.out.println("Debug: Response: " + response); // Debug
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