package configs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

public class BinOpAgent implements Agent {

	private final String name;
	private final String[] subs;
	private final String[] pubs;
	String in1Name, in2Name, outName;
	BinaryOperator<Double> binOp;
	private final Map<String, Double> receivedValues = new HashMap<>();
	TopicManager topicManager;
	
	public BinOpAgent(String name, String[] pubs, String[] subs) {
		this.name = name;
		this.pubs = pubs;
		this.subs = subs;
		for (String sub : subs) {
			TopicManagerSingleton.get().getTopic(sub).subscribe(this);
		}
		for (String pub : pubs) {
			TopicManagerSingleton.get().getTopic(pub).addPublisher(this);
		}
	}

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
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void reset() {
		receivedValues.put(in1Name, 0.0);
    	receivedValues.put(in2Name, 0.0);
	}

	@Override
	public void close() {
		for (String sub : subs) {
			TopicManagerSingleton.get().getTopic(sub).unsubscribe(this);
		}
	}	
    
	public String[] getPubs() { return pubs; }
	public String[] getSubs() { return subs; }
}
