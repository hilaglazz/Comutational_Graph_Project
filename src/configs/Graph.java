package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.Set;

import graph.Agent;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

public class Graph extends ArrayList<Node>{
    private static final Logger LOGGER = Logger.getLogger(Graph.class.getName());
    private static final int MAX_NODES = 1000;
    private static final int MAX_EDGES_PER_NODE = 100;
    
    public Graph() {
        super();
        LOGGER.fine("Graph created");
    }
    
    public boolean hasCycles() {
        try {
            if (this.isEmpty()) {
                LOGGER.fine("Empty graph, no cycles possible");
                return false;
            }
            
            for (Node node : this) {
                if (node == null) {
                    LOGGER.warning("Found null node in graph");
                    continue;
                }
                
                if (node.hasCycles()) {
                    LOGGER.info("Cycle detected in graph");
                    return true;
                }
            }
            LOGGER.fine("No cycles detected in graph");
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking for cycles", e);
            throw new RuntimeException("Error checking for cycles", e);
        }
    }
    
    public void createFromTopics() {
        LOGGER.info("Creating graph from topics");
        
        try {
            TopicManager topicManager = TopicManagerSingleton.get();
            if (topicManager == null) {
                LOGGER.severe("TopicManager is null");
                throw new RuntimeException("TopicManager not available");
            }
            
            // Create a map to store nodes by their names
            Map<String, Node> nodeMap = new HashMap<>(); 

            // Create nodes for all topics
            for (Topic topic : topicManager.getTopics()) {
                if (topic == null) {
                    LOGGER.warning("Found null topic, skipping");
                    continue;
                }
                
                if (topic.name == null || topic.name.trim().isEmpty()) {
                    LOGGER.warning("Found topic with null or empty name, skipping");
                    continue;
                }
                
                String nodeName = "T" + topic.name;
                
                // Check node limit
                if (nodeMap.size() >= MAX_NODES) {
                    LOGGER.warning("Maximum nodes reached: " + MAX_NODES);
                    break;
                }
                
                // Check if the node already exists in the map, and create it if it doesn't
                Node node = nodeMap.computeIfAbsent(nodeName, (name) -> {
                    try {
                        return new Node(name);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error creating node: " + name, e);
                        return null;
                    }
                });
                
                if (node != null) {
                    LOGGER.fine("Added topic node: " + nodeName);
                }
            }

            // --- Build graph: for each agent, add edges from pubs (topic → agent) and to subs (agent → topic)
            // Collect all agents
            Set<Agent> allAgents = new HashSet<>();
            for (Topic topic : topicManager.getTopics()) {
                allAgents.addAll(topic.getSubscribers());
                allAgents.addAll(topic.getPublishers());
            }
            for (Agent agent : allAgents) {
                if (agent == null) continue;
                String agentName = agent.getName();
                if (agentName == null || agentName.trim().isEmpty()) continue;
                String agentNodeName = "A" + agentName;
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, (name) -> {
                    try { return new Node(name); } catch (Exception e) { return null; }
                });
                if (agentNode == null) continue;
                try {
                    // Use reflection to get pubs and subs arrays
                    java.lang.reflect.Method getPubs = agent.getClass().getMethod("getPubs");
                    java.lang.reflect.Method getSubs = agent.getClass().getMethod("getSubs");
                    String[] pubs = (String[]) getPubs.invoke(agent);
                    String[] subs = (String[]) getSubs.invoke(agent);
                    // For each pub: topic → agent
                    for (String pub : pubs) {
                        Node topicNode = nodeMap.get("T" + pub);
                        if (topicNode != null && topicNode.getEdgeCount() < MAX_EDGES_PER_NODE) {
                            try { topicNode.addEdge(agentNode); } catch (Exception e) {}
                        }
                    }
                    // For each sub: agent → topic
                    for (String sub : subs) {
                        Node topicNode = nodeMap.get("T" + sub);
                        if (topicNode != null && agentNode.getEdgeCount() < MAX_EDGES_PER_NODE) {
                            try { agentNode.addEdge(topicNode); } catch (Exception e) {}
                        }
                    }
                } catch (Exception e) {
                    // If agent does not have getPubs/getSubs, skip
                    continue;
                }
            }

            // Copy all nodes from the nodeMap to the graph
            // Clear the graph to avoid leftover nodes from previous calls
            this.clear(); 
            
            // Add all unique nodes to the graph
            for (Node node : nodeMap.values()) {
                if (node != null) {
                    this.add(node);
                }
            }
            
            LOGGER.info("Graph created successfully with " + this.size() + " nodes");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating graph from topics", e);
            throw new RuntimeException("Error creating graph from topics", e);
        }
    }
    
    public boolean isEmpty() {
        return super.isEmpty();
    }
    
    public int getNodeCount() {
        return this.size();
    }
    
    public int getEdgeCount() {
        int totalEdges = 0;
        for (Node node : this) {
            if (node != null) {
                totalEdges += node.getEdgeCount();
            }
        }
        return totalEdges;
    }
    
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
    
    @Override
    public String toString() {
        return "Graph{nodes=" + this.size() + ", edges=" + getEdgeCount() + "}";
    }
}
