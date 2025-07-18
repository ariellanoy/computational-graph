package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import configs.Config;
import configs.GenericConfig;
import configs.Graph;
import graph.TopicManagerSingleton;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

/**
 * Servlet responsible for handling configuration file uploads and creating computational graphs.
 * This servlet processes multipart form data containing configuration files, parses them,
 * creates the corresponding computational graph, and returns an HTML visualization.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Extract configuration files from multipart HTTP requests</li>
 *   <li>Parse and validate configuration file format</li>
 *   <li>Create computational graphs using GenericConfig</li>
 *   <li>Generate HTML visualizations of the created graphs</li>
 *   <li>Handle errors gracefully with informative error pages</li>
 *   <li>Manage configuration lifecycle (create/close)</li>
 * </ul>
 * 
 * <p>Expected file format:
 * Configuration files should contain groups of exactly 3 lines:
 * <ol>
 *   <li>Fully qualified class name</li>
 *   <li>Comma-separated input topics</li>
 *   <li>Comma-separated output topics</li>
 * </ol>
 * 
 * <p>Example configuration:
 * <pre>
 * configs.PlusAgent
 * A,B
 * Sum
 * configs.IncAgent
 * Sum
 * Result
 * </pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class ConfLoader implements Servlet {
    
    private Config currentConfig;
    
    /**
     * {@inheritDoc}
     * 
     * <p>Processes configuration file uploads by:
     * <ol>
     *   <li>Extracting file content from multipart form data</li>
     *   <li>Validating the configuration format</li>
     *   <li>Creating and loading the configuration</li>
     *   <li>Generating graph visualization HTML</li>
     *   <li>Returning success or error response</li>
     * </ol>
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            System.out.println("=== ConfLoader: Processing configuration upload ===");
            
            // Extract file content from request
            byte[] content = ri.getContent();
            String contentStr = new String(content);
            
            System.out.println("Raw content length: " + content.length);
            
            // Extract the actual file content using improved parser
            String fileContent = extractMultipartFileContent(contentStr);
            
            if (fileContent == null || fileContent.trim().isEmpty()) {
                sendErrorResponse(toClient, "No file content found. Please make sure you selected a file.");
                return;
            }
            
            System.out.println("=== Extracted file content ===");
            System.out.println("'" + fileContent + "'");
            System.out.println("=== End of file content ===");
            
            // Validate extracted content
            validateConfigurationContent(fileContent);
            
            // Save to temp file
            Path configPath = Paths.get("temp_configs", "uploaded_config.conf");
            Files.createDirectories(configPath.getParent());
            Files.write(configPath, fileContent.getBytes());
            
            System.out.println("File saved to: " + configPath.toAbsolutePath());
            
            // Test the configuration loading
            testConfigurationLoading(configPath.toString(), toClient);
            
        } catch (Exception e) {
            System.err.println("Error in ConfLoader: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(toClient, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Extracts file content from multipart form data.
     * Parses the multipart boundaries and extracts the actual file content.
     * 
     * @param contentStr the raw multipart content
     * @return the extracted file content, or null if not found
     */
    private String extractMultipartFileContent(String contentStr) {
        System.out.println("=== Starting multipart extraction ===");
        
        String[] lines = contentStr.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        boolean inFileContent = false;
        boolean foundFilename = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            System.out.println("Processing line " + i + ": '" + line + "'");
            
            // Look for filename in Content-Disposition header
            if (line.contains("Content-Disposition:") && line.contains("filename=")) {
                foundFilename = true;
                System.out.println("Found filename line");
                continue;
            }
            
            // Skip Content-Type header
            if (line.contains("Content-Type:")) {
                System.out.println("Found Content-Type line");
                continue;
            }
            
            // After filename and content-type, look for the actual content
            if (foundFilename && line.trim().isEmpty()) {
                // Empty line after headers means content starts next
                inFileContent = true;
                System.out.println("Found empty line after headers, content starts next");
                continue;
            }
            
            if (inFileContent) {
                // Check if this is the end boundary
                if (line.startsWith("---") || line.startsWith("WebKit") || line.startsWith("Content-Disposition")) {
                    System.out.println("Found end boundary: " + line);
                    break;
                }
                
                // This is actual file content
                result.append(line).append("\n");
                System.out.println("Added content line: '" + line + "'");
            }
        }
        
        String extracted = result.toString().trim();
        System.out.println("=== Extraction complete ===");
        System.out.println("Extracted content: '" + extracted + "'");
        
        return extracted;
    }
    
    /**
     * Validates the configuration content format.
     * 
     * @param fileContent the configuration content to validate
     * @throws IllegalArgumentException if the format is invalid
     */
    private void validateConfigurationContent(String fileContent) {
        String[] lines = fileContent.split("\\n");
        System.out.println("Number of lines: " + lines.length);
        
        if (lines.length % 3 != 0) {
            throw new IllegalArgumentException("Invalid configuration file format. " +
                "File must contain groups of exactly 3 lines (class, inputs, outputs). " +
                "Found " + lines.length + " lines.");
        }
        
        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line " + i + ": '" + lines[i].trim() + "'");
        }
    }
    
    /**
     * Tests loading the configuration and creates the graph visualization.
     * 
     * @param configPath path to the configuration file
     * @param toClient output stream for the response
     * @throws IOException if there's an error with the response
     */
    private void testConfigurationLoading(String configPath, OutputStream toClient) throws IOException {
        try {
            System.out.println("=== Testing configuration loading ===");
            
            // Close previous configuration if exists
            if (currentConfig != null) {
                System.out.println("Closing previous configuration");
                currentConfig.close();
            }
            
            // Clear existing topics and message history
            System.out.println("Clearing existing topics");
            TopicManagerSingleton.get().clear();
            TopicDisplayer.clearAllMessages();
            System.out.println("Cleared all stored topic messages");
            
            // Load new configuration
            System.out.println("Creating new GenericConfig");
            GenericConfig config = new GenericConfig();
            config.setConfFile(configPath);
            
            System.out.println("About to call config.create()");
            config.create();
            
            System.out.println("Configuration created successfully!");
            currentConfig = config;
            
            // Create graph from loaded configuration
            System.out.println("Creating graph from topics");
            Graph graph = new Graph();
            graph.createFromTopics();
            
            System.out.println("Graph created with " + graph.size() + " nodes");
            
            // Check for cycles
            boolean hasCycles = graph.hasCycles();
            System.out.println("Has cycles: " + hasCycles);
            
            // Generate HTML response
            generateSuccessResponse(graph, hasCycles, toClient);
            
        } catch (Exception e) {
            System.err.println("Configuration loading failed: " + e.getMessage());
            e.printStackTrace();
            
            String fullError = "Configuration loading failed: " + e.getMessage();
            if (e.getCause() != null) {
                fullError += "\nCause: " + e.getCause().getMessage();
            }
            
            sendErrorResponse(toClient, fullError);
        }
    }
    
    /**
     * Generates a success response with graph visualization.
     * 
     * @param graph the created graph
     * @param hasCycles whether the graph contains cycles
     * @param toClient output stream for the response
     * @throws IOException if there's an error writing the response
     */
    private void generateSuccessResponse(Graph graph, boolean hasCycles, OutputStream toClient) throws IOException {
        try {
            // Generate HTML response with graph visualization
            List<String> graphHtml = HtmlGraphWriter.getGraphHTML(graph);
            StringBuilder htmlResponse = new StringBuilder();
            
            for (String line : graphHtml) {
                htmlResponse.append(line).append("\n");
            }
            
            // Add cycle warning if needed
            if (hasCycles) {
                String cycleWarning = "<div style=\"background-color: #ffebee; color: #c62828; " +
                                    "padding: 10px; margin: 10px 0; border-radius: 4px; border: 1px solid #e57373;\">" +
                                    "<strong>Warning:</strong> The computational graph contains cycles!" +
                                    "</div>";
                htmlResponse.insert(0, cycleWarning);
            }
            
            // Add success message
            String successMessage = "<div style=\"background-color: #e8f5e8; color: #2e7d32; " +
                                  "padding: 10px; margin: 10px 0; border-radius: 4px; border: 1px solid #4caf50;\">" +
                                  "<strong>Success:</strong> Configuration loaded successfully! Graph has " + graph.size() + " nodes." +
                                  "</div>";
            htmlResponse.insert(0, successMessage);
            
            // Add script to refresh topics frame
            String refreshScript = "<script>" +
                    "try {" +
                    "  if (parent && parent.frames && parent.frames['topics']) {" +
                    "    parent.frames['topics'].location.href = 'http://localhost:8080/publish';" +
                    "  }" +
                    "} catch(e) { console.log('Could not refresh topics:', e); }" +
                    "</script>";
            htmlResponse.append(refreshScript);
            
            // Send HTTP response
            String response = htmlResponse.toString();
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: " + response.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n" + response;
            
            toClient.write(httpResponse.getBytes());
            toClient.flush();
            
        } catch (Exception e) {
            System.err.println("Error generating success response: " + e.getMessage());
            sendErrorResponse(toClient, "Graph loaded but failed to generate visualization: " + e.getMessage());
        }
    }
    
    /**
     * Sends an error response with helpful debugging information.
     * 
     * @param toClient output stream for the response
     * @param errorMessage the error message to display
     * @throws IOException if there's an error writing the response
     */
    private void sendErrorResponse(OutputStream toClient, String errorMessage) throws IOException {
        String errorHtml = "<!DOCTYPE html>\n" +
                          "<html><head><title>Configuration Error</title></head>\n" +
                          "<body style=\"font-family: Arial, sans-serif; padding: 20px;\">\n" +
                          "<h2 style=\"color: #d32f2f;\">Configuration Load Error</h2>\n" +
                          "<div style=\"background: #ffebee; padding: 15px; border-radius: 4px; margin: 20px 0;\">\n" +
                          "<pre style=\"white-space: pre-wrap; color: #c62828;\">" + escapeHtml(errorMessage) + "</pre>\n" +
                          "</div>\n" +
                          "<h3>Configuration File Format:</h3>\n" +
                          "<p>The configuration file should have groups of exactly 3 lines:</p>\n" +
                          "<pre style=\"background: #f5f5f5; padding: 10px; border-radius: 4px;\">" +
                          "configs.ClassName\n" +
                          "input1,input2\n" +
                          "output1,output2\n" +
                          "</pre>\n" +
                          "<h4>Working Example:</h4>\n" +
                          "<pre style=\"background: #e8f5e8; padding: 10px; border-radius: 4px;\">" +
                          "configs.PlusAgent\n" +
                          "A,B\n" +
                          "R1\n" +
                          "configs.IncAgent\n" +
                          "R1\n" +
                          "R2\n" +
                          "</pre>\n" +
                          "<button onclick=\"history.back()\" style=\"margin-top: 20px; padding: 10px 20px; background: #2196f3; color: white; border: none; border-radius: 4px; cursor: pointer;\">Go Back</button>\n" +
                          "</body></html>";
        
        String httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                             "Content-Type: text/html\r\n" +
                             "Content-Length: " + errorHtml.length() + "\r\n" +
                             "Connection: close\r\n" +
                             "\r\n" + errorHtml;
        
        toClient.write(httpResponse.getBytes());
        toClient.flush();
    }
    
    /**
     * Escapes HTML special characters to prevent XSS attacks.
     * 
     * @param text the text to escape, may be null
     * @return HTML-safe text
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (currentConfig != null) {
            currentConfig.close();
        }
    }
}