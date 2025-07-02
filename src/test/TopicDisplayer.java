package test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import test.RequestParser.RequestInfo;

public class TopicDisplayer implements Servlet {
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        Map<String, String> params = ri.getParameters();
        String topicName = params.get("topic");
        String value = params.get("value");
        if (topicName == null || value == null) {
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
        // Show the topic and value sent (latest value)
        String html = "<html><body>"
            + "<h2>Message Published</h2>"
            + "<table border='1'><tr><th>Topic</th><th>Latest Value</th></tr>"
            + "<tr><td>" + escapeHtml(topicName) + "</td><td>" + escapeHtml(value) + "</td></tr>"
            + "</table></body></html>";
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