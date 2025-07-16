package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for all topics in the system.
 * Provides case-insensitive lookup, creation, and removal of topics.
 */
public class TopicManagerSingleton {
    private static final int MAX_TOPIC_NAME_LENGTH = 100;
    private static final int MAX_TOPICS = 1000;
    
   
    // TopicManager is a singleton class that manages the lifecycle and lookup of Topic objects.
    public static class TopicManager{
        private static final TopicManager instance = new TopicManager();
        // Maps topic names (uppercase) to Topic objects
        private final ConcurrentHashMap<String, Topic> map;
        
        private TopicManager() {
            map = new ConcurrentHashMap<>();
        }
        
        // Get or create a topic by name (case-insensitive).
        public Topic getTopic(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Topic name cannot be null");
            }
            name = name.toUpperCase();
            if (name.trim().isEmpty()) { // throw an exception if the topic name is empty
                throw new IllegalArgumentException("Topic name cannot be empty"); 
            }
            if (name.length() > MAX_TOPIC_NAME_LENGTH) { // throw an exception if the topic name is too long
                throw new IllegalArgumentException("Topic name too long (max " + MAX_TOPIC_NAME_LENGTH + " characters)"); 
            }
            if (map.size() >= MAX_TOPICS) { // throw an exception if the maximum number of topics is reached
                throw new IllegalStateException("Maximum number of topics reached: " + MAX_TOPICS); 
            }
            try {
                Topic topic = map.computeIfAbsent(name, Topic::new);
                if (topic == null) { // throw an exception if the topic is not created
                    throw new RuntimeException("Failed to create topic: " + name);
                }
                return topic;
            } catch (Exception e) {
                throw new RuntimeException("Error getting/creating topic: " + name, e);
            }
        }
        
        // Get all topics currently managed
        public Collection<Topic> getTopics() {
            return map.values(); // return all topics currently managed
        }
      
        // Remove all topics from the manager.
        public void clear() {
            map.clear();
        }
       
        // Get the number of topics currently managed
        public int getTopicCount() {
            return map.size(); // return the number of topics currently managed
        }
        
        // Check if a topic exists (case-insensitive)
        public boolean hasTopic(String name) {
            if (name == null || name.trim().isEmpty()) {
                return false; // return false if the topic name is null or empty
            }
            name = name.toUpperCase();
            return map.containsKey(name); // return true if the topic exists
        }
      
        // Remove a topic by name (case-insensitive)
        public void removeTopic(String name) {
            if (name == null || name.trim().isEmpty()) {
                return; // return if the topic name is null or empty
            }
            name = name.toUpperCase();
            map.remove(name);
        }
    }
    
    // Get the singleton TopicManager instance
    public static TopicManager get(){ 
        return TopicManager.instance; // return the singleton TopicManager instance
    }
}
