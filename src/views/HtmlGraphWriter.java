package views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import configs.Graph;
import configs.Node;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/*
 * HtmlGraphWriter is a class that writes the graph to an HTML file.
 * It is used to write the graph to an HTML file.
 * It is also used to get the graph HTML.
 */
public class HtmlGraphWriter {
    private static final String GRAPH_TEMPLATE_PATH = "html_files/graph.html"; // Path to the graph template
    
    // Get the graph HTML
    public static String getGraphHTML(Graph g) {
        if (g == null || g.isEmpty()) { // If the graph is null or empty, return an error message
            return "<html><body><h2>No graph loaded</h2></body></html>";
        }

        try {
            String template = new String(Files.readAllBytes(Paths.get(GRAPH_TEMPLATE_PATH))); // Load the template
            Map<String, Object> graphData = prepareGraphData(g); // Prepare the graph data
            return injectDataIntoTemplate(template, graphData); // Inject the data into the template
            
        // Catch the error if the template cannot be loaded
        } catch (IOException e) {
            System.err.println("Error loading graph template: " + e.getMessage());
            return "<html><body><h2>Error loading graph template</h2></body></html>";
        }
    }

    // Prepare the graph data
    private static Map<String, Object> prepareGraphData(Graph g) {
        Map<String, Object> data = new HashMap<>(); // Create a new map for the data
        
        List<Map<String, Object>> nodes = new ArrayList<>(); // Create a new list for the nodes
        List<Map<String, String>> links = new ArrayList<>(); // Create a new list for the links
        
        // Get topic manager to access current values
        TopicManager topicManager = TopicManagerSingleton.get(); // Get the topic manager
        
        for (Node node : g) {
            Map<String, Object> nodeData = new HashMap<>(); // Create a new map for the node data
            String nodeName = node.getName(); // Get the node name
            nodeData.put("id", nodeName); // Add the node name to the node data
            nodeData.put("type", nodeName.startsWith("T") ? "topic" : "agent"); // Add the node type to the node data
            
            // Add label without prefix
            if (nodeName.startsWith("T") || nodeName.startsWith("A")) { // If the node name starts with T or A
                nodeData.put("label", nodeName.substring(1)); // Add the node name to the node data
            } else {
                nodeData.put("label", nodeName); // Add the node name to the node data
            }
            
            // Add enhanced information based on node type
            if (nodeName.startsWith("T")) { // If the node name starts with T
                // Topic node - add value display fields
                String topicName = nodeName.substring(1); // Get the topic name
                Topic topic = topicManager.getTopic(topicName); // Get the topic
                String value = "0"; // Default value
                if (topic != null && topic.getLatestMessage() != null) { // If the topic is not null and the latest message is not null
                    value = topic.getLatestMessage().asText; // Get the latest message
                }
                // (No value fields added)
                nodeData.put("operationType", "Data Storage"); // Add the operation type to the node data
                nodeData.put("description", "Stores data values for computational operations"); // Add the description to the node data
                nodeData.put("value", value); // Add the value to the node data
            } else if (nodeName.startsWith("A")) {
                // Agent node - add operation information
                String agentName = nodeName.substring(1); // Get the agent name
                nodeData.put("operationType", getAgentOperationType(agentName)); // Add the operation type to the node data
                nodeData.put("mathematicalExpression", getAgentExpression(agentName)); // Add the mathematical expression to the node data
                nodeData.put("description", "Performs mathematical operations on input data"); // Add the description to the node data
            }
            
            nodes.add(nodeData); // Add the node data to the nodes list
            
            // Add edges
            for (Node edge : node.getEdges()) {
                Map<String, String> link = new HashMap<>(); // Create a new map for the link
                link.put("source", node.getName()); // Add the source to the link
                link.put("target", edge.getName()); // Add the target to the link
                links.add(link);
            }
        }
        
        data.put("nodes", nodes); // Add the nodes to the data
        data.put("links", links); // Add the links to the data
        return data;
    }
    
    // Get the agent operation type
    private static String getAgentOperationType(String agentName) {
        // Determine operation type based on agent name patterns
        if (agentName.toLowerCase().contains("plus") || agentName.toLowerCase().contains("add")) {
            return "Addition";
        } else if (agentName.toLowerCase().contains("inc") || agentName.toLowerCase().contains("increment")) {
            return "Increment";
        } else if (agentName.toLowerCase().contains("mult") || agentName.toLowerCase().contains("multiply")) {
            return "Multiplication";
        } else if (agentName.toLowerCase().contains("div") || agentName.toLowerCase().contains("divide")) {
            return "Division";
        } else if (agentName.toLowerCase().contains("sub") || agentName.toLowerCase().contains("subtract")) {
            return "Subtraction";
        } else {
            return "Operation";
        }
    }
    
    // Get the agent expression
    private static String getAgentExpression(String agentName) {
        // Generate mathematical expression based on agent type
        if (agentName.toLowerCase().contains("plus") || agentName.toLowerCase().contains("add")) {
            return "a + b";
        } else if (agentName.toLowerCase().contains("inc") || agentName.toLowerCase().contains("increment")) {
            return "x + 1";
        } else if (agentName.toLowerCase().contains("mul") || agentName.toLowerCase().contains("multiply")) {
            return "a × b";
        } else if (agentName.toLowerCase().contains("div") || agentName.toLowerCase().contains("divide")) {
            return "a ÷ b";
        } else if (agentName.toLowerCase().contains("sub") || agentName.toLowerCase().contains("subtract")) {
            return "a - b";
        } else {
            return "f(x)";
        }
    }

    // Inject the data into the template
    private static String injectDataIntoTemplate(String template, Map<String, Object> data) {
        // Convert data to JSON string
        String jsonData = "const graphData = " + toJson(data) + ";";
        
        // Replace the placeholder
        return template.replace("/*DATA_PLACEHOLDER*/", jsonData);
    }

    // Convert the data to JSON
    private static String toJson(Object obj) {
        if (obj instanceof Map) { // If the object is a map
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        } else if (obj instanceof Collection) { // If the object is a collection
            Collection<?> coll = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            return sb.append("]").toString();
        } else if (obj instanceof String) { // If the object is a string
            return "\"" + escapeJson(obj.toString()) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) { // If the object is a number or a boolean
            return obj.toString();
        } else { // If the object is not a map, collection, string, number, or boolean
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }

    // Escape the JSON
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\b", "\\b")
                 .replace("\f", "\\f")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
