package configs;

import java.util.function.BinaryOperator;

import graph.Agent;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * A binary operation agent that performs mathematical operations on two input topics
 * and publishes the result to an output topic.
 * 
 * <p>This agent subscribes to two input topics, waits for numeric messages on both,
 * applies a binary operation (such as addition, subtraction, multiplication, etc.)
 * to the values, and publishes the result to an output topic.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create an addition agent that adds values from topics "A" and "B" 
 * // and publishes result to topic "Sum"
 * BinOpAgent addAgent = new BinOpAgent(
 *     "AdditionAgent", 
 *     "A", 
 *     "B", 
 *     "Sum", 
 *     (x, y) -> x + y
 * );
 * 
 * // Create a multiplication agent
 * BinOpAgent mulAgent = new BinOpAgent(
 *     "MultiplyAgent", 
 *     "Input1", 
 *     "Input2", 
 *     "Product", 
 *     (x, y) -> x * y
 * );
 * }</pre>
 * 
 * <p><strong>Behavior:</strong></p>
 * <ul>
 *   <li>The agent waits for numeric messages on both input topics</li>
 *   <li>When both values are received, it applies the binary operation</li>
 *   <li>The result is published to the output topic</li>
 *   <li>Input flags are reset after each computation</li>
 *   <li>Non-numeric messages are ignored</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe when used with 
 * the ParallelAgent wrapper, which serializes callback executions.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Agent
 * @see BinaryOperator
 * @see TopicManagerSingleton
 */
public class BinOpAgent implements Agent {
    
    /** The unique name of this agent instance */
    private String name;
    
    /** The name of the first input topic */
    private String inputTopicName1;
    
    /** The name of the second input topic */
    private String inputTopicName2;
    
    /** The name of the output topic where results are published */
    private String outputTopicName;
    
    /** The binary operation to perform on the two input values */
    private BinaryOperator<Double> operation;
    
    /** Reference to the singleton topic manager */
    private TopicManager tm;
    
    /** The value from the first input topic */
    private Double input1;
    
    /** The value from the second input topic */
    private Double input2;
    
    /** Flag indicating whether a value has been received from the first input topic */
    private boolean inFound1 = false;
    
    /** Flag indicating whether a value has been received from the second input topic */
    private boolean inFound2 = false;

    /**
     * Constructs a new BinOpAgent that performs binary operations on two input topics.
     * 
     * <p>The agent automatically subscribes to the specified input topics and 
     * registers as a publisher for the output topic. When numeric messages are 
     * received on both input topics, the specified operation is applied and the 
     * result is published to the output topic.</p>
     * 
     * @param agentName the unique name for this agent instance
     * @param inputTopicName1 the name of the first input topic to subscribe to
     * @param inputTopicName2 the name of the second input topic to subscribe to  
     * @param outputTopicName the name of the output topic to publish results to
     * @param operation the binary operation to apply to the input values.
     *                 Common operations include:
     *                 <ul>
     *                   <li>{@code (x, y) -> x + y} for addition</li>
     *                   <li>{@code (x, y) -> x - y} for subtraction</li>
     *                   <li>{@code (x, y) -> x * y} for multiplication</li>
     *                   <li>{@code (x, y) -> x / y} for division</li>
     *                   <li>{@code Math::pow} for exponentiation</li>
     *                 </ul>
     * 
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if agentName is empty or whitespace-only
     * 
     * @see BinaryOperator
     * @see TopicManagerSingleton#get()
     */
    public BinOpAgent(String agentName, String inputTopicName1, String inputTopicName2, 
                     String outputTopicName, BinaryOperator<Double> operation) {
        if (agentName == null || agentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be null or empty");
        }
        if (inputTopicName1 == null) {
            throw new NullPointerException("First input topic name cannot be null");
        }
        if (inputTopicName2 == null) {
            throw new NullPointerException("Second input topic name cannot be null");
        }
        if (outputTopicName == null) {
            throw new NullPointerException("Output topic name cannot be null");
        }
        if (operation == null) {
            throw new NullPointerException("Binary operation cannot be null");
        }
        
        this.name = agentName;
        this.inputTopicName1 = inputTopicName1;
        this.inputTopicName2 = inputTopicName2;
        this.outputTopicName = outputTopicName;
        this.operation = operation;
        this.tm = TopicManagerSingleton.get();
        
        reset();
        
        // Subscribe to input topics
        tm.getTopic(inputTopicName1).subscribe(this);
        tm.getTopic(inputTopicName2).subscribe(this);
        
        // Register as publisher for output topic
        tm.getTopic(outputTopicName).addPublisher(this);
    }

