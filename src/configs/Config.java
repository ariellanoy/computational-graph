package configs;

/**
 * Interface defining the contract for configuration objects in the computational graph system.
 * Configurations are responsible for creating and managing sets of agents that work together
 * to form computational graphs.
 * 
 * <p>Implementations of this interface should:
 * <ul>
 *   <li>Create the necessary agents and connect them to appropriate topics</li>
 *   <li>Provide metadata about the configuration (name and version)</li>
 *   <li>Clean up resources when the configuration is no longer needed</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Config config = new MathExampleConfig();
 * System.out.println("Loading: " + config.getName() + " v" + config.getVersion());
 * 
 * try {
 *     config.create(); // Set up the computational graph
 *     // Use the graph...
 * } finally {
 *     config.close(); // Clean up resources
 * }
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public interface Config {
    
    /**
     * Creates and initializes the computational graph by instantiating agents
     * and connecting them to appropriate topics. This method should be idempotent
     * - calling it multiple times should have the same effect as calling it once.
     * 
     * @throws RuntimeException if the configuration cannot be created due to
     *         invalid parameters, missing resources, or other errors
     */
    void create();
    
    /**
     * Returns a human-readable name for this configuration.
     * This name should be descriptive and unique among configurations.
     * 
     * @return the configuration name, never null or empty
     */
    String getName();
    
    /**
     * Returns the version number of this configuration.
     * This can be used for compatibility checking and upgrade management.
     * 
     * @return the version number, typically starting from 1
     */
    int getVersion();
    
    /**
     * Closes the configuration and releases any resources it has allocated.
     * This typically involves closing agents, unsubscribing from topics,
     * and cleaning up any temporary files or connections.
     * 
     * <p>After calling this method, the configuration should not be used again
     * without calling create() first.
     */
    void close();
}