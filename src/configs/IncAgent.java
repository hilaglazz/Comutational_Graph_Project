package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;

/**
 * An agent that increments its input value by 1 and publishes the result.
 */
public class IncAgent implements Agent {
    private double value = 0; // The value of the agent
    private final String name; // The name of the agent
    private final String[] subs; // The input topics (subscribers)
    private final String[] pubs; // The output topics (publishers)

    // Create an IncAgent.
    public IncAgent(String name, String[] subs, String[] pubs) {
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
        value = 0;
    }

    // Called when a message is received on a subscribed topic.
    @Override
    public void callback(String topic, Message msg) {
        value = msg.asDouble;
        if (!Double.isNaN(value)) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(value + 1));
        }
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
