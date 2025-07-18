package configs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import graph.Agent;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Represents a computational graph composed of nodes representing topics and agents.
 * 
 * <p>This class extends {@link ArrayList} to provide a collection of {@link Node} objects
 * that represent the structure of a computational graph. The graph can be automatically
 * constructed from the current topic configuration managed by {@link TopicManagerSingleton},
 * creating a visual representation of the relationships between topics and agents.</p>
 * 
 * <p><strong>Graph Structure:</strong></p>
 * <ul>
 *   <li><strong>Topic Nodes:</strong> Prefixed with "T" (e.g., "TTopicName")</li>
 *   <li><strong>Agent Nodes:</strong> Prefixed with "A" (e.g., "AAgentName")</li>
 *   <li><strong>Edges:</strong> Represent data flow between topics and agents</li>
 * </ul>
 * 
 * <p><strong>Data Flow Representation:</strong></p>
 * <ul>
 *   <li>Topic → Agent: When an agent subscribes to a topic</li>
 *   <li>Agent → Topic: When an agent publishes to a topic</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Assume we have a configuration with agents and topics set up
 * GenericConfig config = new GenericConfig();
 * config.setConfFile("path/to/config.conf");
 * config.create();
 * 
 * // Create graph from the current topic configuration
 * Graph graph = new Graph();
 * graph.createFromTopics();
 * 
 * // Check for cycles in the computational graph
 * if (graph.hasCycles()) {
 *     System.out.println("Warning: The computational graph contains cycles!");
 *     // Handle cycle detection...
 * }
 * 
 * // Access individual nodes
 * System.out.println("Graph has " + graph.size() + " nodes:");
 * for (Node node : graph) {
 *     System.out.println("Node: " + node.getName());
 *     System.out.println("Edges: " + node.getEdges().size());
 * }
 * 
 * // Clean up
 * config.close();
 * }</pre>
 * 
 * <p><strong>Visualization Support:</strong></p>
 * <p>This class is designed to work with visualization tools that can render
 * the graph structure. The naming convention (T for topics, A for agents) helps
 * visualization systems distinguish between different node types for appropriate
 * styling and layout.</p>
 * 
 * <p><strong>Cycle Detection:</strong></p>
 * <p>The graph supports cycle detection which is crucial for computational graphs
 * as cycles can lead to infinite loops or unstable computations. The cycle detection
 * algorithm traverses the entire graph to identify any circular dependencies.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. If accessed
 * from multiple threads, external synchronization is required.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Node
 * @see TopicManagerSingleton
 * @see Agent
 * @see Topic
 */
public class Graph extends ArrayList<Node> {
    
