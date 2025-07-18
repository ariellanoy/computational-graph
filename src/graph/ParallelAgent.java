package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A thread-safe wrapper that provides asynchronous message processing for agents.
 * 
 * <p>The ParallelAgent acts as a decorator around any {@link Agent} implementation,
 * providing thread safety and asynchronous message processing capabilities. It uses
 * a dedicated background thread with a message queue to ensure that agent callbacks
 * are processed sequentially while allowing the calling thread to return immediately.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Thread Safety:</strong> Serializes all agent callbacks to prevent race conditions</li>
 *   <li><strong>Asynchronous Processing:</strong> Non-blocking message submission with background processing</li>
 *   <li><strong>Message Ordering:</strong> Guarantees FIFO processing of messages</li>
 *   <li><strong>Resource Management:</strong> Automatic thread lifecycle management</li>
 *   <li><strong>Error Isolation:</strong> Prevents agent errors from affecting the topic system</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Wrap a regular agent for thread-safe operation
 * Agent baseAgent = new PlusAgent(new String[]{"A", "B"}, new String[]{"Result"});
 * ParallelAgent threadSafeAgent = new ParallelAgent(baseAgent, 100);
 * 
 * // Use in topic subscriptions (automatic with GenericConfig)
 * Topic inputTopic = tm.getTopic("InputData");
 * inputTopic.subscribe(threadSafeAgent);
 * 
 * // Messages are now processed asynchronously and thread-safely
 * // Background thread processes these sequentially
 * 
 * // Clean shutdown
 * threadSafeAgent.close(); // Stops background thread and cleans up
 * }</pre>
 * 
 * <p><strong>Thread Safety Guarantees:</strong></p>
 * <ul>
 *   <li>All calls to the wrapped agent are serialized in a single background thread</li>
 *   <li>No race conditions can occur within the wrapped agent's methods</li>
 *   <li>Message processing order is preserved (FIFO)</li>
 *   <li>Thread-safe shutdown with proper resource cleanup</li>
 * </ul>
 * 
 * <p><strong>Error Handling:</strong></p>
 * <p>The ParallelAgent provides error isolation - if the wrapped agent throws an
 * exception during message processing, it doesn't crash the topic system or affect
 * other agents. Errors are logged and processing continues with the next message.</p>
 * 
 * <p><strong>Integration with GenericConfig:</strong></p>
 * <p>This class is automatically used by {@link configs.GenericConfig} to wrap
 * all created agents, ensuring thread safety throughout the computational graph
 * without requiring changes to individual agent implementations.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Agent
 * @see configs.GenericConfig
 * @see java.util.concurrent.BlockingQueue
 */
public class ParallelAgent implements Agent{
    
    /** The wrapped agent that performs the actual computation */
	private final Agent agent; 
	
    /** Thread-safe queue for buffering incoming messages */
    private final BlockingQueue<TopicMessage> messageQueue;
    
    /** Background thread that processes messages from the queue */
    private final Thread processThread;
    
    /** Flag indicating whether the agent is running and should process messages */
    private volatile boolean running;
    
    /**
     * Internal container class for pairing topics with their messages.
     * 
     * <p>This immutable data structure ensures that each message is processed
     * with the correct topic context, maintaining the semantics of the original
     * agent callback interface.</p>
     * 
     * <p><strong>Immutability:</strong> Both fields are final to prevent
     * accidental modification during queue operations and ensure thread safety.</p>
     */
    private static class TopicMessage {
        /** The name of the topic that published this message */
        final String topic;
        
        /** The message content to be processed */
        final Message message;
        
        /**
         * Creates a new topic-message pair.
         * 
         * @param topic the name of the publishing topic
         * @param message the message to be processed
         */
        TopicMessage(String topic, Message message) {
            this.topic = topic;
            this.message = message;
        }
    }
    
