package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import graph.Agent;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

public class Graph extends ArrayList<Node>{
    
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) {
                return true;
            }
        }
        return false;
    }
    public void createFromTopics() {
        System.out.println("in create from topics");
        TopicManager topicManager = TopicManagerSingleton.get();
        // Create a map to store nodes by their names
        Map<String, Node> nodeMap = new HashMap<>(); 

        // Create nodes for all topics
        for (Topic topic : topicManager.getTopics()) {
            String nodeName = "T" + topic.name;
            // Check if the node already exists in the map, and create it if it doesn't
            nodeMap.computeIfAbsent(nodeName, Node::new); 
            System.out.println("add " + nodeName + "to node map");
        }

        // Create nodes for agents and add edges from topics to agents
        for (Topic topic : topicManager.getTopics()) {
            Node topicNode = nodeMap.get("T" + topic.name);
            for (Agent agent : topic.subs) {
                String agentNodeName = "A" + agent.getName();
                // Check if the agent node exists in the nodeMap, and create it if it doesn't
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, Node::new); 
                // Add edge from topic to agent
                topicNode.addEdge(agentNode); 
                System.out.println("added edge from " + topicNode + "to " + agentNodeName);
            }
        }

        // Add edges from agents to topics they publish to
        for (Topic topic : topicManager.getTopics()) {
            for (Agent agent : topic.pubs) {
                String agentNodeName = "A" + agent.getName();
                // Check if the agent node exists in the nodeMap, and create it if it doesn't
                Node agentNode = nodeMap.computeIfAbsent(agentNodeName, Node::new);

                String topicNodeName = "T" + topic.name;
                // Get the topic node
                Node topicNode = nodeMap.get(topicNodeName);
                System.out.println("found topic " + topicNodeName + "in nodemap");
                // Add edge from agent to topic 
                agentNode.addEdge(topicNode); 
                System.out.println("added edge from " + agentNode + "to " + topicNode);
            }
        }

        // Copy all nodes from the nodeMap to the graph
        // Clear the graph to avoid leftover nodes from previous calls
        this.clear(); 
        // Add all unique nodes to the graph
        this.addAll(nodeMap.values()); 
    }   
}
