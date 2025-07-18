package configs;

import graph.Message;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

/**
 * Represents a node in a computational graph with cycle detection capabilities.
 * 
 * <p>A Node is a fundamental building block of computational graphs, representing
 * either a topic or an agent in the system. Each node has a unique name, can
 * maintain a current message value, and connects to other nodes through directed
 * edges that represent data flow relationships.</p>
 * 
 * <p><strong>Node Types:</strong></p>
 * <ul>
 *   <li><strong>Topic Nodes:</strong> Represent data channels (typically prefixed with "T")</li>
 *   <li><strong>Agent Nodes:</strong> Represent computational units (typically prefixed with "A")</li>
 * </ul>
 * 
 * <p><strong>Cycle Detection:</strong></p>
 * <p>One of the key features of this class is its ability to detect cycles
 * in the graph structure. Cycles are problematic in computational graphs as
 * they can lead to infinite loops, stack overflows, or unstable behavior.</p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Create nodes for a simple computational graph
 * Node inputTopic = new Node("TInput");
 * Node processorAgent = new Node("AProcessor");  
 * Node outputTopic = new Node("TOutput");
 * 
 * // Set up data flow: Input → Processor → Output
 * inputTopic.addEdge(processorAgent);
 * processorAgent.addEdge(outputTopic);
 * 
 * // Check for cycles (should be false for this linear flow)
 * boolean hasCycle = inputTopic.hasCycles();
 * System.out.println("Graph has cycles: " + hasCycle); // false
 * 
 * // Store current value in a topic node
 * Message currentValue = new Message(42.0);
 * inputTopic.setMessage(currentValue);
 * 
 * // Retrieve and use the value
 * Message retrieved = inputTopic.getmessage();
 * if (retrieved != null) {
 *     System.out.println("Current value: " + retrieved.asText);
 * }
 * }</pre>
 * 
 * 
 * <p><strong>Integration with Graph Class:</strong></p>
 * <p>Nodes are typically used within the {@link Graph} class to represent
 * entire computational graphs. The Graph class leverages Node's cycle detection
 * to validate the overall graph structure.</p>
 * 
 * <p><strong>Message Storage:</strong></p>
 * <p>Nodes can optionally store a current {@link Message} value, which is
 * particularly useful for topic nodes that need to maintain their latest
 * published value for visualization or debugging purposes.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. If accessed
 * from multiple threads, external synchronization is required.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Graph
 * @see Message
 */
public class Node {
    
    /** The unique name of this node */
    private String name;
    
    /** List of outgoing edges to other nodes */
    private List<Node> edges;
    
    /** The current message stored in this node (optional) */
    private Message msg;
    
    /**
     * Constructs a new Node with the specified name.
     * 
     * <p>Creates a node with an empty edge list and no stored message.
     * The node is ready to have edges added and messages assigned.</p>
     * 
     * @param name the unique identifier for this node. Should be descriptive
     *            and follow naming conventions (e.g., "TTopicName" for topics,
     *            "AAgentName" for agents). Cannot be null.
     * 
     * @throws NullPointerException if name is null
     * 
     * @example
     * <pre>{@code
     * // Create different types of nodes
     * Node inputTopic = new Node("TUserInput");
     * Node mathAgent = new Node("APlusAgent_1");
     * Node resultTopic = new Node("TCalculationResult");
     * 
     * // Names should be unique within the graph
     * Node duplicateName = new Node("TUserInput"); // Avoid this!
     * }</pre>
     */
    public Node(String name) {
        if (name == null) {
            throw new NullPointerException("Node name cannot be null");
        }
        this.name = name;
        this.edges = new ArrayList<>();
        this.msg = null;
    }
    
