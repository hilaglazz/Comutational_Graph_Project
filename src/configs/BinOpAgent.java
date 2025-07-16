package configs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * An agent that performs a binary operation (e.g., addition, multiplication) on two input values and publishes the result.
 */
public class BinOpAgent implements Agent {

	private final String name; // The name of the agent
	private final String[] subs; // The input topics (subscribers)
	private final String[] pubs; // The output topics (publishers)
	String in1Name, in2Name, outName; // The names of the input and output topics
	BinaryOperator<Double> binOp; // The binary operation to perform
	private final Map<String, Double> receivedValues = new HashMap<>();
	TopicManager topicManager; // The topic manager
	
	// Create a BinOpAgent.
	public BinOpAgent(String name, String[] subs, String[] pubs) {
		this.name = name;
		this.subs = subs;
		this.pubs = pubs;
		for (String sub : subs) {
			TopicManagerSingleton.get().getTopic(sub).subscribe(this);
		}
		for (String pub : pubs) {
			TopicManagerSingleton.get().getTopic(pub).addPublisher(this);
		}
	}

	// Called when a message is received on a subscribed topic.
	@Override
	public void callback(String topic, Message msg) {
	    // Try to parse the message as a double
        double value;
        try {
            value = Double.parseDouble(msg.asText.trim());
        } catch (NumberFormatException e) {
			System.out.println("Error: invalid message format. Expected a double.");
            return;
        }

        // Store the received value
        receivedValues.put(topic, value);

        // If both values are available, apply the binary operation
        if (receivedValues.containsKey(in1Name) && receivedValues.containsKey(in2Name)) {
            double val1 = receivedValues.get(in1Name);
            double val2 = receivedValues.get(in2Name);
            double result = binOp.apply(val1, val2);

            // Publish the result as a message
            Message resultMessage = new Message(result);
            this.topicManager.getTopic(outName).publish(resultMessage);

            // Clear stored values after processing
            receivedValues.clear();
        }
	}
	
	// Get the name of the agent
	@Override
	public String getName() {
		return name;
	}

	// Reset the value of the agent
	@Override
	public void reset() {
		receivedValues.put(in1Name, 0.0);
    	receivedValues.put(in2Name, 0.0);
	}

	// Close the agent
	@Override
	public void close() {
		for (String sub : subs) {
			TopicManagerSingleton.get().getTopic(sub).unsubscribe(this);
		}
	}	
    
	// Get the output topics
	public String[] getPubs() { return pubs; }
	// Get the input topics
	public String[] getSubs() { return subs; }
}
