package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import test.TopicManagerSingleton.TopicManager;

public class PlusAgent implements Agent {

    ArrayList<String> subs;
    ArrayList<String> pubs;
    TopicManager topicManager;
    double x = 0;
    double y = 0;
    String name = "plus";
    private final Map<String, Double> receivedValues = new HashMap<>();

    public PlusAgent(ArrayList<String> subs, ArrayList<String> pubs) {
        this.subs = subs;
        this.pubs = pubs;
        this.topicManager = TopicManagerSingleton.get();
        this.topicManager.getTopic(this.subs.get(0)).subscribe(this);
        this.topicManager.getTopic(this.subs.get(1)).subscribe(this);
        this.topicManager.getTopic(this.pubs.get(0)).addPublisher(this);
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
        if (receivedValues.containsKey(subs.get(0)) && receivedValues.containsKey(subs.get(1))) {
            x = receivedValues.get(subs.get(0));
            y = receivedValues.get(subs.get(1));
            double result = x + y;

            // Publish the result as a message
            Message resultMessage = new Message(result);
            this.topicManager.getTopic(pubs.get(0)).publish(resultMessage);

            // Clear stored values after processing
            receivedValues.clear();
        }
	}

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

    @Override
    public void close() {  ////////////######## check what really needs to be done here
        // Unsubscribe from topics if necessary
        for (String sub : subs) {
            topicManager.getTopic(sub).unsubscribe(this);
        }
        // Clear maps and lists
        receivedValues.clear();
        subs.clear();
        pubs.clear();
    }
	
}
