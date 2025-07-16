package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A parallel agent that runs in a separate thread and processes messages from a queue.
 */
public class ParallelAgent implements Agent{
    Agent agent; // The agent to run in parallel
    BlockingQueue<Message> queue; // The queue to store messages
    boolean stop = false; // Whether to stop the agent
    Thread t; // The thread to run the agent
    
    // Constructor
    public ParallelAgent(Agent agent, int capacity) {
    	this.agent = agent;
    	this.queue = new ArrayBlockingQueue<Message>(capacity);
    	t = new Thread(()->{ // Create a new thread to run the agent
    		String[] parts; // The parts of the message
    		while(!stop) { // While the agent is not stopped
					Message msg;
					try {
						msg = this.queue.take(); // Take a message from the queue
			    		parts = msg.asText.split(",", 2); // Split the message into parts
			            if (parts.length < 2) { // If the message is not valid
			                throw new IllegalArgumentException("Invalid message format: Topic and message not found.");
			            }
			            
			            String topic = parts[0].trim(); // Get the topic from the message
			            Message msg1 = new Message(parts[1].trim()); // Create a new message with the topic and message
			            this.agent.callback(topic, msg1); // Call the agent's callback method
					} catch (InterruptedException e) {
					}
    		}
    	});
    	t.start(); 
    }
    
    // Callback method
    @Override
    public void callback(String topic, Message msg) {
    	String combinedText = topic + "," + msg.asText;
        Message newMessage = new Message(combinedText);
        try {
			queue.put(newMessage);
		} catch (InterruptedException e) {
		}
    }

	// Get the name of the agent
	@Override
	public String getName() {
		return this.agent.getName();
	}

	// Reset the agent
	@Override
	public void reset() {
		this.agent.reset();
	}

	// Close the agent
	@Override
	public void close() {
		stop = true; // Set the stop flag to true
		t.interrupt(); // Interrupt the thread
		this.agent.close(); // Close the agent
	}
}
