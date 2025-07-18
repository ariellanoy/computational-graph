package configs;

/**
 * A demonstration configuration that creates a mathematical computational graph.
 * 
 * <p>This class serves as an example implementation of the {@link Config} interface,
 * showing how to programmatically create a computational graph using binary operation
 * agents. The configuration demonstrates mathematical operations and data flow patterns
 * commonly used in computational systems.</p>
 * 
 * <p><strong>Mathematical Expression:</strong></p>
 * <p>The configuration implements the mathematical expression:</p>
 * <pre>
 * R3 = (A + B) * (A - B)
 * </pre>
 * <p>Which is mathematically equivalent to:</p>
 * <pre>
 * R3 = A² - B²
 * </pre>
 * 
 * <p><strong>Agent Configuration:</strong></p>
 * <ul>
 *   <li><strong>Agent 1 ("plus"):</strong> Adds topics A and B, outputs to R1</li>
 *   <li><strong>Agent 2 ("minus"):</strong> Subtracts B from A, outputs to R2</li>
 *   <li><strong>Agent 3 ("mul"):</strong> Multiplies R1 and R2, outputs to R3</li>
 * </ul>
 * 
 * <p><strong>Topic Flow:</strong></p>
 * <ol>
 *   <li><strong>Input Topics:</strong> A, B (external data sources)</li>
 *   <li><strong>Intermediate Topics:</strong> R1 (A+B), R2 (A-B)</li>
 *   <li><strong>Output Topic:</strong> R3 (final result)</li>
 * </ol>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create and deploy the mathematical configuration
 * MathExampleConfig config = new MathExampleConfig();
 * config.create();
 * 
 * // Now, the computational graph is active and ready to process data
 * System.out.println("Configuration: " + config.getName());
 * System.out.println("Version: " + config.getVersion());
 * 
 * // Send test data to the graph
 * TopicManager tm = TopicManagerSingleton.get();
 * 
 * // Clean up when done
 * config.close();
 * }</pre>
 * 
 * <p><strong>Educational Value:</strong></p>
 * <p>This configuration demonstrates several important concepts:</p>
 * <ul>
 *   <li><strong>Data Flow:</strong> How values flow through a computational graph</li>
 *   <li><strong>Parallel Processing:</strong> Independent operations (+ and -) can execute concurrently</li>
 *   <li><strong>Dependency Management:</strong> The multiplication waits for both inputs</li>
 *   <li><strong>Algebraic Optimization:</strong> Complex expressions can be decomposed</li>
 *   <li><strong>Agent Reuse:</strong> BinOpAgent can perform different operations</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> When used with {@link graph.ParallelAgent} wrappers,
 * this configuration creates a thread-safe computational graph suitable for
 * concurrent data processing.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Config
 * @see BinOpAgent
 * @see graph.TopicManagerSingleton
 */
public class MathExampleConfig implements Config {
    
