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

public class HtmlGraphWriter {
    private static final String GRAPH_TEMPLATE_PATH = "html_files/graph.html";
    
    public static String getGraphHTML(Graph g) {
        if (g == null || g.isEmpty()) {
            return "<html><body><h2>No graph loaded</h2></body></html>";
        }

        try {
            // 1. Load template
            String template = new String(Files.readAllBytes(Paths.get(GRAPH_TEMPLATE_PATH)));
            
            // 2. Prepare graph data
            Map<String, Object> graphData = prepareGraphData(g);
            
            // 3. Inject data into template
            return injectDataIntoTemplate(template, graphData);
            
        } catch (IOException e) {
            System.err.println("Error loading graph template: " + e.getMessage());
            return "<html><body><h2>Error loading graph template</h2></body></html>";
        }
    }

    private static Map<String, Object> prepareGraphData(Graph g) {
        Map<String, Object> data = new HashMap<>();
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, String>> links = new ArrayList<>();
        
        // Get topic manager to access current values
        TopicManager topicManager = TopicManagerSingleton.get();
        
        for (Node node : g) {
            Map<String, Object> nodeData = new HashMap<>();
            String nodeName = node.getName();
            nodeData.put("id", nodeName);
            nodeData.put("type", nodeName.startsWith("T") ? "topic" : "agent");
            
            // Add label without prefix
            if (nodeName.startsWith("T") || nodeName.startsWith("A")) {
                nodeData.put("label", nodeName.substring(1));
            } else {
                nodeData.put("label", nodeName);
            }
            
            // Add enhanced information based on node type
            if (nodeName.startsWith("T")) {
                // Topic node - do not add value display fields
                String topicName = nodeName.substring(1);
                Topic topic = topicManager.getTopic(topicName);
                // (No value fields added)
                nodeData.put("operationType", "Data Storage");
                nodeData.put("description", "Stores data values for computational operations");
            } else if (nodeName.startsWith("A")) {
                // Agent node - add operation information
                String agentName = nodeName.substring(1);
                nodeData.put("operationType", getAgentOperationType(agentName));
                nodeData.put("mathematicalExpression", getAgentExpression(agentName));
                nodeData.put("description", "Performs mathematical operations on input data");
            }
            
            nodes.add(nodeData);
            
            // Add edges
            for (Node edge : node.getEdges()) {
                Map<String, String> link = new HashMap<>();
                link.put("source", node.getName());
                link.put("target", edge.getName());
                links.add(link);
            }
        }
        
        data.put("nodes", nodes);
        data.put("links", links);
        return data;
    }
    
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

    private static String injectDataIntoTemplate(String template, Map<String, Object> data) {
        // Convert data to JSON string
        String jsonData = "const graphData = " + toJson(data) + ";";
        
        // Replace the placeholder
        return template.replace("/*DATA_PLACEHOLDER*/", jsonData);
    }

    private static String toJson(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        } else if (obj instanceof Collection) {
            Collection<?> coll = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            return sb.append("]").toString();
        } else if (obj instanceof String) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }

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
