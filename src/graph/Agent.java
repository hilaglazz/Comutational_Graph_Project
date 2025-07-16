package graph;

/**
 * Interface for agents in the computational graph system.
 * Implementations provide methods for managing agent state and behavior.
 */
public interface Agent {
    // Get the name of the agent
    String getName();
    // Reset the agent's state
    void reset();
    // Called when a message is received on a subscribed topic
    void callback(String topic, Message msg);
    // Close the agent
    void close();
}
