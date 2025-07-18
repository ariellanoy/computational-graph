package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import graph.TopicManagerSingleton.TopicManager;
import server.RequestParser.RequestInfo;

/**
 * Servlet responsible for displaying current topic values and handling message publishing.
 * This servlet provides a web interface for monitoring topic states and sending new messages
 * to topics in the computational graph system.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Displays current values of all topics in an HTML table</li>
 *   <li>Handles message publishing via GET requests with parameters</li>
 *   <li>Maintains history of last messages for each topic</li>
 *   <li>Provides real-time updates when new messages are sent</li>
 *   <li>Thread-safe message storage using ConcurrentHashMap</li>
 * </ul>
 * 
 * <p>URL Parameters:
 * <ul>
 *   <li>topic - the name of the topic to publish to</li>
 *   <li>message - the message value to publish</li>
 * </ul>
 * 
 * <p>Example URL: /publish?topic=temperature&message=25.5
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class TopicDisplayer implements Servlet {
    
    /** Static map to store the last message for each topic */
    private static final ConcurrentHashMap<String, Message> lastMessages = new ConcurrentHashMap<>();
    
    /**
     * Updates the stored last message for a topic. This method is static
     * to allow other components to update topic values.
     * 
     * @param topicName the name of the topic, not null
     * @param message the new message for the topic, not null
     */
    public static void updateLastMessage(String topicName, Message message) {
        if (topicName != null && message != null) {
            lastMessages.put(topicName, message);
        }
    }
    
    /**
     * Retrieves the last message for a specific topic.
     * 
     * @param topicName the name of the topic, not null
     * @return the last message, or null if no message has been stored
     */
    public static Message getLastMessage(String topicName) {
        return lastMessages.get(topicName);
    }
    
    /**
     * Clears all stored messages. This is typically used when resetting
     * the system or loading a new configuration.
     */
    public static void clearAllMessages() {
        lastMessages.clear();
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>Handles both message publishing and topic display. If topic and message
     * parameters are provided, publishes the message to the specified topic.
     * Always returns an HTML table showing current topic states.
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            // Get topic and message from parameters
            Map<String, String> params = ri.getParameters();
            String topicName = params.get("topic");
            String messageValue = params.get("message");
            
            TopicManager tm = TopicManagerSingleton.get();
            
            // If topic and message are provided, publish the message
            if (topicName != null && messageValue != null && 
                !topicName.trim().isEmpty() && !messageValue.trim().isEmpty()) {
                try {
                    Topic topic = tm.getTopic(topicName.trim());
                    Message msg = new Message(messageValue.trim());
                    
                    // Store the message before publishing
                    updateLastMessage(topicName.trim(), msg);
                    
                    // Publish message
                    topic.publish(msg);
                    
                    System.out.println("Published message '" + messageValue + "' to topic '" + topicName + "'");
                } catch (Exception e) {
                    System.err.println("Error publishing message: " + e.getMessage());
                }
            }
            
            // Generate HTML response with topics table
            String htmlResponse = generateTopicsTable(tm);
            
            // Send HTTP response
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: " + htmlResponse.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n" + htmlResponse;
            
            toClient.write(httpResponse.getBytes());
            toClient.flush();
            
        } catch (Exception e) {
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n\r\nError: " + e.getMessage();
            toClient.write(errorResponse.getBytes());
        }
    }
    
    /**
     * Generates an HTML table displaying all topics and their current values.
     * 
     * @param tm the topic manager to get topics from
     * @return complete HTML page with topics table
     */
    private String generateTopicsTable(TopicManager tm) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>Topics Table</title>\n")
            .append("    <style>\n")
            .append("        body { font-family: Arial, sans-serif; margin: 20px; }\n")
            .append("        h2 { color: #333; margin-bottom: 20px; }\n")
            .append("        table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n")
            .append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            .append("        th { background-color: #f2f2f2; font-weight: bold; }\n")
            .append("        tr:nth-child(even) { background-color: #f9f9f9; }\n")
            .append("        .topic-name { font-weight: bold; color: #2196F3; }\n")
            .append("        .topic-value { color: #4CAF50; font-weight: bold; }\n")
            .append("        .no-value { color: #999; font-style: italic; }\n")
            .append("        .updated { background-color: #e8f5e8; }\n")
            .append("        .status { font-size: 12px; color: #666; margin-bottom: 10px; }\n")
            .append("        .refresh-info { background-color: #e3f2fd; padding: 10px; border-radius: 4px; margin-bottom: 20px; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <h2>Current Topic Values</h2>\n")
            .append("    <div class=\"refresh-info\">\n")
            .append("        <strong>Note:</strong> This table updates automatically when you send new messages.\n")
            .append("    </div>\n")
            .append("    <div class=\"status\">Last updated: " + new java.util.Date() + "</div>\n")
            .append("    <table>\n")
            .append("        <tr>\n")
            .append("            <th>Topic Name</th>\n")
            .append("            <th>Current Value</th>\n")
            .append("            <th>Subscribers</th>\n")
            .append("            <th>Publishers</th>\n")
            .append("        </tr>\n");
        
        Collection<Topic> topics = tm.getTopics();
        
        if (topics.isEmpty()) {
            html.append("        <tr>\n")
                .append("            <td colspan=\"4\" style=\"text-align: center; color: #999;\">No topics available. Upload a configuration first.</td>\n")
                .append("        </tr>\n");
        } else {
            for (Topic topic : topics) {
                Message lastMessage = getLastMessage(topic.name);
                String valueDisplay = getTopicValueDisplay(lastMessage);
                String rowClass = lastMessage != null ? "updated" : "";
                
                html.append("        <tr class=\"" + rowClass + "\">\n")
                    .append("            <td class=\"topic-name\">").append(escapeHtml(topic.name)).append("</td>\n")
                    .append("            <td class=\"topic-value\">").append(valueDisplay).append("</td>\n")
                    .append("            <td>").append(topic.subs.size()).append(" agents</td>\n")
                    .append("            <td>").append(topic.pubs.size()).append(" agents</td>\n")
                    .append("        </tr>\n");
            }
        }
        
        html.append("    </table>\n")
            .append("    <div style=\"font-size: 12px; color: #666; margin-top: 20px;\">\n")
            .append("        Send a message using the form on the left to see updates here.\n")
            .append("    </div>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString();
    }
    
    /**
     * Formats a message for display in the topics table.
     * 
     * @param lastMessage the message to format, may be null
     * @return HTML-formatted string for display
     */
    private String getTopicValueDisplay(Message lastMessage) {
        if (lastMessage == null) {
            return "<span class=\"no-value\">No messages yet</span>";
        }
        
        String value = lastMessage.asText;
        if (!Double.isNaN(lastMessage.asDouble)) {
            value = String.valueOf(lastMessage.asDouble);
        }
        
        return "<span class=\"topic-value\">" + escapeHtml(value) + "</span>";
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
    }
}