package graph;

/**
 * The Agent interface defines the contract for computational agents in the graph system.
 * Agents are processing units that can subscribe to topics, receive messages, and perform
 * computations based on incoming data.
 * 
 * <p>Agents are designed to work in a publish-subscribe messaging pattern where they can:
 * <ul>
 *   <li>Subscribe to topics to receive messages</li>
 *   <li>Process incoming messages through callbacks</li>
 *   <li>Publish results to other topics</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Agent myAgent = new MyCustomAgent();
 * Topic inputTopic = topicManager.getTopic("input");
 * inputTopic.subscribe(myAgent);
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public interface Agent {
    
    /**
     * Returns the unique name identifier for this agent.
     * 
     * @return the agent's name, never null
     */
    String getName();
    
    /**
     * Resets the agent's internal state to its initial values.
     * This method should clear any cached data, reset counters,
     * and prepare the agent for a fresh computation cycle.
     */
    void reset();
    
    /**
     * Callback method invoked when a message is published to a subscribed topic.
     * This is where the agent's main processing logic should be implemented.
     * 
     * @param topic the name of the topic that published the message, not null
     * @param msg the message containing the data to process, not null
     */
    void callback(String topic, Message msg);
    
    /**
     * Closes the agent and releases any resources it may be holding.
     * This method should unsubscribe from all topics and clean up
     * any connections or file handles.
     */
    void close();
}