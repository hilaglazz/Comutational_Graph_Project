package test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ParallelAgent implements Agent{
    Agent agent;
    BlockingQueue<Message> queue;
    boolean stop = false;
    Thread t;
    
    
    public ParallelAgent(Agent agent, int capacity) {
    	this.agent = agent;
    	this.queue = new ArrayBlockingQueue<Message>(capacity);
    	t = new Thread(()->{ 
    		String[] parts;
    		while(!stop) {
					Message msg;
					try {
						msg = this.queue.take();
			    		parts = msg.asText.split(",", 2);
			            if (parts.length < 2) {
			                throw new IllegalArgumentException("Invalid message format: Topic and message not found.");
			            }
			            
			            String topic = parts[0].trim();
			            Message msg1 = new Message(parts[1].trim());
			            this.agent.callback(topic, msg1);
					} catch (InterruptedException e) {
					}
    		}
    	});
    	t.start(); 
    }
    
    @Override
    public void callback(String topic, Message msg) {
    	String combinedText = topic + "," + msg.asText;
        Message newMessage = new Message(combinedText);
        try {
			queue.put(newMessage);
		} catch (InterruptedException e) {
		}
    }

	@Override
	public String getName() {
		return this.agent.getName();
	}

	@Override
	public void reset() {
		this.agent.reset();
	}

	@Override
	public void close() {
		stop = true;
		t.interrupt();
		this.agent.close();
	}
}
