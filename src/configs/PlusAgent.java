package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * An agent that performs addition operations on two numeric inputs.
 * PlusAgent subscribes to two input topics, waits for numeric messages on both,
 * and publishes the sum to an output topic when both values are available.
 * 
 * <p>The agent maintains internal state to track when both inputs have been received.
 * Once both inputs are available, it calculates their sum and publishes the result.
 * The agent then resets its state to wait for the next pair of inputs.
 * 
 * <p>Example usage:
 * <pre>
 * // Create agent that adds values from topics "A" and "B", outputs to "result"
 * String[] inputs = {"A", "B"};
 * String[] outputs = {"result"};
 * PlusAgent adder = new PlusAgent(inputs, outputs);
 * 
 * // Send messages to input topics
 * TopicManager tm = TopicManagerSingleton.get();
 * tm.getTopic("A").publish(new Message("5.0"));
 * tm.getTopic("B").publish(new Message("3.0"));
 * // Agent automatically publishes "8.0" to "result" topic
 * </pre>
 * 
 * <p>Thread Safety: This class is thread-safe when used with the ParallelAgent wrapper.
 * Direct usage in multi-threaded environments may require external synchronization.
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class PlusAgent implements Agent{
    private static int instanceCounter = 0; // Static counter for unique names
	private String name;
	private String inputTopicName1;
	private String inputTopicName2;
	private String outputTopicName;
	private TopicManager tm;
	private Double x;
	private Double y;
    private boolean xFound = false;
    private boolean yFound = false;
    private final String[] subs;
   	private final String[] pubs;
	
    /**
     * Creates a new PlusAgent with the specified input and output topics.
     * The agent automatically subscribes to input topics and registers as a publisher
     * for output topics using the singleton TopicManager.
     * 
     * <p>Input/Output Configuration:
     * <ul>
     * <li>If subs.length > 0: subscribes to subs[0] as first input</li>
     * <li>If subs.length > 1 subscribes to subs[1] as second input</li>
     * <li>If pubs.length > 0: registers as publisher for pubs[0]</li>
     * </ul>
     * 
     * @param subs array of input topic names (should contain 1-2 elements)
     * @param pubs array of output topic names (should contain 1 element)
     * @throws NullPointerException if subs or pubs is null
     */
    public PlusAgent(String[] subs, String[] pubs) {
    	this.subs = subs;
		this.pubs = pubs;  
		instanceCounter++;
        this.name = "PlusAgent_" + instanceCounter;    	
        this.tm = TopicManagerSingleton.get();    	
    	reset();
    	
    	if (subs.length > 0) {
        	this.inputTopicName1 = subs[0];
        	tm.getTopic(inputTopicName1).subscribe(this);
    	}
    	if (subs.length > 1) {
        	this.inputTopicName2 = subs[1];    	
        	tm.getTopic(inputTopicName2).subscribe(this);
    	}
    	if (pubs.length > 0) {
        	this.outputTopicName = pubs[0];
        	tm.getTopic(outputTopicName).addPublisher(this);
    	}
    }
    
    /**
     * Returns the unique name of this agent instance.
     * 
     * @return the agent's unique name
     */
	@Override
	public String getName() {
		return name;
	}

	/**
     * Resets the agent's internal state to initial values.
     * Clears stored input values and resets the flags indicating which inputs have been received.
     * This prepares the agent to process a new pair of input values.
     */
	@Override
	public void reset() {
		this.x = 0.0;
		this.y = 0.0;
		this.xFound = false;
		this.yFound = false;
	}

	/**
     * Processes incoming messages from subscribed topics.
     * When a numeric message is received, it's stored as either the first or second input
     * based on the topic name. When both inputs are available, their sum is calculated
     * and published to the output topic.
     * 
     * <p>Message Processing Logic:
     * <ul>
     * <li>Only processes messages that can be parsed as numeric values</li>
     * <li>Stores values based on which input topic sent the message</li>
     * <li>Performs addition when both inputs are received</li>
     * <li>Publishes result and resets state for next calculation</li>
     * </ul>
     * 
     * @param topic the name of the topic that sent the message
     * @param msg the message containing the numeric data
     */
	@Override
	public void callback(String topic, Message msg) {
		if (!Double.isNaN(msg.asDouble)){
            if (subs.length > 0 && topic.equals(inputTopicName1)) {
                x = msg.asDouble;
                xFound = true;
            }
            else if (subs.length > 1 && topic.equals(inputTopicName2)) {
                y = msg.asDouble;
                yFound = true;
            }
            
            if (xFound && yFound && x != null && y != null) {
                double result = x + y;
                if (pubs.length > 0) {
                    tm.getTopic(outputTopicName).publish(new Message(result));
                }
            }
        }
	}

	/**
     * Closes the agent and releases all resources.
     * Unsubscribes from all input topics and removes itself from publisher lists.
     * This method should be called when the agent is no longer needed to prevent
     * memory leaks and ensure clean shutdown.
     */
	@Override
	public void close() {
		if (subs.length > 0 && inputTopicName1 != null) {
	        tm.getTopic(inputTopicName1).unsubscribe(this);
	    }
	    if (subs.length > 1 && inputTopicName2 != null) {
	        tm.getTopic(inputTopicName2).unsubscribe(this);
	    }
	    if (pubs.length > 0 && outputTopicName != null) {
	        tm.getTopic(outputTopicName).removePublisher(this);
	    }		
	}

    
}