package views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import configs.Graph;
import configs.Node;

/**
 * Utility class for generating HTML visualizations of computational graphs.
 * This class converts Graph objects into interactive HTML representations
 * suitable for web display, including nodes, edges, and dynamic positioning.
 * 
 * <p>The generated HTML includes:
 * <ul>
 *   <li>Visual distinction between topic nodes (rectangular) and agent nodes (circular)</li>
 *   <li>Dynamic positioning algorithms for optimal layout</li>
 *   <li>Interactive features like drag-and-drop (when implemented in template)</li>
 *   <li>Current values display for topic nodes</li>
 *   <li>Mathematical expression representation when possible</li>
 * </ul>
 * 
 * <p>The class uses a template-based approach where a base HTML template
 * is loaded and specific placeholders are replaced with generated content.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Graph graph = new Graph();
 * graph.createFromTopics();
 * 
 * List<String> htmlLines = HtmlGraphWriter.getGraphHTML(graph);
 * 
 * // Write to file or send as HTTP response
 * Files.write(Paths.get("graph.html"), htmlLines);
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class HtmlGraphWriter {
    
    /** Path to the HTML template file */
    private static final String GRAPH_TEMPLATE_FILE = "html_files/graph.html";
    
    /**
     * Generates HTML representation of the given graph.
     * 
     * @param graph the computational graph to visualize, not null
     * @return list of HTML lines ready for output, never null
     */
    public static List<String> getGraphHTML(Graph graph) {
        List<String> result = new ArrayList<>();
        
        try {
            // Load template file
            String template = loadTemplate();
            
            // Generate graph content
            String graphContent = generateGraphContent(graph);
            
            // Generate mathematical expression
            String expression = generateExpression(graph);
            
            // Replace placeholders in template
            String finalHtml = template
                .replace("{{GRAPH_CONTENT}}", graphContent)
                .replace("{{EXPRESSION}}", expression);
            
            // Split into lines for the List<String> return type
            String[] lines = finalHtml.split("\n");
            for (String line : lines) {
                result.add(line);
            }
            
        } catch (Exception e) {
            // Fallback to basic HTML if template loading fails
            result.add("<!DOCTYPE html>");
            result.add("<html><head><title>Graph Error</title></head>");
            result.add("<body>");
            result.add("<h2>Error generating graph visualization</h2>");
            result.add("<p>Error: " + e.getMessage() + "</p>");
            result.add("</body></html>");
        }
        
        return result;
    }
    
    /**
     * Loads the HTML template from file system.
     * 
     * @return the template content as string
     * @throws IOException if template cannot be loaded
     */
    private static String loadTemplate() throws IOException {
        Path templatePath = Paths.get(GRAPH_TEMPLATE_FILE);
        if (Files.exists(templatePath)) {
            return new String(Files.readAllBytes(templatePath));
        } else {
            return getDefaultTemplate();
        }
    }
    
    /**
     * Generates the HTML content for nodes and edges.
     * 
     * @param graph the graph to generate content for
     * @return HTML string representing the graph
     */
    private static String generateGraphContent(Graph graph) {
        StringBuilder content = new StringBuilder();
        
        // Calculate positions for nodes
        Map<Node, Position> positions = calculateNodePositions(graph);
        
        // Generate nodes HTML
        for (Node node : graph) {
            Position pos = positions.get(node);
            String nodeHtml = generateNodeHtml(node, pos);
            content.append(nodeHtml).append("\n");
        }
        
        // Generate edges HTML
        for (Node node : graph) {
            Position fromPos = positions.get(node);
            for (Node neighbor : node.getEdges()) {
                Position toPos = positions.get(neighbor);
                String edgeHtml = generateEdgeHtml(fromPos, toPos);
                content.append(edgeHtml).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * Generates HTML for a single node.
     * 
     * @param node the node to generate HTML for
     * @param pos the position of the node
     * @return HTML string for the node
     */
    private static String generateNodeHtml(Node node, Position pos) {
        String nodeName = node.getName();
        boolean isTopic = nodeName.startsWith("T");
        String nodeClass = isTopic ? "topic-node" : "agent-node";
        
        // Clean display name (remove T or A prefix)
        String displayName = nodeName.length() > 1 ? nodeName.substring(1) : nodeName;
        
        // Get current value if it's a topic
        String valueDisplay = "";
        if (isTopic && node.getmessage() != null) {
            valueDisplay = "<div class=\"value-display\">" + 
                          escapeHtml(node.getmessage().asText) + "</div>";
        }
        
        return String.format(
            "<div class=\"node %s\" style=\"position: absolute; left: %dpx; top: %dpx;\">%s%s</div>",
            nodeClass, pos.x, pos.y, escapeHtml(displayName), valueDisplay
        );
    }
    
    /**
     * Generates HTML for an edge between two nodes.
     * 
     * @param from the starting position
     * @param to the ending position
     * @return HTML string for the edge
     */
    private static String generateEdgeHtml(Position from, Position to) {
        // Calculate edge position and rotation
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx) * 180 / Math.PI;
        
        // Calculate proper starting and ending points to touch the node edges
        double normalizedDx = dx / length;
        double normalizedDy = dy / length;
        
        // Offset from node centers (different for topics vs agents)
        int fromOffsetX = 40; // Half of typical node width
        int fromOffsetY = 20; // Half of typical node height
        int toOffsetX = 40;
        int toOffsetY = 20;
        
        // Starting point (edge of source node)
        int startX = (int)(from.x + fromOffsetX + normalizedDx * fromOffsetX);
        int startY = (int)(from.y + fromOffsetY + normalizedDy * fromOffsetY);
        
        // Ending point (edge of target node)
        int endX = (int)(to.x + toOffsetX - normalizedDx * toOffsetX);
        int endY = (int)(to.y + toOffsetY - normalizedDy * toOffsetY);
        
        // Recalculate actual arrow length
        int actualDx = endX - startX;
        int actualDy = endY - startY;
        double actualLength = Math.sqrt(actualDx * actualDx + actualDy * actualDy);
        
        return String.format(
            "<div class=\"edge\" style=\"left: %dpx; top: %dpx; width: %.0fpx; transform: rotate(%.1fdeg); transform-origin: 0 50%%;\"></div>",
            startX, startY, actualLength, angle
        );
    }
    
    /**
     * Calculates optimal positions for all nodes in the graph.
     * Uses either circular layout for small graphs or grid layout for larger ones.
     * 
     * @param graph the graph to calculate positions for
     * @return map of nodes to their calculated positions
     */
    private static Map<Node, Position> calculateNodePositions(Graph graph) {
        Map<Node, Position> positions = new HashMap<>();
        
        // Get container dimensions
        int containerWidth = 600;
        int containerHeight = 500;
        int centerX = containerWidth / 2;
        int centerY = containerHeight / 2;
        int nodeCount = graph.size();
        
        if (nodeCount <= 8) {
            // Circular layout for small graphs
            int radius = Math.min(containerWidth, containerHeight) / 3;
            for (int i = 0; i < nodeCount; i++) {
                Node node = graph.get(i);
                double angle = 2 * Math.PI * i / nodeCount - Math.PI / 2;
                int x = centerX + (int)(radius * Math.cos(angle)) - 40;
                int y = centerY + (int)(radius * Math.sin(angle)) - 20;
                positions.put(node, new Position(x, y));
            }
        } else {
            // Grid layout for larger graphs
            int cols = (int)Math.ceil(Math.sqrt(nodeCount));
            int rows = (int)Math.ceil((double)nodeCount / cols);
            int spacingX = 120;
            int spacingY = 100;
            
            int totalWidth = (cols - 1) * spacingX;
            int totalHeight = (rows - 1) * spacingY;
            int startX = centerX - totalWidth / 2;
            int startY = centerY - totalHeight / 2;
            
            for (int i = 0; i < nodeCount; i++) {
                Node node = graph.get(i);
                int row = i / cols;
                int col = i % cols;
                int x = startX + col * spacingX - 40;
                int y = startY + row * spacingY - 20;
                positions.put(node, new Position(x, y));
            }
        }
        
        return positions;
    }
    
    /**
     * Generates a mathematical expression representation of the graph.
     * 
     * @param graph the graph to analyze
     * @return string representation of the mathematical expression
     */
    private static String generateExpression(Graph graph) {
        StringBuilder expression = new StringBuilder();
        boolean hasTopics = false;
        
        for (Node node : graph) {
            if (node.getName().startsWith("T")) {
                hasTopics = true;
                break;
            }
        }
        
        if (hasTopics) {
            expression.append("Computational graph with ")
                     .append(graph.size())
                     .append(" nodes");
        } else {
            expression.append("Empty graph");
        }
        
        return expression.toString();
    }
    
    /**
     * Returns a default HTML template when the template file is not found.
     * 
     * @return basic HTML template string
     */
    private static String getDefaultTemplate() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>Computational Graph</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
               "        .graph-container { width: 100%; height: 500px; border: 1px solid #ccc; position: relative; }\n" +
               "        .node { position: absolute; border: 2px solid #333; padding: 10px; background: white; }\n" +
               "        .topic-node { background-color: #e8f5e8; border-color: #4CAF50; }\n" +
               "        .agent-node { background-color: #e3f2fd; border-color: #2196F3; border-radius: 50%; }\n" +
               "        .edge { position: absolute; height: 2px; background: #333; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h2>Computational Graph</h2>\n" +
               "    <div class=\"graph-container\">\n" +
               "        {{GRAPH_CONTENT}}\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Escapes HTML special characters to prevent XSS attacks.
     * 
     * @param text the text to escape
     * @return HTML-safe text
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Simple immutable class to represent 2D positions.
     */
    private static class Position {
        final int x, y;
        
        /**
         * Creates a new position.
         * 
         * @param x the x coordinate
         * @param y the y coordinate
         */
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}