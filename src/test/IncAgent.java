package test;

import java.util.ArrayList;

import test.TopicManagerSingleton.TopicManager;

public class IncAgent implements Agent {
    ArrayList<String> subs;
    ArrayList<String> pubs;
    TopicManager topicManager;
    double x = 0;
    String name = "inc";

    public IncAgent(ArrayList<String> subs, ArrayList<String> pubs) {
        this.subs = subs;
        this.pubs = pubs;
        this.topicManager = TopicManagerSingleton.get();
        this.topicManager.getTopic(subs.get(0)).subscribe(this);
        this.topicManager.getTopic(pubs.get(0)).addPublisher(this);
    }

    @Override
    public void callback(String topic, Message msg) {
        // Try to parse the message as a double
        try {
            x = Double.parseDouble(msg.asText.trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: invalid message format. Expected a double.");
            return; // Ignore non-double messages
        }

        // Increment the value and publish it
        double incValue = x + 1;
        Message resultMessage = new Message(incValue);
        this.topicManager.getTopic(pubs.get(0)).publish(resultMessage);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void close() {   ////////////######## check what really needs to be done here
        this.topicManager.getTopic(this.subs.get(0)).unsubscribe(this);
        this.topicManager.getTopic(this.pubs.get(0)).removePublisher(this);
        // Clear lists
        subs.clear();
        pubs.clear();
    }

    @Override
    public void reset() {   ////////////######## check what really needs to be done here
        x = 0;
    }

    
}
