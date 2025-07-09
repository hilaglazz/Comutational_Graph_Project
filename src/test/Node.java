package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Node {
    private String name;
    private List<Node> edges;
    private Message msg;
    private Double value;
    private String operation;

    public	Node(String name) {
    	this.name = name;
        this.edges = new ArrayList<>();
    	this.msg = new Message(""); 
        this.value = null;
        this.operation = null;
    }
    
    //Getters
    public String getName() { return this.name; }
    public List<Node> getEdges(){ return this.edges; }
    public Message getMsg() { return this.msg; }
    public Double getValue() { return this.value; }
    public String getOperation() { return this.operation; }
    //Setters
    public void setName(String name) { this.name = name; }
    public void setEdges(List<Node> edges) { this.edges = edges; }
    public void setMsg(Message msg) { this.msg = msg; }
    public void setValue(Double value) { this.value = value; }
    public void setOperation(String operation) { this.operation = operation; }
    public void addEdge(Node n) {
    	this.edges.add(n);
    }
    
    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();
        Set<Node> stack = new HashSet<>(); 
        return dfs(this, visited, stack);
    }

    private boolean dfs(Node node, Set<Node> visited, Set<Node> stack) {
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
            if (dfs(neighbor, visited, stack)) {
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
        return name.equals(node.name); 
    }

    @Override
    public int hashCode() {
        // Use the hash code of the name field
        return name.hashCode(); 
    }

    public boolean isTopic() {
        return name.startsWith("T");
    }

    public boolean isAgent() {
        return name.startsWith("A");
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
}
