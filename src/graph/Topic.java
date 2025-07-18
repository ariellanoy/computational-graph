package graph;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import servlets.TopicDisplayer;

/**
 * Represents a communication channel for message passing between agents in a computational graph.
 * 
 * <p> A Topic serves as a named message path between 
 * publishers and subscribers in the computational graph. Topics
 * implement the publish-subscribe pattern, allowing multiple agents to subscribe to
 * receive messages and multiple agents to publish messages to the same channel.</p>
 * 
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li><strong>Thread-Safe:</strong> Uses CopyOnWriteArrayList for concurrent access</li>
 *   <li><strong>Many-to-Many:</strong> Multiple publishers can send to multiple subscribers</li>
 *   <li><strong>Immediate Delivery:</strong> Messages are delivered synchronously to all subscribers</li>
 *   <li><strong>Duplicate Prevention:</strong> Agents cannot subscribe or publish multiple times</li>
 *   <li><strong>Visualization Integration:</strong> Automatically updates UI displays</li>
 * </ul>
 * 
 * <p><strong>Computational Graph Integration:</strong></p>
 * <p>Topics serve as the "wires" in computational graphs, connecting agents that
 * produce data with agents that consume and process it. This creates flexible
 * data flow patterns:</p>
 * <ul>
 *   <li><strong>Fan-out:</strong> One publisher, multiple subscribers</li>
 *   <li><strong>Fan-in:</strong> Multiple publishers, one subscriber</li>
 *   <li><strong>Pipeline:</strong> Chain of processing stages</li>
 *   <li><strong>Broadcast:</strong> Data distributed to all interested parties</li>
 * </ul>
 * 
 * <p><strong>Error Handling:</strong></p>
 * <p>The Topic provides graceful error handling for UI integration while ensuring
 * core message delivery continues even if visualization updates fail.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Agent
 * @see Message
 * @see TopicManagerSingleton
 * @see servlets.TopicDisplayer
 */
public class Topic {
	
	/** The unique name identifying this topic channel */
	public final String name;
	
	/** Thread-safe list of agents subscribed to receive messages from this topic */
	public final List<Agent> subs;
	
	/** Thread-safe list of agents registered as publishers to this topic */
	public final List<Agent> pubs;
	
	/**
	 * Creates a new Topic with the specified name.
	 * 
	 * <p>Initializes an empty topic with no subscribers or publishers.
	 * The topic is immediately ready to accept subscriptions and publications.</p>
	 * 
	 * <p><strong>Thread-Safe Collections:</strong></p>
	 * <p>Both subscriber and publisher lists are initialized as 
	 * {@link CopyOnWriteArrayList} instances, providing thread safety
	 * with the following characteristics:</p>
	 * <ul>
	 *   <li>Lock-free reads enable high-performance message publishing</li>
	 *   <li>Atomic writes ensure consistent list state during modifications</li>
	 *   <li>Iterators are immune to concurrent modifications</li>
	 * </ul>
	 * 
	 * <p><strong>Naming Conventions:</strong></p>
	 * <p>Topic names should be:</p>
	 * <ul>
	 *   <li>Descriptive of the data they carry (e.g., "Temperature", "UserInput")</li>
	 *   <li>Unique within the computational graph</li>
	 *   <li>Consistent with graph visualization conventions</li>
	 * </ul>
	 * 
	 * @param name the unique identifier for this topic. Should be non-null
	 *            and descriptive of the data that will flow through this channel.
	 *            This name is used for agent subscriptions, graph visualization,
	 *            and debugging purposes.
	 * 
	 * @throws NullPointerException if name is null (implicit from field assignment)
	 * 
	 * @see CopyOnWriteArrayList
	 */
	public Topic(String name) {
		this.name = name;
		subs = new CopyOnWriteArrayList<>();
		pubs = new CopyOnWriteArrayList<>();
	}
	