    /** Serialization version identifier */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs an empty Graph.
     * 
     * <p>The graph is initially empty. Use {@link #createFromTopics()} to populate
     * it with nodes representing the current topic and agent configuration.</p>
     */
    public Graph() {
        super();
    }
    
    /**
     * Checks if the computational graph contains any cycles.
     * 
     * <p>A cycle in a computational graph represents a circular dependency where
     * data flows in a loop. This can be problematic in computational systems as
     * it may lead to:</p>
     * <ul>
     *   <li>Infinite loops in computation</li>
     *   <li>Stack overflow errors</li>
     *   <li>Unstable or unpredictable behavior</li>
     *   <li>Deadlock situations</li>
     * </ul>
     * 
     * <p><strong>Algorithm:</strong> This method iterates through all nodes in the
     * graph and uses each node's individual cycle detection algorithm. If any node
     * detects a cycle in its reachable subgraph, the entire graph is considered
     * to contain cycles.</p>
     * 
     * <p><strong>Performance:</strong> The time complexity is O(V * (V + E)) where
     * V is the number of vertices (nodes) and E is the number of edges, as each
     * node may trigger a full graph traversal.</p>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * Graph graph = new Graph();
     * graph.createFromTopics();
     * 
     * if (graph.hasCycles()) {
     *     System.err.println("Cycle detected! The computational graph may not behave correctly.");
     *     // Consider:
     *     // 1. Reviewing the configuration file
     *     // 2. Redesigning the agent connections
     *     // 3. Adding cycle-breaking mechanisms
     * } else {
     *     System.out.println("Graph is acyclic - safe for computation.");
     * }
     * }</pre>
     * 
     * @return {@code true} if the graph contains at least one cycle,
     *         {@code false} if the graph is acyclic (cycle-free)
     * 
     * @see Node#hasCycles()
     */
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates the graph structure from the current topics in the TopicManager.
     * 
     * <p>This method automatically constructs a computational graph by analyzing
     * the current topic and agent configuration. It creates nodes for all topics
     * and agents, then establishes edges based on subscription and publishing
     * relationships.</p>
     * 
     * <p><strong>Construction Process:</strong></p>
     * <ol>
     *   <li><strong>Clear existing graph:</strong> Removes all current nodes</li>
     *   <li><strong>Retrieve topics:</strong> Gets all topics from TopicManagerSingleton</li>
     *   <li><strong>Create topic nodes:</strong> For each topic, creates a node with name "T{topicName}"</li>
     *   <li><strong>Process subscribers:</strong> For each agent subscribed to a topic:
     *       <ul>
     *         <li>Creates agent node "A{agentName}" if it doesn't exist</li>
     *         <li>Creates edge from topic node to agent node</li>
     *       </ul>
     *   </li>
     *   <li><strong>Process publishers:</strong> For each agent publishing to a topic:
     *       <ul>
     *         <li>Creates agent node "A{agentName}" if it doesn't exist</li>
     *         <li>Creates edge from agent node to topic node</li>
     *       </ul>
     *   </li>
     * </ol>
     * 
     * <p><strong>Node Naming Convention:</strong></p>
     * <ul>
     *   <li><strong>Topics:</strong> "T" + topic name (e.g., "TInputData", "TResult")</li>
     *   <li><strong>Agents:</strong> "A" + agent name (e.g., "APlusAgent_1", "AIncAgent_2")</li>
     * </ul>
     * 
     * <p><strong>Edge Semantics:</strong></p>
     * <ul>
     *   <li><strong>Topic → Agent:</strong> Data flows from topic to subscribing agent</li>
     *   <li><strong>Agent → Topic:</strong> Agent publishes data to topic</li>
     * </ul>
     * 
     * <p><strong>Duplicate Prevention:</strong> The method ensures that each unique
     * topic or agent appears only once in the graph, even if it has multiple
     * connections. A {@link HashMap} is used to track created nodes.</p>
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * // Set up configuration
     * GenericConfig config = new GenericConfig();
     * config.setConfFile("mathematical_operations.conf");
     * config.create();
     * 
     * // Create graph representation
     * Graph graph = new Graph();
     * graph.createFromTopics();
     * 
     * System.out.println("Created graph with " + graph.size() + " nodes:");
     * 
     * // Analyze the graph structure
     * int topicCount = 0, agentCount = 0;
     * for (Node node : graph) {
     *     if (node.getName().startsWith("T")) {
     *         topicCount++;
     *     } else if (node.getName().startsWith("A")) {
     *         agentCount++;
     *     }
     * }
     * 
     * System.out.println("Topics: " + topicCount + ", Agents: " + agentCount);
     * }</pre>
     * 
     * <p><strong>State Management:</strong> This method clears any existing graph
     * content before reconstruction, ensuring a clean state that accurately reflects
     * the current topic configuration.</p>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li>Time complexity: O(T + A + E) where T=topics, A=agents, E=total connections</li>
     *   <li>Space complexity: O(T + A) for storing unique nodes</li>
     *   <li>Should be called after configuration changes to maintain accuracy</li>
     * </ul>
     * 
     * @throws RuntimeException if there's an error accessing the TopicManager
     *                         or if the topic configuration is in an invalid state
     * 
     * @see TopicManagerSingleton#get()
     * @see TopicManager#getTopics()
     * @see Topic#subs
     * @see Topic#pubs
     * @see Node#addEdge(Node)
     */
    public void createFromTopics() {
        // Clear existing graph content
        this.clear();
        
        // Get the topic manager and all current topics
        TopicManager tm = TopicManagerSingleton.get();
        Collection<Topic> topics = tm.getTopics();
        
        // Map to prevent duplicate node creation
        Map<String, Node> nodeMap = new HashMap<>();
        
        // Process each topic in the system
        for (Topic topic : topics) {
            // Create topic node with "T" prefix
            String topicNodeName = "T" + topic.name;
            Node topicNode = new Node(topicNodeName);
            nodeMap.put(topicNodeName, topicNode);
            this.add(topicNode);
            
            // Process all subscribers 
            // Creates edge: Topic → Agent 
            for (Agent sub : topic.subs) {
                String subscriberNodeName = "A" + sub.getName();
                
                // Create subscriber node if it doesn't exist
                Node subNode = nodeMap.get(subscriberNodeName);
                if (subNode == null) {
                	subNode = new Node(subscriberNodeName);
                    nodeMap.put(subscriberNodeName, subNode);
                    this.add(subNode);
                }
                
                // Add edge from topic to subscriber 
                topicNode.addEdge(subNode);
            }
            
            // Process all publishers 
            // Creates edge: Agent → Topic 
            for (Agent pub : topic.pubs) {
                String publisherNodeName = "A" + pub.getName();
                
                // Create publisher node if it doesn't exist
                Node pubNode = nodeMap.get(publisherNodeName);
                if (pubNode == null) {
                	pubNode = new Node(publisherNodeName);
                    nodeMap.put(publisherNodeName, pubNode);
                    this.add(pubNode);
                }
                
                // Add edge from publisher to topic
                pubNode.addEdge(topicNode);
            }
        }
    }
}