package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;


public class IncAgent implements Agent {
    private double value = 0;
    private final String name;
    private final String[] subs;
    private final String[] pubs;

    public IncAgent(String name, String[] pubs, String[] subs) {
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
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        value = 0;
    }

    @Override
    public void callback(String topic, Message msg) {
        value = msg.asDouble;
        if (!Double.isNaN(value)) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(value + 1));
        }
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
