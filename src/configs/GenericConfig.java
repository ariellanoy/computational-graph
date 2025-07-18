package configs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import graph.Agent;
import graph.ParallelAgent;

/**
 * A generic configuration loader that creates computational graph agents from configuration files.
 * 
 * <p>This class implements the {@link Config} interface and provides functionality to load
 * agent configurations from text files. Each configuration file defines a series of agents
 * with their input/output topic connections, allowing for the creation of complex computational
 * graphs.</p>
 * 
 * <p><strong>Configuration File Format:</strong></p>
 * <p>The configuration file must follow a strict 3-line pattern for each agent:</p>
 * <pre>
 * Line 1: Fully qualified class name (e.g., "configs.PlusAgent")
 * Line 2: Comma-separated input topic names (e.g., "A,B" or "InputTopic")
 * Line 3: Comma-separated output topic names (e.g., "Result" or "Output1,Output2")
 * </pre>
 * 
 * <p><strong>Example Configuration File:</strong></p>
 * <pre>{@code
 * configs.PlusAgent
 * A,B
 * Sum
 * configs.IncAgent
 * Sum
 * Result
 * configs.BinOpAgent
 * Result,C
 * FinalOutput
 * }</pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create and configure the generic config
 * GenericConfig config = new GenericConfig();
 * config.setConfFile("path/to/config.conf");
 * 
 * try {
 *     // Load the configuration and create agents
 *     config.create();
 *     
 *     // Configuration is now active
 *     System.out.println("Configuration loaded: " + config.getName());
 *     
 *     // ... use the computational graph ...
 *     
 * } finally {
 *     // Always clean up resources
 *     config.close();
 * }
 * }</pre>
 * 
 * <p><strong>Agent Requirements:</strong></p>
 * <p>All agent classes referenced in the configuration file must:</p>
 * <ul>
 *   <li>Implement the {@link Agent} interface</li>
 *   <li>Have a constructor that accepts {@code (String[] subs, String[] pubs)}</li>
 *   <li>Be accessible via the current classpath</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. It should be used
 * from a single thread or with external synchronization.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Config
 * @see Agent
 * @see ParallelAgent
 */
public class GenericConfig implements Config {

    /** The path to the configuration file */
    private String confFile;
    
    /** List of parallel agents created from the configuration */
    private List<ParallelAgent> agents;
    
    /**
     * Constructs a new GenericConfig instance.
     * 
     * <p>The configuration file must be set using {@link #setConfFile(String)}
     * before calling {@link #create()}.</p>
     */
    public GenericConfig() {
        this.agents = new ArrayList<>();
    }
    
    /**
     * Sets the path to the configuration file.
     * 
     * <p>This method must be called before {@link #create()} to specify
     * which configuration file should be loaded.</p>
     * 
     * @param confFile the path to the configuration file. The file must exist
     *                and be readable, following the required 3-line format
     *                for each agent definition
     * 
     * @throws NullPointerException if confFile is null
     * 
     * @see #create()
     */
    public void setConfFile(String confFile) {
        if (confFile == null) {
            throw new NullPointerException("Configuration file path cannot be null");
        }
        this.confFile = confFile;
    }
    
    /**
     * Loads the configuration file and creates all specified agents.
     * 
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Reads and validates the configuration file format</li>
     *   <li>Parses agent definitions (class name, inputs, outputs)</li>
     *   <li>Creates agent instances using reflection</li>
     *   <li>Wraps each agent in a {@link ParallelAgent} for thread safety</li>
     *   <li>Stores all created agents for later cleanup</li>
     * </ol>
     * 
     * <p><strong>File Format Validation:</strong></p>
     * <ul>
     *   <li>Empty lines are ignored</li>
     *   <li>Total number of non-empty lines must be divisible by 3</li>
     *   <li>Each group of 3 lines defines one agent</li>
     * </ul>
     * 
     * <p><strong>Agent Creation Process:</strong></p>
     * <ul>
     *   <li>Uses reflection to load the specified class</li>
     *   <li>Looks for a constructor with signature {@code (String[], String[])}</li>
     *   <li>Creates the agent with parsed input/output topic arrays</li>
     *   <li>Wraps in ParallelAgent with queue capacity of 10</li>
     * </ul>
     * 
     * @throws RuntimeException if any of the following occurs:
     *         <ul>
     *           <li>Configuration file is null (call {@link #setConfFile(String)} first)</li>
     *           <li>Configuration file cannot be read</li>
     *           <li>File format is invalid (not divisible by 3 lines)</li>
     *           <li>Agent class cannot be found or loaded</li>
     *           <li>Agent class doesn't have required constructor</li>
     *           <li>Agent instantiation fails</li>
     *         </ul>
     * 
     * @see #setConfFile(String)
     * @see ParallelAgent#ParallelAgent(Agent, int)
     */
    @Override
    public void create() {
        if (confFile == null) {
            throw new RuntimeException("Configuration file is null. Call setConfFile() first.");
        }
        
        try {
            List<String> lines = readAllLines(confFile);
            List<String> infoLines = new ArrayList<>();
            
            // Filter out empty lines
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    infoLines.add(line);
                }
            }
            