    /**
     * Returns the name of this node.
     * 
     * @return the node's name, never null
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets the name of this node.
     * 
     * <p><strong>Warning:</strong> Changing a node's name after it has been
     * added to a graph may cause inconsistencies in graph algorithms or
     * visualization systems that rely on stable node identifiers.</p>
     * 
     * @param name the new name for this node, cannot be null
     * 
     * @throws NullPointerException if name is null
     * 
     * @see #getName()
     */
    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("Node name cannot be null");
        }
        this.name = name;
    }
    
    /**
     * Returns the list of outgoing edges from this node.
     * 
     * <p>The returned list is the actual internal list used by the node.
     * Modifications to this list will affect the node's edge structure.
     * For safe read-only access, consider creating a copy of the returned list.</p>
     * 
     * <p><strong>Direct Modification Example:</strong></p>
     * <pre>{@code
     * Node node = new Node("Example");
     * List<Node> edges = node.getEdges();
     * 
     * // Direct modification (affects the node)
     * edges.add(otherNode);        // Adds edge to node
     * edges.remove(someNode);      // Removes edge from node
     * 
     * // Safe read-only access
     * List<Node> edgesCopy = new ArrayList<>(node.getEdges());
     * }</pre>
     * 
     * @return the mutable list of nodes that this node has edges to.
     *         Never null, but may be empty.
     * 
     * @see #addEdge(Node)
     * @see #setEdges(List)
     */
    public List<Node> getEdges() {
        return this.edges;
    }
    
    /**
     * Replaces the entire edge list of this node.
     * 
     * <p>This method completely replaces the current edge list with a new one.
     * Use this method when you need to rebuild the node's connections entirely,
     * rather than adding individual edges.</p>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Rebuilding graph structure from serialized data</li>
     *   <li>Implementing graph transformation algorithms</li>
     *   <li>Clearing all edges (by passing an empty list)</li>
     *   <li>Batch edge assignment for performance</li>
     * </ul>
     * 
     * @param edges the new list of edges for this node. A copy of this list
     *             is stored internally. Can be null (treated as empty list).
     * 
     * @example
     * <pre>{@code
     * Node centralNode = new Node("Central");
     * 
     * // Create a list of nodes to connect to
     * List<Node> newConnections = Arrays.asList(
     *     new Node("Target1"),
     *     new Node("Target2"), 
     *     new Node("Target3")
     * );
     * 
     * // Replace all edges at once
     * centralNode.setEdges(newConnections);
     * 
     * // Clear all edges
     * centralNode.setEdges(new ArrayList<>());
     * // or
     * centralNode.setEdges(null);
     * }</pre>
     * 
     * @see #getEdges()
     * @see #addEdge(Node)
     */
    public void setEdges(List<Node> edges) {
        if (edges == null) {
            this.edges = new ArrayList<>();
        } else {
            this.edges = new ArrayList<>(edges); // Create defensive copy
        }
    }
    
    /**
     * Returns the current message stored in this node.
     * 
     * <p>Messages are typically stored in topic nodes to maintain the latest
     * published value. Agent nodes usually don't store messages as they
     * process data in a stateless manner.</p>
     * 
     * @return the current message, or null if no message is stored
     * 
     * @see #setMessage(Message)
     */
    public Message getmessage() {
        return this.msg;
    }
    
    /**
     * Sets the current message for this node.
     * 
     * <p>This method is typically used to store the latest value in topic nodes
     * for visualization, debugging, or state tracking purposes. The message
     * represents the current data value flowing through this node.</p>
     * 
     * <p><strong>Typical Usage Patterns:</strong></p>
     * <ul>
     *   <li><strong>Topic Nodes:</strong> Store latest published value</li>
     *   <li><strong>Agent Nodes:</strong> Usually remain null (stateless processing)</li>
     *   <li><strong>Debugging:</strong> Track intermediate values in complex graphs</li>
     *   <li><strong>Visualization:</strong> Display current node values in UI</li>
     * </ul>
     * 
     * @param message the message to store, or null to clear the stored message
     * 
     * @example
     * <pre>{@code
     * Node topicNode = new Node("TTemperature");
     * 
     * // Store a temperature reading
     * Message tempReading = new Message(23.5);
     * topicNode.setMessage(tempReading);
     * 
     * // Later retrieve for display
     * Message current = topicNode.getmessage();
     * if (current != null) {
     *     System.out.println("Current temperature: " + current.asDouble + "°C");
     * }
     * 
     * // Clear the stored value
     * topicNode.setMessage(null);
     * }</pre>
     * 
     * @see #getmessage()
     * @see Message
     */
    public void setMessage(Message message) {
        this.msg = message;
    }
    
    /**
     * Adds a directed edge from this node to the specified target node.
     * 
     * <p>This method creates a one-way connection from this node to the target,
     * representing data flow or dependency relationships. Duplicate edges to
     * the same target are automatically prevented.</p>
     * 
     * <p><strong>Edge Meaning:</strong></p>
     * <ul>
     *   <li><strong>Topic → Agent:</strong> Topic publishes data to subscribing agent</li>
     *   <li><strong>Agent → Topic:</strong> Agent publishes result to output topic</li>
     * </ul>
     * 
     * <p><strong>Duplicate Prevention:</strong></p>
     * <p>The method automatically checks for existing edges to prevent duplicates.
     * This ensures graph integrity and prevents redundant connections.</p>
     * 
     * @param node the target node to create an edge to. Cannot be null.
     *            Self-edges (node pointing to itself) are allowed but should
     *            be used carefully as they can create trivial cycles.
     * 
     * @throws NullPointerException if node is null
     * 
     * @example
     * <pre>{@code
     * // Create a simple data flow: Input → Processor → Output
     * Node input = new Node("TInput");
     * Node processor = new Node("AProcessor");
     * Node output = new Node("TOutput");
     * 
     * // Add edges to represent data flow
     * input.addEdge(processor);     
     * processor.addEdge(output);    
     * 
     * // Attempting to add duplicate edge has no effect
     * input.addEdge(processor);     // No change - edge already exists
     * 
     * // Self-edge (creates immediate cycle)
     * Node recursive = new Node("ARecursive");
     * recursive.addEdge(recursive); // Caution: creates cycle!
     * }</pre>
     * 
     * @see #getEdges()
     * @see List#contains(Object)
     */
    public void addEdge(Node node) {
        if (node == null) {
            throw new NullPointerException("Target node cannot be null");
        }
        
        // Prevent duplicate edges to the same node
        if (!this.edges.contains(node)) {
            this.edges.add(node);
        }
    }
    
    /**
     * Detects whether this node is part of any cycle in the graph.
     * 
     * <p>This method performs a depth-first search starting from this node
     * to detect any cycles in the reachable portion of the graph. A cycle
     * exists when there is a path from this node back to itself following
     * the directed edges.</p>
     * 
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * // Linear graph (no cycles)
     * Node a = new Node("A");
     * Node b = new Node("B");
     * Node c = new Node("C");
     * a.addEdge(b);
     * b.addEdge(c);
     * boolean hasCycle = a.hasCycles(); // false
     * 
     * // Simple cycle
     * Node x = new Node("X");
     * Node y = new Node("Y");
     * x.addEdge(y);
     * y.addEdge(x); // Creates X → Y → X cycle
     * boolean hasCycle = x.hasCycles(); // true
     * }</pre>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Only detects cycles reachable from this node</li>
     *   <li>For complete graph cycle detection, check all nodes</li>
     *   <li>Multiple calls are safe but may be inefficient for large graphs</li>
     *   <li>The algorithm handles disconnected components correctly</li>
     * </ul>
     * 
     * @return true if this node is part of any cycle, false if no cycles
     *         are detected in the reachable portion of the graph
     * 
     * @see #recursiveCycleChecker(Node, Set, Set)
     * @see Graph#hasCycles()
     */
    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();     
        Set<Node> currentPath = new HashSet<>();  
        return recursiveCycleChecker(this, visited, currentPath);
    }
    
    /**
     * Recursive helper method for cycle detection using depth-first search.
     * 
     * <p>This method implements the core cycle detection algorithm using a
     * modified DFS that tracks both visited nodes and the current path.
     * It's designed to detect back edges, which indicate the presence of cycles.</p>
     * 
     * <p><strong>Algorithm Details:</strong></p>
     * <ol>
     *   <li><strong>Cycle Check:</strong> If current node is in currentPath, we found a cycle</li>
     *   <li><strong>Visited Check:</strong> If already processed, skip to avoid redundant work</li>
     *   <li><strong>Path Tracking:</strong> Add current node to both visited and currentPath</li>
     *   <li><strong>Recursive Exploration:</strong> Check all neighboring nodes</li>
     *   <li><strong>Backtrack:</strong> Remove current node from currentPath when done</li>
     * </ol>
     * 
     * <p><strong>State Management:</strong></p>
     * <ul>
     *   <li><strong>visited:</strong> Tracks all nodes that have been completely processed</li>
     *   <li><strong>currentPath:</strong> Tracks nodes in the current DFS path</li>
     * </ul>
     * 
     * <p><strong>Termination Conditions:</strong></p>
     * <ul>
     *   <li><strong>Cycle Found:</strong> Returns true immediately</li>
     *   <li><strong>Already Visited:</strong> Returns false (safe to skip)</li>
     *   <li><strong>No More Edges:</strong> Returns false (end of path)</li>
     * </ul>
     * 
     * @param node the current node being examined in the DFS traversal
     * @param visited set of nodes that have been completely processed 
     *               These nodes and their subgraphs have been fully explored
     * @param currentPath set of nodes in the current DFS path 
     *                   These represent the chain of ancestors from the root
     *                   to the current node
     * 
     * @return true if a cycle is detected starting from the given node,
     *         false if no cycle is found in this branch of exploration
     * 
     * @throws StackOverflowError if the graph is extremely deep, though this
     *                           is mitigated by the visited set optimization
     * 
     * @see #hasCycles()
     */
    private boolean recursiveCycleChecker(Node node, Set<Node> visited, Set<Node> currentPath) {
        // If we encounter a node that's in our current path, we found a cycle
        if (currentPath.contains(node)) {
            return true;
        }
        
        // ALready processed this node,don't check again
        if (visited.contains(node)) {
            return false;
        }
        
        // Mark this node as visited and add to current path
        visited.add(node);
        currentPath.add(node);
        
        // Recursively check all neighbors
        for (Node neighbor : node.getEdges()) {
            if (recursiveCycleChecker(neighbor, visited, currentPath)) {
                return true; // Cycle found in recursion
            }
        }
        
        // Backtrack: remove from current path 
        currentPath.remove(node);
        return false;
    }
}