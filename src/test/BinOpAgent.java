package test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import test.TopicManagerSingleton.TopicManager;

public class BinOpAgent implements Agent {

	String agentName, in1Name, in2Name, outName;
	BinaryOperator<Double> binOp;
	private final Map<String, Double> receivedValues = new HashMap<>();
	TopicManager topicManager;
	
	public BinOpAgent(String agent, String in1, String in2, String out, BinaryOperator<Double> binOp) {
		this.agentName = agent;
		this.in1Name = in1;
		this.in2Name = in2;
		this.outName = out;
		this.binOp = binOp;
		
		this.topicManager =  TopicManagerSingleton.get();
		//subscribe this agent instance to the input topics
		this.topicManager.getTopic(this.in1Name).subscribe(this);
		this.topicManager.getTopic(this.in2Name).subscribe(this);
		this.topicManager.getTopic(this.outName).addPublisher(this);
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
		return this.agentName;
	}

	@Override
	public void reset() {
		receivedValues.put(in1Name, 0.0);
    	receivedValues.put(in2Name, 0.0);
	}

	@Override
	public void close() {
		this.close();
	}	
    
}
