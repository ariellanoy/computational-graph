package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * An increment agent that adds 1 to incoming numeric values and publishes the result.
 * 
 * <p>The IncAgent is a simple computational agent that performs increment operations
 * on numeric data flowing through the computational graph. It subscribes to an input
 * topic, receives numeric messages, adds 1 to each value, and publishes the incremented
 * result to an output topic.</p>
 * 
 * <p><strong>Mathematical Operation:</strong></p>
 * <pre>
 * output = input + 1
 * </pre>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Create an increment agent that reads from "Counter" and writes to "NextValue"
 * String[] inputs = {"Counter"};
 * String[] outputs = {"NextValue"};
 * IncAgent incrementer = new IncAgent(inputs, outputs);
 * 
 * // The agent will automatically:
 * // 1. Subscribe to the "Counter" topic
 * // 2. Process any numeric messages received
 * // 3. Add 1 to the value
 * // 4. Publish the result to "NextValue" topic
 * 
 * // Example data flow:
 * // Counter topic receives: 5.0 → IncAgent processes → NextValue topic gets: 6.0
 * // Counter topic receives: -2.5 → IncAgent processes → NextValue topic gets: -1.5
 * }</pre>
 * 
 * <p><strong>Configuration File Usage:</strong></p>
 * <p>In a configuration file, the IncAgent can be defined as:</p>
 * <pre>
 * configs.IncAgent
 * InputTopic
 * OutputTopic
 * </pre>
 * 
 * <p><strong>Common Use Cases:</strong></p>
 * <ul>
 *   <li><strong>Counters:</strong> Implementing step counters or iteration counters</li>
 *   <li><strong>Offset Operations:</strong> Adding a constant offset to data streams</li>
 *   <li><strong>Index Adjustment:</strong> Converting 0-based to 1-based indexing</li>
 *   <li><strong>Signal Processing:</strong> Adding DC bias to signals</li>
 *   <li><strong>Sequence Generation:</strong> Part of arithmetic sequence generators</li>
 * </ul>
 * 
 * <p><strong>Computational Graph Integration:</strong></p>
 * <pre>{@code
 * // Example: Creating a sequence generator
 * // Initial → IncAgent → IncAgent → IncAgent → Final
 * //   0    →    1    →    2    →    3    →   3
 * 
 * // Configuration:
 * // Agent 1: reads "Start", writes "Step1" 
 * // Agent 2: reads "Step1", writes "Step2"
 * // Agent 3: reads "Step2", writes "Final"
 * }</pre>
 * 
 * <p><strong>Behavior Characteristics:</strong></p>
 * <ul>
 *   <li><strong>Stateless:</strong> Each message is processed independently</li>
 *   <li><strong>Immediate Processing:</strong> Output is generated immediately upon input</li>
 *   <li><strong>Non-blocking:</strong> Does not wait for multiple inputs</li>
 *   <li><strong>Numeric Only:</strong> Only processes messages with valid numeric values</li>
 *   <li><strong>Error Tolerant:</strong> Ignores non-numeric messages silently</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe when used with the
 * {@link graph.ParallelAgent} wrapper, which serializes all callback executions.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Agent
 * @see Message
 * @see TopicManagerSingleton
 */
public class IncAgent implements Agent {
    
    /** Static counter for generating unique agent names */
    private static int instanceCounter = 0;
    
    /** The unique name of this agent instance */
    private String name;
    
    /** The name of the input topic this agent subscribes to */
    private String inputTopicName;
    
    /** The name of the output topic this agent publishes to */
    private String outputTopicName;
    
    /** Reference to the singleton topic manager */
    private TopicManager tm;
    
    /** Array of subscription topic names from constructor */
    private final String[] subs;
    
    /** Array of publication topic names from constructor */
    private final String[] pubs;
    