    /**
     * Creates the mathematical computational graph.
     * 
     * <p>This method instantiates three {@link BinOpAgent} instances to create
     * a computational graph that evaluates the expression (A + B) * (A - B).
     * Each agent is configured with specific input topics, output topics, and
     * mathematical operations.</p>
     * 
     * <p><strong>Agent Creation Sequence:</strong></p>
     * <ol>
     *   <li><strong>Addition Agent:</strong>
     *       <ul>
     *         <li>Name: "plus"</li>
     *         <li>Inputs: topics "A" and "B"</li>
     *         <li>Output: topic "R1"</li>
     *         <li>Operation: (x, y) → x + y</li>
     *       </ul>
     *   </li>
     *   <li><strong>Subtraction Agent:</strong>
     *       <ul>
     *         <li>Name: "minus"</li>
     *         <li>Inputs: topics "A" and "B"</li>
     *         <li>Output: topic "R2"</li>
     *         <li>Operation: (x, y) → x - y</li>
     *       </ul>
     *   </li>
     *   <li><strong>Multiplication Agent:</strong>
     *       <ul>
     *         <li>Name: "mul"</li>
     *         <li>Inputs: topics "R1" and "R2"</li>
     *         <li>Output: topic "R3"</li>
     *         <li>Operation: (x, y) → x * y</li>
     *       </ul>
     *   </li>
     * </ol>
     * 
     * <p><strong>Data Flow Analysis:</strong></p>
     * <p>The configuration creates a two-stage pipeline:</p>
     * <ul>
     *   <li><strong>Stage 1 (Parallel):</strong> Addition and subtraction operations
     *       execute independently and can run concurrently</li>
     *   <li><strong>Stage 2 (Sequential):</strong> Multiplication waits for both
     *       R1 and R2 to be available before proceeding</li>
     * </ul>
     * 
     * <p><strong>Topic Registration:</strong></p>
     * <p>The following topics are automatically created and registered:</p>
     * <ul>
     *   <li><strong>Input Topics:</strong> "A", "B" (for external data injection)</li>
     *   <li><strong>Intermediate Topics:</strong> "R1", "R2" (for pipeline communication)</li>
     *   <li><strong>Output Topic:</strong> "R3" (for final result extraction)</li>
     * </ul>
     * 
     * <p><strong>Lambda Expression Usage:</strong></p>
     * <p>The method demonstrates modern Java functional programming patterns:</p>
     * <pre>{@code
     * // These lambda expressions are equivalent to:
     * BinaryOperator<Double> addOp = new BinaryOperator<Double>() {
     *     public Double apply(Double x, Double y) { return x + y; }
     * };
     * BinaryOperator<Double> subOp = new BinaryOperator<Double>() {
     *     public Double apply(Double x, Double y) { return x - y; }
     * };
     * BinaryOperator<Double> mulOp = new BinaryOperator<Double>() {
     *     public Double apply(Double x, Double y) { return x * y; }
     * };
     * }</pre>
     * 
     * <p><strong>Execution Timeline Example:</strong></p>
     * <pre>
     * Time | Event
     * -----|------------------------------------------------------------
     * T0   | A=5.0 published → plus and minus agents receive input
     * T1   | B=3.0 published → plus and minus agents have both inputs
     * T2   | plus computes: 5.0 + 3.0 = 8.0 → publishes to R1
     * T2   | minus computes: 5.0 - 3.0 = 2.0 → publishes to R2  
     * T3   | mul receives R1=8.0 and R2=2.0 → computes 8.0 * 2.0 = 16.0
     * T4   | Final result R3=16.0 is available
     * </pre>
     * 
     * <p><strong>Memory and Resource Usage:</strong></p>
     * <ul>
     *   <li>Creates 3 agent instances</li>
     *   <li>Registers 5 topics in the topic manager</li>
     *   <li>No persistent state maintained between calculations</li>
     *   <li>Suitable for high-frequency data processing</li>
     * </ul>
     * 
     * @throws RuntimeException if agent creation fails due to:
     *         <ul>
     *           <li>Topic manager not available</li>
     *           <li>Memory allocation failures</li>
     *           <li>Topic registration conflicts</li>
     *         </ul>
     * 
     * @see BinOpAgent#BinOpAgent(String, String, String, String, java.util.function.BinaryOperator)
     * @see java.util.function.BinaryOperator
     */
    @Override
    public void create() {
        // Create addition agent: R1 = A + B
        new BinOpAgent("plus", "A", "B", "R1", (x, y) -> x + y);
        
        // Create subtraction agent: R2 = A - B  
        new BinOpAgent("minus", "A", "B", "R2", (x, y) -> x - y);
        
        // Create multiplication agent: R3 = R1 * R2 = (A + B) * (A - B)
        new BinOpAgent("mul", "R1", "R2", "R3", (x, y) -> x * y);
    }
    
    /**
     * Returns the human-readable name of this configuration.
     * 
     * <p>This name is used for identification purposes in logging, debugging,
     * and user interfaces. It provides a clear description of what this
     * configuration implements.</p>
     * 
     * @return the string "Math Example" representing this configuration
     */
    @Override
    public String getName() {
        return "Math Example";
    }
    
    /**
     * Returns the version number of this configuration implementation.
     * 
     * <p>Version numbers help track configuration evolution and ensure
     * compatibility with different system versions. This can be useful
     * for:</p>
     * <ul>
     *   <li>Configuration migration and upgrades</li>
     *   <li>Compatibility checking</li>
     *   <li>Bug tracking and debugging</li>
     *   <li>Feature availability detection</li>
     * </ul>
     * 
     * @return the version number (currently 1)
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Cleans up resources used by this configuration.
     * 
     * <p>Since this configuration uses the programmatic approach with
     * {@link BinOpAgent} instances that manage their own lifecycle,
     * no explicit cleanup is required at the configuration level.</p>
     * 
     * <p><strong>Cleanup Responsibility:</strong></p>
     * <ul>
     *   <li><strong>Agent Cleanup:</strong> Each BinOpAgent handles its own
     *       unsubscription and resource release when the application shuts down</li>
     *   <li><strong>Topic Cleanup:</strong> Topics are managed by the singleton
     *       TopicManager and persist until explicitly cleared</li>
     *   <li><strong>Memory Cleanup:</strong> Java garbage collection handles
     *       memory deallocation for unreferenced agents</li>
     * </ul>
     * 
     * <p><strong>Alternative Cleanup Approaches:</strong></p>
     * <p>If explicit cleanup were needed, it could be implemented as:</p>
     * <pre>{@code
     * // Example of explicit agent tracking and cleanup:
     * private List<BinOpAgent> createdAgents = new ArrayList<>();
     * 
     * public void create() {
     *     createdAgents.add(new BinOpAgent("plus", "A", "B", "R1", (x, y) -> x + y));
     *     // ... other agents
     * }
     * 
     * public void close() {
     *     for (BinOpAgent agent : createdAgents) {
     *         agent.close();
     *     }
     *     createdAgents.clear();
     * }
     * }</pre>
     * 
     * <p><strong>Comparison with GenericConfig:</strong></p>
     * <p>Unlike {@link GenericConfig} which explicitly tracks and closes
     * {@link graph.ParallelAgent} wrappers, this configuration relies on
     * the natural lifecycle of the created agents.</p>
     * 
     * @see BinOpAgent#close()
     * @see graph.TopicManagerSingleton.TopicManager#clear()
     */
    @Override
    public void close() {
        // No explicit cleanup required for this simple configuration
        // BinOpAgent instances will be garbage collected when no longer referenced
        // Topics remain in TopicManager until explicitly cleared
    }
}