package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

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

            // Create nodes for agents and add edges from topics to agents
            for (Topic topic : topicManager.getTopics()) {
                if (topic == null || topic.name == null) {
                    continue;
                }
                
                Node topicNode = nodeMap.get("T" + topic.name);
                if (topicNode == null) {
                    LOGGER.warning("Topic node not found for: " + topic.name);
                    continue;
                }
                
                for (Agent agent : topic.getSubscribers()) {
                    if (agent == null) {
                        LOGGER.warning("Found null agent in topic subscribers: " + topic.name);
                        continue;
                    }
                    
                    String agentName = agent.getName();
                    if (agentName == null || agentName.trim().isEmpty()) {
                        LOGGER.warning("Agent has null or empty name in topic: " + topic.name);
                        continue;
                    }
                    
                    String agentNodeName = "A" + agentName;
                    
                    // Check node limit
                    if (nodeMap.size() >= MAX_NODES) {
                        LOGGER.warning("Maximum nodes reached: " + MAX_NODES);
                        break;
                    }
                    
                    // Check if the agent node exists in the nodeMap, and create it if it doesn't
                    Node agentNode = nodeMap.computeIfAbsent(agentNodeName, (name) -> {
                        try {
                            return new Node(name);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error creating agent node: " + name, e);
                            return null;
                        }
                    });
                    
                    if (agentNode != null) {
                        // Check edge limit
                        if (topicNode.getEdgeCount() >= MAX_EDGES_PER_NODE) {
                            LOGGER.warning("Maximum edges reached for node: " + topicNode.getName());
                            continue;
                        }
                        
                        // Add edge from topic to agent
                        try {
                            topicNode.addEdge(agentNode);
                            LOGGER.fine("Added edge from topic " + topic.name + " to agent " + agentName);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error adding edge from topic to agent", e);
                        }
                    }
                }
            }

            // Add edges from agents to topics they publish to
            for (Topic topic : topicManager.getTopics()) {
                if (topic == null || topic.name == null) {
                    continue;
                }
                
                for (Agent agent : topic.getPublishers()) {
                    if (agent == null) {
                        LOGGER.warning("Found null agent in topic publishers: " + topic.name);
                        continue;
                    }
                    
                    String agentName = agent.getName();
                    if (agentName == null || agentName.trim().isEmpty()) {
                        LOGGER.warning("Agent has null or empty name in topic publishers: " + topic.name);
                        continue;
                    }
                    
                    String agentNodeName = "A" + agentName;
                    
                    // Check if the agent node exists in the nodeMap, and create it if it doesn't
                    Node agentNode = nodeMap.computeIfAbsent(agentNodeName, (name) -> {
                        try {
                            return new Node(name);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error creating agent node: " + name, e);
                            return null;
                        }
                    });

                    if (agentNode != null) {
                        String topicNodeName = "T" + topic.name;
                        // Get the topic node
                        Node topicNode = nodeMap.get(topicNodeName);
                        
                        if (topicNode != null) {
                            // Check edge limit
                            if (agentNode.getEdgeCount() >= MAX_EDGES_PER_NODE) {
                                LOGGER.warning("Maximum edges reached for agent node: " + agentNode.getName());
                                continue;
                            }
                            
                            // Add edge from agent to topic 
                            try {
                                agentNode.addEdge(topicNode);
                                LOGGER.fine("Added edge from agent " + agentName + " to topic " + topic.name);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Error adding edge from agent to topic", e);
                            }
                        } else {
                            LOGGER.warning("Topic node not found for: " + topicNodeName);
                        }
                    }
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