	/**
	 * Subscribes an agent to receive messages published to this topic.
	 * 
	 * <p>Once subscribed, the agent's {@link Agent#callback(String, Message)}
	 * method will be called for every message published to this topic. The
	 * subscription remains active until explicitly removed via 
	 * {@link #unsubscribe(Agent)}.</p>
	 * 
	 * <p><strong>Duplicate Prevention:</strong></p>
	 * <p>If the agent is already subscribed to this topic, the method has no
	 * effect. This prevents duplicate message delivery and maintains list
	 * integrity.</p>
	 * 
	 * <p><strong>Thread Safety:</strong></p>
	 * <p>This method is thread-safe and can be called concurrently with
	 * message publishing and other subscription operations.</p>
	 * 
	 * <p><strong>Immediate Effect:</strong></p>
	 * <p>The subscription takes effect immediately. If messages are published
	 * after this method returns, the agent will receive them.</p>
	 * 
	 * @param agent the agent to subscribe to this topic. The agent must
	 *             implement the {@link Agent} interface and be prepared
	 *             to handle callback invocations. Cannot be null.
	 * 
	 * @throws NullPointerException if agent is null (from List.contains/add)
	 *
	 * 
	 * @see #unsubscribe(Agent)
	 * @see #publish(Message)
	 * @see Agent#callback(String, Message)
	 */
	public void subscribe(Agent agent) {
		if(!subs.contains(agent)) {
			subs.add(agent);
		}
	}
	
	/**
	 * Removes an agent's subscription from this topic.
	 * 
	 * <p>After unsubscribing, the agent will no longer receive messages
	 * published to this topic. If the agent was not previously subscribed,
	 * the method has no effect.</p>
	 * 
	 * <p><strong>Immediate Effect:</strong></p>
	 * <p>The unsubscription takes effect immediately. Messages published
	 * after this method returns will not be delivered to the agent.</p>
	 * 
	 * <p><strong>Thread Safety:</strong></p>
	 * <p>This method is thread-safe and can be called concurrently with
	 * message publishing and other subscription operations. Due to the
	 * use of CopyOnWriteArrayList, ongoing message deliveries will complete
	 * with the previous subscriber list.</p>
	 * 
	 * <p><strong>Resource Cleanup:</strong></p>
	 * <p>This method is essential for proper resource management. Agents
	 * should unsubscribe from topics during shutdown to prevent memory
	 * leaks and ensure clean system termination.</p>
	 * 
	 * @param agent the agent to unsubscribe from this topic. If the agent
	 *             is not currently subscribed, the method has no effect.
	 *             Can be null (no effect).
	 * 
	 * @example
	 * <pre>{@code
	 * Topic topic = new Topic("Events");
	 * Agent listener = new EventListener();
	 * 
	 * // Subscribe and use
	 * topic.subscribe(listener);
	 * topic.publish(new Message("Event1")); // listener receives this
	 * 
	 * // Unsubscribe
	 * topic.unsubscribe(listener);
	 * topic.publish(new Message("Event2")); // listener does NOT receive this
	 * 
	 * // Unsubscribing again has no effect
	 * topic.unsubscribe(listener); // Safe to call
	 * 
	 * // Typical cleanup pattern
	 * public void cleanup() {
	 *     for (Topic topic : myTopics) {
	 *         topic.unsubscribe(this);
	 *     }
	 * }
	 * }</pre>
	 * 
	 * @see #subscribe(Agent)
	 * @see Agent#close()
	 */
	public void unsubscribe(Agent agent) {
		subs.remove(agent);
	}
	
