package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

/**
 * An agent that multiplies two input values and publishes the result.
 */
public class MulAgent implements Agent {
    private double x = 0; // The first input value
    private double y = 0; // The second input value
    private final String name; // The name of the agent
    private final String[] subs; // The input topics (subscribers)
    private final String[] pubs; // The output topics (publishers)

    // Create a MulAgent.
    public MulAgent(String name, String[] subs, String[] pubs) {
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

    // Get the name of the agent
    @Override
    public String getName() {
        return name;
    }

    // Reset the value of the agent
    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

    // Called when a message is received on a subscribed topic.
    @Override
    public void callback(String topic, Message msg) {
        if (subs[0].equals(topic)) {
            x = msg.asDouble;
        } else if (subs.length > 1 && subs[1].equals(topic)) {
            y = msg.asDouble;
        }
        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            double result = x * y;
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
    }

    // Close the agent
    @Override
    public void close() {
        // Unsubscribe the agent from all input topics
        for (String sub : subs) {
            TopicManagerSingleton.get().getTopic(sub).unsubscribe(this);
        }
    }

    // Get the output topics
    public String[] getPubs() { return pubs; }
    // Get the input topics
    public String[] getSubs() { return subs; }
}
    
    
    