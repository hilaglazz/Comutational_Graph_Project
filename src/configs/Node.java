package configs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import graph.Message;


public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private static final int MAX_EDGES = 100;
    
    private String name;
    private List<Node> edges;
    private Message msg;
    private Double value;
    private String operation;

    public Node(String name) {
        if (name == null || name.trim().isEmpty()) {
            LOGGER.severe("Attempted to create node with null or empty name");
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        
        this.name = name;
        this.edges = new ArrayList<>();
        this.msg = new Message(""); 
        this.value = null;
        this.operation = null;
        LOGGER.fine("Node created: " + name);
    }
    
    //Getters
    public String getName() { 
        return this.name; 
    }
    
    public List<Node> getEdges(){ 
        return new ArrayList<>(this.edges); // Return defensive copy
    }
    
    public Message getMsg() { 
        return this.msg; 
    }
    
    public Double getValue() { 
        return this.value; 
    }
    
    public String getOperation() { 
        return this.operation; 
    }
    
    public int getEdgeCount() {
        return this.edges.size();
    }
    
    //Setters
    public void setName(String name) { 
        if (name == null || name.trim().isEmpty()) {
            LOGGER.warning("Attempted to set null or empty name for node: " + this.name);
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        this.name = name;
        LOGGER.fine("Node name updated: " + name);
    }
    
    public void setEdges(List<Node> edges) { 
        if (edges == null) {
            LOGGER.warning("Attempted to set null edges for node: " + this.name);
            this.edges = new ArrayList<>();
        } else {
            this.edges = new ArrayList<>(edges); // Defensive copy
        }
        LOGGER.fine("Node edges updated for: " + this.name + ", count: " + this.edges.size());
    }
    
    public void setMsg(Message msg) { 
        if (msg == null) {
            LOGGER.warning("Attempted to set null message for node: " + this.name);
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.msg = msg;
        LOGGER.fine("Node message updated for: " + this.name);
    }
    
    public void setValue(Double value) { 
        this.value = value;
        LOGGER.fine("Node value updated for: " + this.name + " to: " + value);
    }
    
    public void setOperation(String operation) { 
        this.operation = operation;
        LOGGER.fine("Node operation updated for: " + this.name + " to: " + operation);
    }
    
    public void addEdge(Node n) {
        if (n == null) {
            LOGGER.warning("Attempted to add null edge to node: " + this.name);
            throw new IllegalArgumentException("Edge node cannot be null");
        }
        
        if (n.equals(this)) {
            LOGGER.warning("Attempted to add self-edge to node: " + this.name);
            throw new IllegalArgumentException("Cannot add self-edge");
        }
        
        if (this.edges.size() >= MAX_EDGES) {
            LOGGER.warning("Maximum edges reached for node: " + this.name);
            throw new IllegalStateException("Maximum edges reached: " + MAX_EDGES);
        }
        
        if (this.edges.contains(n)) {
            LOGGER.fine("Edge already exists for node: " + this.name + " to: " + n.getName());
            return;
        }
        
        this.edges.add(n);
        LOGGER.fine("Edge added from " + this.name + " to " + n.getName());
    }
    
    public void removeEdge(Node n) {
        if (n == null) {
            LOGGER.warning("Attempted to remove null edge from node: " + this.name);
            return;
        }
        
        boolean removed = this.edges.remove(n);
        if (removed) {
            LOGGER.fine("Edge removed from " + this.name + " to " + n.getName());
        } else {
            LOGGER.fine("Edge not found from " + this.name + " to " + n.getName());
        }
    }
    
    public boolean hasCycles() {
        try {
            Set<Node> visited = new HashSet<>();
            Set<Node> stack = new HashSet<>(); 
            boolean hasCycle = dfs(this, visited, stack);
            LOGGER.fine("Cycle detection for node " + this.name + ": " + hasCycle);
            return hasCycle;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error detecting cycles for node: " + this.name, e);
            throw new RuntimeException("Error detecting cycles", e);
        }
    }

    private boolean dfs(Node node, Set<Node> visited, Set<Node> stack) {
        if (node == null) {
            return false;
        }
        
        // Cycle detected
        if (stack.contains(node)) { 
            return true;
        }
        
        // Already processed
        if (visited.contains(node)) {
            return false; 
        }

        visited.add(node);
        stack.add(node);

        for (Node neighbor : node.getEdges()) {
            if (neighbor != null && dfs(neighbor, visited, stack)) {
                return true;
            }
        }

        //Backtrack
        stack.remove(node);
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        // Check if the object is the same instance
        if (this == obj) return true; 
        // Check if the object is of the same class
        if (obj == null || getClass() != obj.getClass()) return false; 
        Node node = (Node) obj;
        // Compare the names of the nodes
        return name != null && name.equals(node.name); 
    }

    @Override
    public int hashCode() {
        // Use the hash code of the name field
        return name != null ? name.hashCode() : 0; 
    }

    public boolean isTopic() {
        return name != null && name.startsWith("T");
    }

    public boolean isAgent() {
        return name != null && name.startsWith("A");
    }

    public String getNodeType() {
        return isTopic() ? "topic" : "agent";
    } 
    
    public String getNodeValue() {
        return isTopic() ? String.valueOf(value) : operation;
    }  
    
    public String getNodeOperation() {
        return isAgent() ? operation : null;
    }
    
    @Override
    public String toString() {
        return "Node{name='" + name + "', edges=" + edges.size() + ", type=" + getNodeType() + "}";
    }
}
