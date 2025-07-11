package test;

import java.util.ArrayList;

public class Topic {
    public final String name;
    public ArrayList<Agent> subs = new ArrayList<>();
    public ArrayList<Agent> pubs = new ArrayList<>();
    
    private Message latestMessage = null;
    
    Topic(String name){
        this.name=name;
    }

    public void subscribe(Agent a){
    	this.subs.add(a);
    }
    public void unsubscribe(Agent a){
    	this.subs.remove(a);
    }

    public void publish(Message m){
    	this.latestMessage = m; // Update latest message
    	for (Agent a: subs)
    		a.callback(this.name, m);
    }

    public void addPublisher(Agent a){
    	this.pubs.add(a);
    }

    public void removePublisher(Agent a){
    	this.pubs.remove(a);
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    //public void setLatestMessage(Message m) {
    //    this.latestMessage = m;
    //}
}