	/**
	 * Publishes a message to all subscribed agents.
	 * 
	 * <p>This method delivers the message to every agent currently subscribed
	 * to this topic by calling their {@link Agent#callback(String, Message)}
	 * method. The delivery is synchronous - this method blocks until all
	 * subscribers have been notified.</p>
	 * 
	 * <p><strong>Message Delivery Process:</strong></p>
	 * <ol>
	 *   <li>Update the topic displayer for UI visualization</li>
	 *   <li>Log the message publication for debugging</li>
	 *   <li>Iterate through all current subscribers</li>
	 *   <li>Call each subscriber's callback method with topic name and message</li>
	 *   <li>Continue delivery even if individual callbacks fail</li>
	 * </ol>
	 * 
	 * <p><strong>Delivery Guarantees:</strong></p>
	 * <ul>
	 *   <li><strong>All Subscribers:</strong> Every currently subscribed agent receives the message</li>
	 *   <li><strong>Consistent Snapshot:</strong> Uses the subscriber list as it existed when publish() started</li>
	 *   <li><strong>Order Preservation:</strong> Subscribers are notified in list order</li>
	 *   <li><strong>No Persistence:</strong> Messages are not stored; only current subscribers receive them</li>
	 * </ul>
	 * 
	 * <p><strong>Error Handling:</strong></p>
	 * <ul>
	 *   <li><strong>UI Update Failures:</strong> Logged but don't prevent message delivery</li>
	 *   <li><strong>Subscriber Exceptions:</strong> Currently propagate to caller (implementation detail)</li>
	 *   <li><strong>Null Messages:</strong> Passed through to subscribers (subscribers must handle)</li>
	 * </ul>
	 * 
	 * <p><strong>Performance Characteristics:</strong></p>
	 * <ul>
	 *   <li><strong>Synchronous:</strong> Blocks until all deliveries complete</li>
	 *   <li><strong>O(n) Time:</strong> Linear in number of subscribers</li>
	 *   <li><strong>Memory Efficient:</strong> No message copying or queuing</li>
	 *   <li><strong>Thread Safe:</strong> Can be called from multiple threads concurrently</li>
	 * </ul>
	 * 
	 * <p><strong>Integration with Visualization:</strong></p>
	 * <p>The method automatically updates the {@link servlets.TopicDisplayer}
	 * to maintain current topic values for web-based monitoring and debugging.
	 * This enables real-time visualization of data flowing through the 
	 * computational graph.</p>
	 * 
	 * @param msg the message to publish to all subscribers. While null messages
	 *           are technically allowed, subscribers should be prepared to handle
	 *           them appropriately. Typically contains data relevant to the
	 *           topic's domain (e.g., sensor readings, computation results).
	 * 
	 * @example
	 * <pre>{@code
	 * Topic sensorTopic = new Topic("Temperature");
	 * 
	 * // Set up subscribers
	 * Agent display = new DisplayAgent();
	 * Agent logger = new LoggerAgent(); 
	 * Agent alerter = new AlertAgent();
	 * 
	 * sensorTopic.subscribe(display);
	 * sensorTopic.subscribe(logger);
	 * sensorTopic.subscribe(alerter);
	 * 
	 * // Publish sensor reading
	 * Message tempReading = new Message(23.5);
	 * sensorTopic.publish(tempReading);
	 * 
	 * // This results in:
	 * // 1. UI updated with latest temperature value
	 * // 2. display.callback("Temperature", tempReading) 
	 * // 3. logger.callback("Temperature", tempReading)
	 * // 4. alerter.callback("Temperature", tempReading)
	 * 
	 * // Publishing to topic with no subscribers
	 * Topic emptyTopic = new Topic("Empty");
	 * emptyTopic.publish(new Message("ignored")); // Safe, no effect
	 * }</pre>
	 * 
	 * @see Agent#callback(String, Message)
	 * @see servlets.TopicDisplayer#updateLastMessage(String, Message)
	 * @see Message
	 */
	public void publish(Message msg) {
		try {
            TopicDisplayer.updateLastMessage(name, msg);
            System.out.println("Updated topic displayer for topic '" + name + "' with value: " + msg.asText);
        } catch (Exception e) {
            System.err.println("Error updating topic displayer: " + e.getMessage());
        }
		for(Agent agent : subs) {
			agent.callback(name, msg);
		}
	}	