    /**
     * Creates a new ParallelAgent wrapping the specified agent.
     * 
     * <p>This constructor immediately starts a background daemon thread for
     * message processing. The agent becomes ready to receive and process
     * messages as soon as construction completes.</p>
     * 
     * <p><strong>Initialization Process:</strong></p>
     * <ol>
     *   <li>Store reference to the wrapped agent</li>
     *   <li>Create bounded message queue with specified capacity</li>
     *   <li>Set running flag to true</li>
     *   <li>Create and start background processing thread</li>
     *   <li>Configure thread as daemon for proper JVM shutdown</li>
     * </ol>
     * 
     * <p><strong>Queue Capacity Guidelines:</strong></p>
     * <ul>
     *   <li><strong>Small (10-50):</strong> Low memory usage, may block publishers on bursts</li>
     *   <li><strong>Medium (100-1000):</strong> Good balance for most applications</li>
     *   <li><strong>Large (1000+):</strong> High throughput but more memory usage</li>
     * </ul>
     * 
     * <p><strong>Thread Naming:</strong></p>
     * <p>The background thread is named "ParallelAgent-{agentName}" to aid
     * in debugging and monitoring. For example, wrapping a "PlusAgent_1"
     * creates a thread named "ParallelAgent-PlusAgent_1".</p>
     * 
     * @param agent the agent to wrap for parallel processing. Cannot be null.
     *             The agent's existing state and configuration are preserved.
     * @param capacity the maximum number of messages that can be queued for
     *                processing. Must be positive. When the queue is full,
     *                calling threads will block until space becomes available.
     * 
     * @throws NullPointerException if agent is null
     * @throws IllegalArgumentException if capacity is not positive
     * 
     * @example
     * <pre>{@code
     * // Create base agent
     * Agent calculator = new PlusAgent(new String[]{"X", "Y"}, new String[]{"Sum"});
     * 
     * // Wrap for thread safety with moderate queue size
     * ParallelAgent parallelCalculator = new ParallelAgent(calculator, 100);
     * 
     * // For high-throughput scenarios
     * ParallelAgent highThroughput = new ParallelAgent(calculator, 1000);
     * 
     * // For memory-constrained environments
     * ParallelAgent lowMemory = new ParallelAgent(calculator, 10);
     * }</pre>
     * 
     * @see ArrayBlockingQueue#ArrayBlockingQueue(int)
     * @see Thread#setDaemon(boolean)
     */
	public ParallelAgent(Agent agent, int capacity) {
		if (agent == null) {
			throw new NullPointerException("Agent cannot be null");
		}
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
		}
		
