package graph;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a topic in the computational graph system.
 * Topics are used to publish and subscribe to messages.
 */
public class Topic {
    private static final int MAX_SUBSCRIBERS = 1000; // Maximum number of subscribers
    private static final int MAX_PUBLISHERS = 1000; // Maximum number of publishers
    
    public final String name; // The name of the topic
    private final CopyOnWriteArrayList<Agent> subs = new CopyOnWriteArrayList<>(); // The subscribers of the topic
    private final CopyOnWriteArrayList<Agent> pubs = new CopyOnWriteArrayList<>(); // The publishers of the topic
    
    private volatile Message latestMessage = null; // The latest message published to the topic
    
    // Constructor
    Topic(String name){
        if (name == null) {
            throw new IllegalArgumentException("Topic name cannot be null");
        }
        if (name.trim().isEmpty()) { // If the topic name is empty
            throw new IllegalArgumentException("Topic name cannot be empty");
        }
        this.name = name; // Set the name of the topic
    }

    // Subscribe an agent to the topic
    public void subscribe(Agent a){
        if (a == null) { // If the agent is null
            throw new IllegalArgumentException("Agent cannot be null");
        }
        
        if (subs.size() >= MAX_SUBSCRIBERS) { // If the number of subscribers is greater than the maximum number of subscribers
            throw new IllegalStateException("Maximum subscribers reached: " + MAX_SUBSCRIBERS);
        }
        
        if (subs.contains(a)) { // If the agent is already subscribed to the topic
            return;
        }
        
        try {
            subs.add(a); // Add the agent to the subscribers list
        } catch (Exception e) {
            throw new RuntimeException("Error subscribing agent to topic: " + name, e);
        }
    }
    
    // Unsubscribe an agent from the topic
    public void unsubscribe(Agent a){
        if (a == null) { // If the agent is null
            return;
        }
        
        try {
            subs.remove(a); // Remove the agent from the subscribers list
        } catch (Exception e) {
            throw new RuntimeException("Error unsubscribing agent from topic: " + name, e);
        }
    }

    // Publish a message to the topic
    public void publish(Message m){
        if (m == null) { // If the message is null
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            this.latestMessage = m; // Update latest message
            
            // Notify all subscribers
            for (Agent a : subs) { // For each subscriber
                if (a != null) {
                    try {
                        a.callback(this.name, m); // Call the agent's callback method
                    } catch (Exception e) {
                        // Continue with other subscribers even if one fails
                    }
                }
            }
        } catch (Exception e) { // If there is an error publishing the message
            throw new RuntimeException("Error publishing message to topic: " + name, e);
        }
    }

    // Add a publisher to the topic
    public void addPublisher(Agent a){
        if (a == null) { // If the agent is null
            throw new IllegalArgumentException("Publisher agent cannot be null");
        }
        
        if (pubs.size() >= MAX_PUBLISHERS) { // If the number of publishers is greater than the maximum number of publishers
            throw new IllegalStateException("Maximum publishers reached: " + MAX_PUBLISHERS);
        }
        
        if (pubs.contains(a)) { // If the agent is already a publisher
            return;
        }
        
        try {
            pubs.add(a); // Add the agent to the publishers list
        } catch (Exception e) { // If there is an error adding the publisher
            throw new RuntimeException("Error adding publisher to topic: " + name, e);
        }
    }

    // Remove a publisher from the topic
    public void removePublisher(Agent a){
        if (a == null) { // If the agent is null
            return;
        }
        
        try {
            pubs.remove(a); // Remove the agent from the publishers list
        } catch (Exception e) { // If there is an error removing the publisher
            throw new RuntimeException("Error removing publisher from topic: " + name, e);
        }
    }

    // Getters:

    // Get the latest message published to the topic
    public Message getLatestMessage() {
        return latestMessage;
    }
    
    // Get the subscribers of the topic
    public List<Agent> getSubscribers() {
        return Collections.unmodifiableList(subs);
    }
    
    // Get the publishers of the topic
    public List<Agent> getPublishers() {
        return Collections.unmodifiableList(pubs);
    }
    
    // Get the number of subscribers of the topic
    public int getSubscriberCount() {
        return subs.size();
    }
    
    // Get the number of publishers of the topic
    public int getPublisherCount() {
        return pubs.size();
    }
    
    // Check if the topic has subscribers
    public boolean hasSubscribers() {
        return !subs.isEmpty();
    }
    
    // Check if the topic has publishers
    public boolean hasPublishers() {
        return !pubs.isEmpty();
    }
    
    // Clear the subscribers of the topic
    public void clearSubscribers() {
        try {
            subs.clear();
        } catch (Exception e) { // If there is an error clearing the subscribers
            throw new RuntimeException("Error clearing subscribers from topic: " + name, e);
        }
    }
    
    // Clear the publishers of the topic
    public void clearPublishers() {
        try {
            pubs.clear();
        } catch (Exception e) { // If there is an error clearing the publishers
            throw new RuntimeException("Error clearing publishers from topic: " + name, e);
        }
    }
    
    // Convert the topic to a string
    @Override
    public String toString() {
        return "Topic{name='" + name + "', subscribers=" + subs.size() + ", publishers=" + pubs.size() + "}";
    }
}