	/**
	 * Registers an agent as a publisher for this topic.
	 * 
	 * <p>This method adds the agent to the list of known publishers for this
	 * topic. While registration is not required for an agent to publish messages
	 * (any code can call {@link #publish(Message)}), maintaining a publisher
	 * list enables:</p>
	 * <ul>
	 *   <li><strong>Graph Visualization:</strong> Shows which agents produce data for this topic</li>
	 *   <li><strong>Dependency Analysis:</strong> Tracks data flow relationships</li>
	 *   <li><strong>Resource Management:</strong> Enables cleanup of publisher relationships</li>
	 *   <li><strong>Debugging:</strong> Identifies data sources during troubleshooting</li>
	 * </ul>
	 * 
	 * <p><strong>Duplicate Prevention:</strong></p>
	 * <p>If the agent is already registered as a publisher for this topic,
	 * the method has no effect. This maintains list integrity and prevents
	 * duplicate entries in visualizations.</p>
	 * 
	 * <p><strong>Thread Safety:</strong></p>
	 * <p>This method is thread-safe and can be called concurrently with
	 * other publisher operations and message publishing.</p>
	 * 
	 * <p><strong>No Publishing Enforcement:</strong></p>
	 * <p>Registering as a publisher does not grant exclusive publishing rights
	 * or prevent other entities from publishing to the topic. It's purely
	 * informational for graph structure tracking.</p>
	 * 
	 * @param agent the agent to register as a publisher for this topic.
	 *             Typically this is done during agent initialization to
	 *             establish the computational graph structure. Cannot be null.
	 * 
	 * @throws NullPointerException if agent is null (from List.contains/add)
	 * 
	 * @example
	 * <pre>{@code
	 * Topic resultTopic = new Topic("CalculationResults");
	 * 
	 * // Create computational agents
	 * Agent calculator1 = new MathAgent("Adder");
	 * Agent calculator2 = new MathAgent("Multiplier");
	 * Agent dataSource = new InputAgent();
	 * 
	 * // Register publishers (establishes graph structure)
	 * resultTopic.addPublisher(calculator1);  // calculator1 produces results
	 * resultTopic.addPublisher(calculator2);  // calculator2 also produces results
	 * 
	 * // Duplicate registration has no effect
	 * resultTopic.addPublisher(calculator1);  // No change
	 * 
	 * // Now graph visualization can show:
	 * // calculator1 → resultTopic
	 * // calculator2 → resultTopic
	 * 
	 * // Agents can now publish (registration enables this conceptually)
	 * calculator1.computeAndPublish(); // Calls resultTopic.publish(...)
	 * calculator2.computeAndPublish(); // Calls resultTopic.publish(...)
	 * }</pre>
	 * 
	 * @see #removePublisher(Agent)
	 * @see #publish(Message)
	 * @see configs.Graph#createFromTopics()
	 */
	public void addPublisher(Agent agent) {
		if(!pubs.contains(agent)) {
			pubs.add(agent);
		}
	}
	
	/**
	 * Removes an agent's registration as a publisher for this topic.
	 * 
	 * <p>This method removes the agent from the list of registered publishers.
	 * It's primarily used for cleanup during agent shutdown and for maintaining
	 * accurate graph structure information.</p>
	 * 
	 * <p><strong>Effect on Publishing:</strong></p>
	 * <p>Removing publisher registration does NOT prevent the agent from
	 * publishing messages to this topic. The {@link #publish(Message)} method
	 * can be called by any code regardless of publisher registration status.
	 * This method only affects:</p>
	 * <ul>
	 *   <li>Graph visualization accuracy</li>
	 *   <li>Dependency analysis results</li>
	 *   <li>Publisher list queries</li>
	 * </ul>
	 * 
	 * <p><strong>Cleanup Usage:</strong></p>
	 * <p>This method is typically called during agent shutdown to ensure
	 * clean resource management and prevent stale references in the
	 * publisher list.</p>
	 * 
	 * <p><strong>Thread Safety:</strong></p>
	 * <p>This method is thread-safe and can be called concurrently with
	 * other publisher operations and message publishing.</p>
	 * 
	 * @param agent the agent to remove from the publisher list. If the agent
	 *             is not currently registered as a publisher, the method has
	 *             no effect. Can be null (no effect).
	 * 
	 * @example
	 * <pre>{@code
	 * Topic outputTopic = new Topic("Results");
	 * Agent processor = new DataProcessor();
	 * 
	 * // Register as publisher
	 * outputTopic.addPublisher(processor);
	 * 
	 * // Agent operates normally
	 * processor.processData(); // May call outputTopic.publish(...)
	 * 
	 * // During shutdown, clean up publisher registration
	 * outputTopic.removePublisher(processor);
	 * 
	 * // Agent can still publish if needed, but won't appear in graph
	 * outputTopic.publish(new Message("final result")); // Still works
	 * 
	 * // Typical cleanup pattern in agent close() method
	 * public void close() {
	 *     for (Topic topic : myPublishedTopics) {
	 *         topic.removePublisher(this);
	 *     }
	 *     // Other cleanup...
	 * }
	 * }</pre>
	 * 
	 * @see #addPublisher(Agent)
	 * @see Agent#close()
	 */
	public void removePublisher(Agent agent) {
		pubs.remove(agent);
	}
	
}