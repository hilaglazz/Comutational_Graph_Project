package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Topic {
    private static final Logger LOGGER = Logger.getLogger(Topic.class.getName());
    private static final int MAX_SUBSCRIBERS = 1000;
    private static final int MAX_PUBLISHERS = 1000;
    
    public final String name;
    private final CopyOnWriteArrayList<Agent> subs = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Agent> pubs = new CopyOnWriteArrayList<>();
    
    private volatile Message latestMessage = null;
    
    Topic(String name){
        if (name == null) {
            throw new IllegalArgumentException("Topic name cannot be null");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic name cannot be empty");
        }
        this.name = name;
        LOGGER.fine("Topic created: " + name);
    }

    public void subscribe(Agent a){
        if (a == null) {
            LOGGER.warning("Attempted to subscribe null agent to topic: " + name);
            throw new IllegalArgumentException("Agent cannot be null");
        }
        
        if (subs.size() >= MAX_SUBSCRIBERS) {
            LOGGER.warning("Maximum subscribers reached for topic: " + name);
            throw new IllegalStateException("Maximum subscribers reached: " + MAX_SUBSCRIBERS);
        }
        
        if (subs.contains(a)) {
            LOGGER.fine("Agent already subscribed to topic: " + name);
            return;
        }
        
        try {
            subs.add(a);
            LOGGER.fine("Agent subscribed to topic: " + name + ", total subscribers: " + subs.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error subscribing agent to topic: " + name, e);
            throw new RuntimeException("Error subscribing agent to topic: " + name, e);
        }
    }
    
    public void unsubscribe(Agent a){
        if (a == null) {
            LOGGER.warning("Attempted to unsubscribe null agent from topic: " + name);
            return;
        }
        
        try {
            boolean removed = subs.remove(a);
            if (removed) {
                LOGGER.fine("Agent unsubscribed from topic: " + name + ", remaining subscribers: " + subs.size());
            } else {
                LOGGER.fine("Agent was not subscribed to topic: " + name);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error unsubscribing agent from topic: " + name, e);
            throw new RuntimeException("Error unsubscribing agent from topic: " + name, e);
        }
    }

    public void publish(Message m){
        if (m == null) {
            LOGGER.warning("Attempted to publish null message to topic: " + name);
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            this.latestMessage = m; // Update latest message
            LOGGER.fine("Message published to topic: " + name + ", subscribers: " + subs.size());
            
            // Notify all subscribers
            for (Agent a : subs) {
                if (a != null) {
                    try {
                        a.callback(this.name, m);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in agent callback for topic: " + name, e);
                        // Continue with other subscribers even if one fails
                    }
                } else {
                    LOGGER.warning("Null agent found in subscribers list for topic: " + name);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error publishing message to topic: " + name, e);
            throw new RuntimeException("Error publishing message to topic: " + name, e);
        }
    }

    public void addPublisher(Agent a){
        if (a == null) {
            LOGGER.warning("Attempted to add null publisher to topic: " + name);
            throw new IllegalArgumentException("Publisher agent cannot be null");
        }
        
        if (pubs.size() >= MAX_PUBLISHERS) {
            LOGGER.warning("Maximum publishers reached for topic: " + name);
            throw new IllegalStateException("Maximum publishers reached: " + MAX_PUBLISHERS);
        }
        
        if (pubs.contains(a)) {
            LOGGER.fine("Agent already a publisher for topic: " + name);
            return;
        }
        
        try {
            pubs.add(a);
            LOGGER.fine("Publisher added to topic: " + name + ", total publishers: " + pubs.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding publisher to topic: " + name, e);
            throw new RuntimeException("Error adding publisher to topic: " + name, e);
        }
    }

    public void removePublisher(Agent a){
        if (a == null) {
            LOGGER.warning("Attempted to remove null publisher from topic: " + name);
            return;
        }
        
        try {
            boolean removed = pubs.remove(a);
            if (removed) {
                LOGGER.fine("Publisher removed from topic: " + name + ", remaining publishers: " + pubs.size());
            } else {
                LOGGER.fine("Agent was not a publisher for topic: " + name);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error removing publisher from topic: " + name, e);
            throw new RuntimeException("Error removing publisher from topic: " + name, e);
        }
    }

    public Message getLatestMessage() {
        return latestMessage;
    }
    
    public List<Agent> getSubscribers() {
        return Collections.unmodifiableList(subs);
    }
    
    public List<Agent> getPublishers() {
        return Collections.unmodifiableList(pubs);
    }
    
    public int getSubscriberCount() {
        return subs.size();
    }
    
    public int getPublisherCount() {
        return pubs.size();
    }
    
    public boolean hasSubscribers() {
        return !subs.isEmpty();
    }
    
    public boolean hasPublishers() {
        return !pubs.isEmpty();
    }
    
    public void clearSubscribers() {
        try {
            int count = subs.size();
            subs.clear();
            LOGGER.info("Cleared " + count + " subscribers from topic: " + name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error clearing subscribers from topic: " + name, e);
            throw new RuntimeException("Error clearing subscribers from topic: " + name, e);
        }
    }
    
    public void clearPublishers() {
        try {
            int count = pubs.size();
            pubs.clear();
            LOGGER.info("Cleared " + count + " publishers from topic: " + name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error clearing publishers from topic: " + name, e);
            throw new RuntimeException("Error clearing publishers from topic: " + name, e);
        }
    }
    
    @Override
    public String toString() {
        return "Topic{name='" + name + "', subscribers=" + subs.size() + ", publishers=" + pubs.size() + "}";
    }
}