package test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class TopicManagerSingleton {
    
	public static class TopicManager{
		private static final TopicManager instance = new TopicManager();
		private ConcurrentHashMap<String, Topic> map;
    	private TopicManager() {
    		map = new ConcurrentHashMap<>();
    	}
    	
    	public Topic getTopic(String name) {
    		 return map.computeIfAbsent(name, Topic::new);
    	}
    	
    	public Collection<Topic> getTopics() {
            return map.values();
        }
    	
    	public void clear() {
    		map.clear();
    	}
    }

	public static TopicManager get(){ 
	       return TopicManager.instance; 
	}
    
}
