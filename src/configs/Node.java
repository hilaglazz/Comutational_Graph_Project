package configs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.Message;

/**
 * Represents a node in the computational graph.
 * A node can be a topic or an agent, and stores its edges, value, and operation.
 */
public class Node {
    private static final int MAX_EDGES = 100;
    
    private String name;
    private List<Node> edges;
    private Message msg;
    private Double value;
    private String operation;

    
    // Create a new node with the given name.
    public Node(String name) {
        if (name == null || name.trim().isEmpty()) { // throw an exception if the node name is null or empty
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        this.name = name;
        this.edges = new ArrayList<>();
        this.msg = new Message(""); 
        this.value = null;
        this.operation = null;
    }
    
    // Getters for the node's properties
    public String getName() { return this.name; }
    public List<Node> getEdges(){ return new ArrayList<>(this.edges); }
    public Message getMsg() { return this.msg; }
    public Double getValue() { return this.value; }
    public String getOperation() { return this.operation; }
    public int getEdgeCount() { return this.edges.size(); }
    
    // Setters for the node's properties
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        this.name = name;
    }
    public void setEdges(List<Node> edges) {
        if (edges == null) {
            this.edges = new ArrayList<>();
        } else {
            this.edges = new ArrayList<>(edges);
        }
    }
    // Set the message for the node
    public void setMsg(Message msg) {
        if (msg == null) { // throw an exception if the message is null
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.msg = msg;
    }
    // Set the value for the node
    public void setValue(Double value) { this.value = value; }
    // Set the operation for the node
    public void setOperation(String operation) { this.operation = operation; }
    
    // Add an edge from this node to another node
    public void addEdge(Node n) {
        if (n == null) throw new IllegalArgumentException("Edge node cannot be null");
        if (n.equals(this)) throw new IllegalArgumentException("Cannot add self-edge");
        if (this.edges.size() >= MAX_EDGES) throw new IllegalStateException("Maximum edges reached: " + MAX_EDGES);
        if (!this.edges.contains(n)) this.edges.add(n);
    }
    
    // Remove an edge from this node to another node
    public void removeEdge(Node n) {
        if (n == null) return;
        this.edges.remove(n);
    }
   
    // Detect if there are cycles reachable from this node.
    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();
        Set<Node> stack = new HashSet<>(); 
        return dfs(this, visited, stack);
    }
    
    // Helper for cycle detection
    private boolean dfs(Node node, Set<Node> visited, Set<Node> stack) {
        if (node == null) return false;
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        stack.add(node);
        for (Node neighbor : node.getEdges()) {
            if (neighbor != null && dfs(neighbor, visited, stack)) return true;
        }
        stack.remove(node);
        return false;
    }
    
    // Check if two nodes are equal
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return name != null && name.equals(node.name);
    }
    
    // Get the hash code for the node
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
    
    // Check if the node is a topic
    public boolean isTopic() {
        return name != null && name.startsWith("T");
    }
    
    // Check if the node is an agent
    public boolean isAgent() {
        return name != null && name.startsWith("A");
    }
    
    // Get the type of node: "topic", "agent", or "unknown"
    public String getNodeType() {
        if (isTopic()) return "topic";
        if (isAgent()) return "agent";
        return "unknown";
    }
    
    // Get the value for topics, or the operation for agents
    public String getNodeValue() {
        return isTopic() ? String.valueOf(value) : operation;
    }
    
    // Get the operation for agents, or null for topics
    public String getNodeOperation() {
        return operation;
    }
    
    // Get the string representation of the node
    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", edges=" + edges.size() +
                '}';
    }
}
