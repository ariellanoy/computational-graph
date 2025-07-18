package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import server.RequestParser.RequestInfo;

/**
 * Servlet that serves static HTML files and other web assets from a directory.
 * 
 * <p>This servlet handles requests for files and images.
 * It automatically adds .html extension if missing.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class HtmlLoader implements Servlet {
    
    /** The directory containing HTML files to serve */
    private final String htmlDirectory;
    
    /**
     * Creates a new HtmlLoader that serves files from the specified directory.
     * 
     * @param htmlDirectory the directory path containing HTML files to serve
     */
    public HtmlLoader(String htmlDirectory) {
        this.htmlDirectory = htmlDirectory;
    }
    
    /**
     * Handles HTTP requests for static files.
     * 
     * <p>Extracts the filename from the URI, loads the file from the configured
     * directory, and sends it to the client with appropriate headers.</p>
     * 
     * @param ri the parsed request information
     * @param toClient the output stream to send the response to
     * @throws IOException if file operations or response writing fails
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        try {
            System.out.println("HtmlLoader handling: " + ri.getUri());
            
            // Extract filename from URI
            String uri = ri.getUri();
            String filename = extractFilename(uri);
            
            if (filename == null || filename.trim().isEmpty()) {
                filename = "index.html";
            }
            
            System.out.println("Looking for file: " + filename);
                                  
            // Make sure that there is .html extension 
            if (!filename.contains(".")) {
                filename += ".html";
            }
            
            // Load file from html directory
            Path filePath = Paths.get(htmlDirectory, filename);
            System.out.println("Full path: " + filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                sendNotFoundResponse(toClient, filename);
                return;
            }
            
            // Read file content
            byte[] fileContent = Files.readAllBytes(filePath);
            
            // Determine content type
            String contentType = getContentType(filename);
            
            // Send HTTP response with proper headers
            String httpHeaders = "HTTP/1.1 200 OK\r\n" +
                               "Content-Type: " + contentType + "\r\n" +
                               "Content-Length: " + fileContent.length + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n";
            
            toClient.write(httpHeaders.getBytes());
            toClient.write(fileContent);
            toClient.flush();
            
            System.out.println("Served file: " + filename + " (" + fileContent.length + " bytes)");
            
        } catch (Exception e) {
            System.err.println("Error in HtmlLoader: " + e.getMessage());
            sendErrorResponse(toClient, "Error loading file: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Extracts the filename from the request URI.
     * 
     * <p>Handles URIs starting with "/app/" by removing the prefix.
     * Falls back to extracting the last segment of the URI path.</p>
     * 
     * @param uri the request URI
     * @return the extracted filename, or null if none found
     */
    private String extractFilename(String uri) {
        
        if (uri.startsWith("/app/")) {
            String remainder = uri.substring(5); // Remove "/app/"
            if (remainder.isEmpty()) {
                return "index.html";
            }
            return remainder;
        }
        
        // Fallback: extract last segment
        String[] segments = uri.split("/");
        if (segments.length > 0) {
            return segments[segments.length - 1];
        }
        
        return null;
    }
    
    /**
     * Sends a 404 Not Found response to the client.
     * 
     * @param toClient the output stream to send the response to
     * @param filename the filename that was not found
     * @throws IOException if writing the response fails
     */
    private void sendNotFoundResponse(OutputStream toClient, String filename) throws IOException {
        String notFoundHtml = "<!DOCTYPE html>\n" +
                             "<html><head><title>File Not Found</title></head>\n" +
                             "<body style=\"font-family: Arial, sans-serif; padding: 20px; text-align: center;\">\n" +
                             "<h1 style=\"color: #d32f2f;\">404 - File Not Found</h1>\n" +
                             "<p>The requested file <strong>" + escapeHtml(filename) + "</strong> was not found.</p>\n" +
                             "<p>Please check the filename and try again.</p>\n" +
                             "<p><a href=\"/app/index.html\">Return to Home</a></p>\n" +
                             "</body></html>";
        
        String httpResponse = "HTTP/1.1 404 Not Found\r\n" +
                             "Content-Type: text/html\r\n" +
                             "Content-Length: " + notFoundHtml.length() + "\r\n" +
                             "Connection: close\r\n" +
                             "\r\n" + notFoundHtml;
        
        toClient.write(httpResponse.getBytes());
        toClient.flush();
    }
    
    /**
     * Sends an HTTP error response to the client.
     * 
     * @param toClient the output stream to send the response to
     * @param errorMessage the error message to display
     * @param statusCode the HTTP status code
     * @throws IOException if writing the response fails
     */
    private void sendErrorResponse(OutputStream toClient, String errorMessage, int statusCode) throws IOException {
        String statusText = getStatusText(statusCode);
        String errorHtml = "<!DOCTYPE html>\n" +
                          "<html><head><title>Error</title></head>\n" +
                          "<body style=\"font-family: Arial, sans-serif; padding: 20px; text-align: center;\">\n" +
                          "<h1 style=\"color: #d32f2f;\">Error " + statusCode + "</h1>\n" +
                          "<p>" + escapeHtml(errorMessage) + "</p>\n" +
                          "<p><a href=\"/app/index.html\">Return to Home</a></p>\n" +
                          "</body></html>";
        
        String httpResponse = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                             "Content-Type: text/html\r\n" +
                             "Content-Length: " + errorHtml.length() + "\r\n" +
                             "Connection: close\r\n" +
                             "\r\n" + errorHtml;
        
        toClient.write(httpResponse.getBytes());
        toClient.flush();
    }
    
    /**
     * Determines the MIME content type based on file extension.
     * 
     * @param filename the filename to check
     * @return the appropriate MIME type
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "html":
            case "htm":
                return "text/html; charset=utf-8";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "txt":
                return "text/plain";
            case "ico":
                return "image/x-icon";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            default:
                return "text/plain";
        }
    }
    
    /**
     * Returns the status text for common HTTP status codes.
     * 
     * @param statusCode the HTTP status code
     * @return the corresponding status text
     */
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Error";
        }
    }
    
    /**
     * Escapes HTML special characters to prevent XSS attacks.
     * 
     * @param text the text to escape
     * @return the HTML-escaped text
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
     * Closes the servlet and releases resources.
     */
    @Override
    public void close() throws IOException {
    }
}