            // Validate file format - must have groups of 3 lines
            if (infoLines.size() % 3 != 0) {
                throw new RuntimeException(
                    "Invalid configuration file format. Expected groups of 3 lines " +
                    "(class name, inputs, outputs), but found " + infoLines.size() + " lines. " +
                    "Total lines must be divisible by 3."
                );
            }
            
            // Process each group of 3 lines to create agents
            for (int i = 0; i < infoLines.size(); i += 3) {
                String className = infoLines.get(i).trim();
                String subsLine = infoLines.get(i + 1).trim();
                String pubsLine = infoLines.get(i + 2).trim();
                
                String[] subs = parseLine(subsLine);
                String[] pubs = parseLine(pubsLine);
                
                Agent agent = createAgentInstance(className, subs, pubs);
                ParallelAgent parallelAgent = new ParallelAgent(agent, 10);
                agents.add(parallelAgent);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Error reading configuration file '" + confFile + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating configuration from file '" + confFile + "': " + e.getMessage(), e);
        }
    }
    
    /**
     * Reads all lines from the specified file.
     * 
     * @param filename the path to the file to read
     * @return a list containing all lines from the file
     * @throws IOException if the file cannot be read
     */
    private List<String> readAllLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    
    /**
     * Parses a comma-separated line into an array of topic names.
     * 
     * <p>This method handles the parsing of input and output topic specifications
     * from the configuration file. It splits on commas and trims whitespace
     * from each topic name.</p>
     * 
     * @param line the line to parse, containing comma-separated topic names
     * @return an array of topic names, or empty array if line is empty.
     *         Each element is trimmed of leading/trailing whitespace
     * 
     * @example
     * <pre>{@code
     * parseLine("A,B,C")     // returns ["A", "B", "C"]
     * parseLine("Topic1")    // returns ["Topic1"]  
     * parseLine("")          // returns []
     * parseLine(" A , B ")   // returns ["A", "B"]
     * }</pre>
     */
    private String[] parseLine(String line) {
        if (line.trim().isEmpty()) {
            return new String[0];
        }
        String[] parts = line.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;    
    }
    
    /**
     * Creates an agent instance using reflection.
     * 
     * <p>This method uses Java reflection to dynamically instantiate agent classes
     * specified in the configuration file. It looks for a constructor that accepts
     * two String arrays for subscription and publication topic names.</p>
     * 
     * @param className the fully qualified class name of the agent to create
     * @param subs array of input topic names the agent should subscribe to
     * @param pubs array of output topic names the agent should publish to
     * @return a new instance of the specified agent class
     * 
     * @throws Exception if any of the following occurs:
     *         <ul>
     *           <li>Class cannot be found ({@link ClassNotFoundException})</li>
     *           <li>Required constructor not found ({@link NoSuchMethodException})</li>
     *           <li>Constructor cannot be accessed ({@link IllegalAccessException})</li>
     *           <li>Agent instantiation fails ({@link java.lang.reflect.InvocationTargetException})</li>
     *           <li>Class is not an Agent ({@link ClassCastException})</li>
     *         </ul>
     * 
     * @see Class#forName(String)
     * @see Class#getConstructor(Class...)
     * @see Constructor#newInstance(Object...)
     */
    private Agent createAgentInstance(String className, String[] subs, String[] pubs) throws Exception {
        try {
            Class<?> agentClass = Class.forName(className);
            Constructor<?> constructor = agentClass.getConstructor(String[].class, String[].class);
            Object instance = constructor.newInstance(subs, pubs);
            
            if (!(instance instanceof Agent)) {
                throw new ClassCastException("Class " + className + " does not implement Agent interface");
            }
            
            return (Agent) instance;
        } catch (ClassNotFoundException e) {
            throw new Exception("Agent class not found: " + className + 
                              ". Make sure the class is in the classpath.", e);
        } catch (NoSuchMethodException e) {
            throw new Exception("Agent class " + className + 
                              " must have a constructor with signature (String[], String[])", e);
        } catch (Exception e) {
            throw new Exception("Failed to create agent instance of class " + className + ": " + 
                              e.getMessage(), e);
        }
    }

    /**
     * Returns the name of this configuration.
     * 
     * @return the string "GenericConfig"
     */
    @Override
    public String getName() {
        return "GenericConfig";
    }

    /**
     * Returns the version of this configuration implementation.
     * 
     * @return the version number (currently 1)
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Closes the configuration and cleans up all created agents.
     * 
     * <p>This method performs the following cleanup operations:</p>
     * <ul>
     *   <li>Calls {@link ParallelAgent#close()} on each created agent</li>
     *   <li>Clears the internal agent list</li>
     *   <li>Releases all resources held by the configuration</li>
     * </ul>
     * 
     * <p><strong>Important:</strong> After calling this method, the configuration
     * should not be used anymore. To reuse the configuration, create a new instance
     * or call {@link #create()} again (which will create new agents).</p>
     * 
     * <p><strong>Thread Safety:</strong> This method will wait for all agent
     * threads to terminate gracefully before returning.</p>
     * 
     * @see ParallelAgent#close()
     */
    @Override
    public void close() {
        // Close all created agents
        for (ParallelAgent agent : agents) {
            try {
                agent.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing agent " + agent.getName() + ": " + e.getMessage());
            }
        }
        agents.clear();
    }
}