    /**
     * Constructs a new IncAgent with the specified input and output topics.
     * 
     * <p>This constructor follows the standard agent pattern required by the
     * {@link GenericConfig} system. It accepts arrays of subscription and
     * publication topic names, though IncAgent only uses the first element
     * of each array.</p>
     * 
     * <p><strong>Constructor Behavior:</strong></p>
     * <ol>
     *   <li>Generates a unique name using static counter</li>
     *   <li>Stores topic arrays for later cleanup</li>
     *   <li>Extracts first subscription topic (if any)</li>
     *   <li>Extracts first publication topic (if any)</li>
     *   <li>Subscribes to input topic (if specified)</li>
     *   <li>Registers as publisher for output topic (if specified)</li>
     * </ol>
     * 
     * <p><strong>Topic Array Handling:</strong></p>
     * <ul>
     *   <li><strong>subs[0]:</strong> Input topic to subscribe to (optional)</li>
     *   <li><strong>pubs[0]:</strong> Output topic to publish to (optional)</li>
     *   <li><strong>Additional elements:</strong> Ignored but stored for completeness</li>
     * </ul>
     * 
     * <p><strong>Naming Convention:</strong></p>
     * <p>Each IncAgent instance gets a unique name in the format "IncAgent_N"
     * where N is an incrementing integer starting from 1.</p>
     * 
     * @param subs array of subscription topic names. If not empty, the first
     *            element specifies the input topic to subscribe to. Additional
     *            elements are ignored but stored for reference.
     * @param pubs array of publication topic names. If not empty, the first
     *            element specifies the output topic to publish to. Additional
     *            elements are ignored but stored for reference.
     * 
     * @throws NullPointerException if subs or pubs arrays are null
     * 
     * @example
     * <pre>{@code
     * // Standard usage with one input and one output
     * String[] inputs = {"NumberStream"};
     * String[] outputs = {"IncrementedStream"};
     * IncAgent agent = new IncAgent(inputs, outputs);
     * 
     * // Input-only agent (processes but doesn't publish)
     * String[] inputsOnly = {"DataIn"};
     * String[] noOutputs = {};
     * IncAgent processor = new IncAgent(inputsOnly, noOutputs);
     * 
     * // Output-only agent (publishes but doesn't subscribe)
     * String[] noInputs = {};
     * String[] outputsOnly = {"DataOut"};
     * IncAgent generator = new IncAgent(noInputs, outputsOnly);
     * }</pre>
     * 
     * @see TopicManagerSingleton#get()
     * @see Topic#subscribe(Agent)
     * @see Topic#addPublisher(Agent)
     */
    public IncAgent(String[] subs, String[] pubs) {
        if (subs == null) {
            throw new NullPointerException("Subscription topics array cannot be null");
        }
        if (pubs == null) {
            throw new NullPointerException("Publication topics array cannot be null");
        }
        
        this.subs = subs;
        this.pubs = pubs;
        
        // Generate unique name with thread-safe increment
        synchronized (IncAgent.class) {
            instanceCounter++;
            this.name = "IncAgent_" + instanceCounter;
        }
        
        this.tm = TopicManagerSingleton.get();
        
        // Set up input subscription if specified
        if (subs.length > 0) {
            this.inputTopicName = subs[0];
            tm.getTopic(inputTopicName).subscribe(this);
        }
        
        // Set up output publication if specified
        if (pubs.length > 0) {
            this.outputTopicName = pubs[0];
            tm.getTopic(outputTopicName).addPublisher(this);
        }
    }
    
    /**
     * Returns the unique name of this agent.
     * 
     * <p>The name follows the pattern "IncAgent_N" where N is a unique
     * integer assigned when the agent is created.</p>
     * 
     * @return the agent's unique name, never null
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Resets the agent's internal state.
     * 
     * <p>Since IncAgent is stateless (each message is processed independently),
     * this method performs no operations. It's included to fulfill the
     * {@link Agent} interface contract.</p>
     * 
     * <p><strong>Implementation Note:</strong> IncAgent doesn't maintain any
     * internal state between message processing calls, so there's nothing
     * to reset. The method is provided for interface compliance and potential
     * future extensions.</p>
     * 
     * @see Agent#reset()
     */
    @Override
    public void reset() {
        // IncAgent is stateless - no internal state to reset
        // This method is implemented for Agent interface compliance
    }