    /**
     * Returns the unique name of this agent.
     * 
     * @return the agent's name as specified in the constructor
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Resets the agent's internal state.
     * 
     * <p>This method clears all stored input values and resets the input flags
     * to their initial state. The agent will wait for new messages on both
     * input topics before performing the next computation.</p>
     * 
     * <p><strong>Note:</strong> This method does not unsubscribe from topics
     * or change the agent's configuration.</p>
     */
    @Override
    public void reset() {
        this.input1 = 0.0;
        this.input2 = 0.0;
        this.inFound1 = false;
        this.inFound2 = false;
    }

    /**
     * Handles incoming messages from subscribed topics.
     * 
     * <p>This method is called automatically when a message is published to 
     * one of the agent's input topics. The agent processes numeric messages
     * and ignores non-numeric ones.</p>
     * 
     * <p><strong>Processing Logic:</strong></p>
     * <ol>
     *   <li>Check if the message contains a valid numeric value</li>
     *   <li>Store the value based on which topic it came from</li>
     *   <li>If both input values are available, apply the binary operation</li>
     *   <li>Publish the result to the output topic</li>
     *   <li>Reset input flags for the next computation</li>
     * </ol>
     * 
     * @param topic the name of the topic that published the message
     * @param msg the message containing the data. Only messages with valid
     *           numeric values (where {@code !Double.isNaN(msg.asDouble)}) 
     *           are processed
     * 
     * @throws RuntimeException if an error occurs during operation computation
     *                         or result publishing
     * 
     * @see Message#asDouble
     * @see Topic#publish(Message)
     */
    @Override
    public void callback(String topic, Message msg) {
        if (!Double.isNaN(msg.asDouble)) {
            if (topic.equals(inputTopicName1)) {
                input1 = msg.asDouble;
                inFound1 = true;
            } else if (topic.equals(inputTopicName2)) {
                input2 = msg.asDouble;
                inFound2 = true;
            }
            
            // Perform computation when both inputs are available
            if (input1 != null && input2 != null && inFound1 && inFound2) {
                try {
                    Double result = operation.apply(input1, input2);
                    Message outputMsg = new Message(result);
                    Topic outputTopic = tm.getTopic(outputTopicName);
                    outputTopic.publish(outputMsg);
                    
                    // Reset flags for next computation
                    inFound1 = false;
                    inFound2 = false;
                } catch (Exception e) {
                    // Reset flags even if operation fails
                    inFound1 = false;
                    inFound2 = false;
                    throw new RuntimeException("Error computing binary operation: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Cleanly shuts down the agent and releases all resources.
     * 
     * <p>This method performs the following cleanup operations:</p>
     * <ul>
     *   <li>Unsubscribes from both input topics</li>
     *   <li>Removes itself as a publisher from the output topic</li>
     *   <li>Clears internal state</li>
     * </ul>
     * 
     * <p><strong>Important:</strong> After calling this method, the agent
     * should not be used anymore. Any subsequent calls to other methods
     * may result in undefined behavior.</p>
     * 
     * @see Topic#unsubscribe(Agent)
     * @see Topic#removePublisher(Agent)
     */
    @Override
    public void close() {
        // Unsubscribe from input topics
        tm.getTopic(inputTopicName1).unsubscribe(this);
        tm.getTopic(inputTopicName2).unsubscribe(this);
        
        // Remove as publisher from output topic
        tm.getTopic(outputTopicName).removePublisher(this);
        
        // Clear internal state
        reset();
    }
}