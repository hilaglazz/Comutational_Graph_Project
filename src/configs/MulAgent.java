package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;



public class MulAgent implements Agent {
    private double x = 0;
    private double y = 0;
    private final String name;
    private final String[] subs;
    private final String[] pubs;

    public MulAgent(String name, String[] pubs, String[] subs) {
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
        x = 0;
        y = 0;
    }

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

    @Override
    public void close() {
        for (String sub : subs) {
            TopicManagerSingleton.get().getTopic(sub).unsubscribe(this);
        }
    }

    public String[] getPubs() { return pubs; }
    public String[] getSubs() { return subs; }
}
    
    
    