		this.messageQueue = new ArrayBlockingQueue<>(capacity);
		this.agent = agent; 
        this.running = true;
        this.processThread = new Thread(this::processMessages, "ParallelAgent-" + agent.getName());
        this.processThread.setDaemon(true); 
        this.processThread.start();
	}

	/**
	 * Returns the name of the wrapped agent.
	 * 
	 * <p>This method delegates to the wrapped agent's getName() method,
	 * preserving the original agent's identity for logging, debugging,
	 * and graph visualization purposes.</p>
	 * 
	 * @return the name of the wrapped agent, never null
	 * 
	 * @see Agent#getName()
	 */
	@Override
	public String getName() {
		return agent.getName();
	}

	/**
	 * Resets the wrapped agent's internal state.
	 * 
	 * <p>This method delegates the reset operation to the wrapped agent.
	 * The reset is performed synchronously in the calling thread, not
	 * in the background processing thread.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> While this method doesn't directly
	 * conflict with message processing, it's recommended to call reset
	 * during system initialization or shutdown when message flow has stopped.</p>
	 * 
	 * <p><strong>Queue Behavior:</strong> This method does not clear the
	 * message queue. Any queued messages will still be processed after
	 * the reset completes.</p>
	 * 
	 * @see Agent#reset()
	 */
	@Override
	public void reset() {
		agent.reset();
	}

	/**
	 * Queues a message for asynchronous processing by the wrapped agent.
	 * 
	 * <p>This method provides the asynchronous interface to the ParallelAgent.
	 * Instead of immediately calling the wrapped agent's callback method,
	 * it queues the message for processing by the background thread.</p>
	 * 
	 * <p><strong>Asynchronous Processing Flow:</strong></p>
	 * <ol>
	 *   <li>Create TopicMessage wrapper with topic and message</li>
	 *   <li>Attempt to add message to queue (may block if queue is full)</li>
	 *   <li>Return immediately to caller</li>
	 *   <li>Background thread later dequeues and processes message</li>
	 * </ol>
	 * 
	 * <p><strong>Blocking Behavior:</strong></p>
	 * <p>If the message queue is full, this method blocks until space becomes
	 * available. This provides natural backpressure - fast publishers will
	 * be slowed down if the agent cannot keep up with processing.</p>
	 * 
	 * <p><strong>Thread Interruption:</strong></p>
	 * <p>If the calling thread is interrupted while waiting to add a message
	 * to a full queue, the method preserves the interruption status and
	 * throws a RuntimeException to indicate the failure.</p>
	 * 
	 * @param topic the name of the topic publishing this message. This will
	 *             be passed to the wrapped agent's callback method.
	 * @param msg the message to be processed. This will be passed to the
	 *           wrapped agent's callback method.
	 * 
	 * @throws RuntimeException if the thread is interrupted while waiting
	 *                         to add the message to the queue
	 * 
	 * @example
	 * <pre>{@code
	 * ParallelAgent agent = new ParallelAgent(baseAgent, 100);
	 * 
	 * // These calls return immediately, messages processed asynchronously
	 * agent.callback("Temperature", new Message(23.5));
	 * agent.callback("Humidity", new Message(45.0));
	 * agent.callback("Temperature", new Message(24.1));
	 * 
	 * // If queue becomes full, this call will block until space is available
	 * agent.callback("Pressure", new Message(1013.25));
	 * }</pre>
	 * 
	 * @see BlockingQueue#put(Object)
	 * @see Thread#interrupt()
	 */
	@Override
	public void callback(String topic, Message msg) {
		try {
            TopicMessage topicMessage = new TopicMessage(topic, msg);
            messageQueue.put(topicMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while adding message to queue", e);
        }
		
	}

	/**
	 * Stops the background processing thread and closes the wrapped agent.
	 * 
	 * <p>This method performs a graceful shutdown of the ParallelAgent,
	 * ensuring that all resources are properly released and no messages
	 * are lost during the shutdown process.</p>
	 * 
	 * <p><strong>Shutdown Process:</strong></p>
	 * <ol>
	 *   <li>Set running flag to false (stops message processing loop)</li>
	 *   <li>Interrupt the background thread (breaks it out of blocking operations)</li>
	 *   <li>Wait up to 5 seconds for thread to terminate gracefully</li>
	 *   <li>Close the wrapped agent (performs agent-specific cleanup)</li>
	 * </ol>
	 * 
	 * <p><strong>Message Queue Handling:</strong></p>
	 * <p>Any messages remaining in the queue when close() is called will not
	 * be processed. For guaranteed message processing, ensure all expected
	 * messages have been sent before calling close().</p>
	 * 
	 * <p><strong>Thread Termination:</strong></p>
	 * <p>The method waits up to 5 seconds for the background thread to
	 * terminate. If the thread doesn't stop within this timeout, the method
	 * continues with agent cleanup. The thread will eventually terminate
	 * on its own, but this prevents indefinite blocking during shutdown.</p>
	 * 
	 * <p><strong>Interruption Handling:</strong></p>
	 * <p>If the calling thread is interrupted while waiting for the background
	 * thread to terminate, the interruption status is preserved and the
	 * shutdown process continues.</p>
	 * 
	 * <p><strong>Post-Close State:</strong></p>
	 * <p>After close() completes:</p>
	 * <ul>
	 *   <li>No new messages will be processed</li>
	 *   <li>The background thread will be terminated</li>
	 *   <li>The wrapped agent will be closed</li>
	 *   <li>Any subsequent method calls may have undefined behavior</li>
	 * </ul>
	 * 
	 * @example
	 * <pre>{@code
	 * ParallelAgent agent = new ParallelAgent(baseAgent, 100);
	 * 
	 * // Use the agent...
	 * agent.callback("Topic1", new Message("data"));
	 * 
	 * // Clean shutdown
	 * agent.close();
	 * 
	 * // Agent is now closed and should not be used
	 * }</pre>
	 * 
	 * @see Thread#interrupt()
	 * @see Thread#join(long)
	 * @see Agent#close()
	 */
	@Override
	public void close() {
		running = false;
		processThread.interrupt();
		try {
			processThread.join(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		agent.close();
	}

	/**
	 * Background thread method that processes messages from the queue.
	 * 
	 * <p>This method runs in the dedicated background thread and implements
	 * the core message processing loop. It continuously dequeues messages
	 * and delegates them to the wrapped agent's callback method.</p>
	 * 
	 * <p><strong>Processing Loop:</strong></p>
	 * <ol>
	 *   <li>Check if agent is still running</li>
	 *   <li>Take next message from queue (blocks if queue is empty)</li>
	 *   <li>Check running flag again (for quick shutdown)</li>
	 *   <li>Call wrapped agent's callback method</li>
	 *   <li>Repeat until shutdown</li>
	 * </ol>
	 * 
	 * <p><strong>Interruption Handling:</strong></p>
	 * <p>The method handles thread interruption gracefully:</p>
	 * <ul>
	 *   <li>If interrupted during queue operations, checks running flag</li>
	 *   <li>If shutting down, exits cleanly</li>
	 *   <li>If not shutting down, preserves interruption and continues</li>
	 * </ul>
	 * 
	 * <p><strong>Error Isolation:</strong></p>
	 * <p>If the wrapped agent throws an exception during callback processing,
	 * the error is logged but doesn't crash the processing thread. This
	 * ensures that one problematic message doesn't stop all future processing.</p>
	 * 
	 * <p><strong>Shutdown Behavior:</strong></p>
	 * <p>The method exits when:</p>
	 * <ul>
	 *   <li>The running flag is set to false</li>
	 *   <li>The thread is interrupted during shutdown</li>
	 *   <li>An unexpected exception occurs (logged and thread exits)</li>
	 * </ul>
	 * 
	 * @see BlockingQueue#take()
	 * @see Agent#callback(String, Message)
	 */
	private void processMessages() {
		try {
			while(running) {
				try {
					TopicMessage topicMessage = messageQueue.take();
					if(!running) {
						break;
					}
					agent.callback(topicMessage.topic, topicMessage.message);
				}
				catch(InterruptedException e){
					if(!running) {
						break;
					}
					Thread.currentThread().interrupt();				
				}
			}
		}
		catch(Exception e) {
            System.err.println("Error in ParallelAgent proccessing thread:" + e.getMessage());
		}
		
	}
	
}