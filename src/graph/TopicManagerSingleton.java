package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TopicManagerSingleton {
    private static final Logger LOGGER = Logger.getLogger(TopicManagerSingleton.class.getName());
    private static final int MAX_TOPIC_NAME_LENGTH = 100;
    private static final int MAX_TOPICS = 1000;
    
	public static class TopicManager{
		private static final TopicManager instance = new TopicManager();
		private final ConcurrentHashMap<String, Topic> map;
		
    	private TopicManager() {
    		map = new ConcurrentHashMap<>();
    		LOGGER.info("TopicManager instance created");
    	}
    	
    	public Topic getTopic(String name) {
    	    // Validate topic name
    	    if (name == null) {
    	        LOGGER.warning("Attempted to get topic with null name");
    	        throw new IllegalArgumentException("Topic name cannot be null");
    	    }
    	    
    	    if (name.trim().isEmpty()) {
    	        LOGGER.warning("Attempted to get topic with empty name");
    	        throw new IllegalArgumentException("Topic name cannot be empty");
    	    }
    	    
    	    if (name.length() > MAX_TOPIC_NAME_LENGTH) {
    	        LOGGER.warning("Topic name too long: " + name.length());
    	        throw new IllegalArgumentException("Topic name too long (max " + MAX_TOPIC_NAME_LENGTH + " characters)");
    	    }
    	    
    	    // Check if we're approaching the limit
    	    if (map.size() >= MAX_TOPICS) {
    	        LOGGER.warning("Maximum number of topics reached: " + MAX_TOPICS);
    	        throw new IllegalStateException("Maximum number of topics reached: " + MAX_TOPICS);
    	    }
    	    
    	    try {
    	        Topic topic = map.computeIfAbsent(name, (topicName) -> {
    	            LOGGER.fine("Creating new topic: " + topicName);
    	            return new Topic(topicName);
    	        });
    	        
    	        if (topic == null) {
    	            LOGGER.severe("Failed to create topic: " + name);
    	            throw new RuntimeException("Failed to create topic: " + name);
    	        }
    	        
    	        return topic;
    	        
    	    } catch (Exception e) {
    	        LOGGER.log(Level.SEVERE, "Error getting/creating topic: " + name, e);
    	        throw new RuntimeException("Error getting/creating topic: " + name, e);
    	    }
    	}
    	
    	public Collection<Topic> getTopics() {
    	    try {
    	        Collection<Topic> topics = map.values();
    	        LOGGER.fine("Retrieved " + topics.size() + " topics");
    	        return topics;
    	    } catch (Exception e) {
    	        LOGGER.log(Level.SEVERE, "Error retrieving topics", e);
    	        throw new RuntimeException("Error retrieving topics", e);
    	    }
        }
    	
    	public void clear() {
    	    try {
    	        int size = map.size();
    	        map.clear();
    	        LOGGER.info("Cleared " + size + " topics");
    	    } catch (Exception e) {
    	        LOGGER.log(Level.SEVERE, "Error clearing topics", e);
    	        throw new RuntimeException("Error clearing topics", e);
    	    }
    	}
    	
    	public int getTopicCount() {
    	    return map.size();
    	}
    	
    	public boolean hasTopic(String name) {
    	    if (name == null || name.trim().isEmpty()) {
    	        return false;
    	    }
    	    return map.containsKey(name);
    	}
    	
    	public void removeTopic(String name) {
    	    if (name == null || name.trim().isEmpty()) {
    	        LOGGER.warning("Attempted to remove topic with null or empty name");
    	        return;
    	    }
    	    
    	    try {
    	        Topic removed = map.remove(name);
    	        if (removed != null) {
    	            LOGGER.info("Removed topic: " + name);
    	        } else {
    	            LOGGER.warning("Topic not found for removal: " + name);
    	        }
    	    } catch (Exception e) {
    	        LOGGER.log(Level.SEVERE, "Error removing topic: " + name, e);
    	        throw new RuntimeException("Error removing topic: " + name, e);
    	    }
    	}
    }

	public static TopicManager get(){ 
	    try {
	        TopicManager manager = TopicManager.instance;
	        if (manager == null) {
	            LOGGER.severe("TopicManager instance is null");
	            throw new RuntimeException("TopicManager instance is null");
	        }
	        return manager;
	    } catch (Exception e) {
	        LOGGER.log(Level.SEVERE, "Error getting TopicManager instance", e);
	        throw new RuntimeException("Error getting TopicManager instance", e);
	    }
	}
    
}
