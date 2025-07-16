package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import graph.Agent;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Represents the computational graph as a list of nodes (topics and agents).
 * Provides methods for cycle detection and graph construction from topics.
 */
public class Graph extends ArrayList<Node>{
    private static final int MAX_NODES = 1000;
    private static final int MAX_EDGES_PER_NODE = 100;
    
   
    // Create an empty graph.
    public Graph() {
        super();
    }
    
    // Detect if the graph contains any cycles.
    public boolean hasCycles() {
        try {
            if (this.isEmpty()) {
                return false;
            }
            // Check for cycles in the graph
            for (Node node : this) {
                if (node == null) {
                    continue;
                }
                // Check if the node has cycles
                if (node.hasCycles()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error checking for cycles", e);
        }
    }
    
    // Build the graph from the current topics in the TopicManager.
    public void createFromTopics() {
        try {
            TopicManager topicManager = TopicManagerSingleton.get();
            if (topicManager == null) {
                throw new RuntimeException("TopicManager not available");
            }
            
            // Create a map to store nodes by their names
            Map<String, Node> nodeMap = new HashMap<>(); 

            // Create nodes for all topics
            for (Topic topic : topicManager.getTopics()) {
                if (topic == null) {
                    continue;
                }
                
                if (topic.name == null || topic.name.trim().isEmpty()) {
                    continue;
                }
                
                String nodeName = "T" + topic.name; // Create a node name for the topic
                
                // Check node limit
                if (nodeMap.size() >= MAX_NODES) {
                    break;
                }
                
                // Check if the node already exists in the map, and create it if it doesn't
                Node node = nodeMap.computeIfAbsent(nodeName, (name) -> {
                    try {
                        return new Node(name);
                    } catch (Exception e) {
                        return null;
                    }
                });
                
                if (node != null) {
                }
            }

            // --- Build graph: for each agent, add edges from subs (topic → agent) and to pubs (agent → topic)
            // Collect all agents
            Set<Agent> allAgents = new HashSet<>();
            // Add all agents from all topics to the set
            for (Topic topic : topicManager.getTopics()) {
                allAgents.addAll(topic.getSubscribers());
                allAgents.addAll(topic.getPublishers());
            }
            // For each agent, add edges from subs (topic → agent) and to pubs (agent → topic)
            for (Agent agent : allAgents) {
                if (agent == null) continue;
                String agentName = agent.getName(); // Get the name of the agent
                if (agentName == null || agentName.trim().isEmpty()) continue; // Skip if the agent name is null or empty
                String agentNodeName = "A" + agentName; // Create a node name for the agent
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, (name) -> {
                    try { return new Node(name); } catch (Exception e) { return null; }
                });
                if (agentNode == null) continue;
                try {
                    // Use reflection to get pubs and subs arrays
                    java.lang.reflect.Method getSubs = agent.getClass().getMethod("getSubs");
                    java.lang.reflect.Method getPubs = agent.getClass().getMethod("getPubs");
                    String[] subs = (String[]) getSubs.invoke(agent);
                    String[] pubs = (String[]) getPubs.invoke(agent);
                   
                    // For each sub: topic → agent
                    for (String sub : subs) {
                        Node topicNode = nodeMap.get("T" + sub); // Get the topic node
                        if (topicNode != null && topicNode.getEdgeCount() < MAX_EDGES_PER_NODE) { // Add an edge from the topic to the agent
                            try { topicNode.addEdge(agentNode); } catch (Exception e) {} // Add an edge from the topic to the agent
                        }
                    }
                    // For each pub: agent → topic
                    for (String pub : pubs) {
                        Node topicNode = nodeMap.get("T" + pub);
                        if (topicNode != null && agentNode.getEdgeCount() < MAX_EDGES_PER_NODE) {
                            try { agentNode.addEdge(topicNode); } catch (Exception e) {}
                        }
                    }
                } catch (Exception e) {
                    // If agent does not have getPubs/getSubs, skip
                    continue;
                }
            }

            // Clear the graph to avoid leftover nodes from previous calls
            this.clear(); 
            
            // Add all unique nodes to the graph
            for (Node node : nodeMap.values()) {
                if (node != null) {
                    this.add(node);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating graph from topics", e);
        }
    }
    
    // Check if the graph is empty
    public boolean isEmpty() {
        return super.isEmpty();
    }
    
    // Get the number of nodes in the graph
    public int getNodeCount() {
        return this.size();
    }
    
    // Get the number of edges in the graph
    public int getEdgeCount() {
        int totalEdges = 0;
        for (Node node : this) {
            if (node != null) {
                totalEdges += node.getEdgeCount();
            }
        }
        return totalEdges;
    }
    
    // Get a node by name
    public Node getNode(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        for (Node node : this) {
            if (node != null && name.equals(node.getName())) {
                return node;
            }
        }
        return null;
    }
    
    // Get the string representation of the graph
    @Override
    public String toString() {
        return "Graph{nodes=" + this.size() + ", edges=" + getEdgeCount() + "}";
    }
}