    /**
     * Processes incoming messages by adding 1 to numeric values.
     * 
     * <p>This method is called automatically by the topic system when a message
     * is published to any topic this agent subscribes to. The agent processes
     * only numeric messages and ignores non-numeric ones.</p>
     * 
     * <p><strong>Processing Algorithm:</strong></p>
     * <ol>
     *   <li>Check if the message contains a valid numeric value</li>
     *   <li>If numeric: add 1 to the value</li>
     *   <li>Create a new message with the incremented value</li>
     *   <li>Publish the result to the output topic (if configured)</li>
     *   <li>If non-numeric: ignore the message silently</li>
     * </ol>
     * 
     * <p><strong>Numeric Value Detection:</strong></p>
     * <p>The agent uses {@link Message#asDouble} to extract numeric values.
     * Messages are considered numeric if {@code !Double.isNaN(msg.asDouble)}
     * returns true.</p>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Non-numeric messages are ignored (no error thrown)</li>
     *   <li>Special numeric values (infinity, very large numbers) are processed normally</li>
     *   <li>If no output topic is configured, processing occurs but no publication happens</li>
     * </ul>
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * // Numeric message processing:
     * Message input1 = new Message("5.0");     // → output: 6.0
     * Message input2 = new Message("0");       // → output: 1.0  
     * Message input3 = new Message("-10.5");   // → output: -9.5
     * Message input4 = new Message("1.7E10");  // → output: 1.7E10 + 1
     * 
     * // Non-numeric messages (ignored):
     * Message input5 = new Message("hello");   // → no output
     * Message input6 = new Message("");        // → no output
     * }</pre>
     * 
     * @param topic the name of the topic that published the message.
     *             For IncAgent, this should match the configured input topic name.
     * @param msg the message containing the data to process. The agent extracts
     *           the numeric value using {@link Message#asDouble}.
     * 
     * @throws RuntimeException if an error occurs during message publishing
     *                         (e.g., topic system failure)
     * 
     * @see Message#asDouble
     * @see Double#isNaN(double)
     * @see Topic#publish(Message)
     */
    @Override
    public void callback(String topic, Message msg) {
        // Only process messages with valid numeric values
        if (!Double.isNaN(msg.asDouble)) {
            // Perform increment operation
            double output = msg.asDouble + 1.0;
            
            // Publish result if output topic is configured
            if (outputTopicName != null) {
                try {
                    Message outputMessage = new Message(output);
                    tm.getTopic(outputTopicName).publish(outputMessage);
                } catch (Exception e) {
                    throw new RuntimeException("Error publishing incremented value to topic '" 
                                             + outputTopicName + "': " + e.getMessage(), e);
                }
            }
        }
        // Non-numeric messages are silently ignored
    }

    /**
     * Cleanly shuts down the agent and releases all resources.
     * 
     * <p>This method performs the following cleanup operations:</p>
     * <ul>
     *   <li>Unsubscribes from the input topic (if configured)</li>
     *   <li>Removes itself as a publisher from the output topic (if configured)</li>
     *   <li>Clears any references to topics</li>
     * </ul>
     * 
     * <p><strong>Resource Management:</strong></p>
     * <p>Proper cleanup is essential to prevent memory leaks and ensure that
     * the topic system doesn't hold references to closed agents. This method
     * should always be called when the agent is no longer needed.</p>
     * 
     * <p><strong>Usage in Configuration Systems:</strong></p>
     * <p>When used with {@link GenericConfig}, this method is automatically
     * called during configuration shutdown, ensuring proper cleanup of all
     * created agents.</p>
     * 
     * <p><strong>Important:</strong> After calling this method, the agent
     * should not be used anymore. Any subsequent message processing or
     * method calls may result in undefined behavior.</p>
     * 
     * @see Topic#unsubscribe(Agent)
     * @see Topic#removePublisher(Agent)
     * @see GenericConfig#close()
     */
    @Override
    public void close() {
        // Unsubscribe from input topic if configured
        if (subs.length > 0 && inputTopicName != null) {
            try {
                tm.getTopic(inputTopicName).unsubscribe(this);
            } catch (Exception e) {
                System.err.println("Warning: Error unsubscribing IncAgent " + name 
                                 + " from topic " + inputTopicName + ": " + e.getMessage());
            }
        }
        
        // Remove as publisher from output topic if configured
        if (pubs.length > 0 && outputTopicName != null) {
            try {
                tm.getTopic(outputTopicName).removePublisher(this);
            } catch (Exception e) {
                System.err.println("Warning: Error removing IncAgent " + name 
                                 + " as publisher from topic " + outputTopicName + ": " + e.getMessage());
            }
        }
        
        // Clear topic references
        inputTopicName = null;
        outputTopicName = null;
    }
}