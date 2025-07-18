package server;

import servlets.Servlet;

/**
 * Interface for HTTP server implementations that can run servlets.
 * 
 * <p>This interface defines the contract for HTTP servers that can handle
 * HTTP requests by routing them to appropriate servlets based on HTTP
 * commands (GET, POST, DELETE) and URIs.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * HTTPServer server = new MyHTTPServer(8080, 5);
 * server.addServlet("GET", "/api/data", new DataServlet());
 * server.addServlet("POST", "/api/upload", new UploadServlet());
 * server.start();
 * // Server is now running and handling requests
 * server.close(); // Shutdown when done
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see Servlet
 * @see Runnable
 */
public interface HTTPServer extends Runnable{
    
    /**
     * Adds a servlet to handle requests for a specific HTTP command and URI.
     * 
     * <p>Associates a servlet with a particular HTTP method and URI pattern.
     * When requests matching this combination are received, they will be
     * routed to the specified servlet for processing.</p>
     * 
     * @param httpCommanmd the HTTP method (e.g., "GET", "POST", "DELETE")
     * @param uri the URI pattern to match (e.g., "/api/data", "/upload")
     * @param s the servlet to handle matching requests
     */
    public void addServlet(String httpCommanmd, String uri, Servlet s);
    
    /**
     * Removes a servlet mapping for a specific HTTP command and URI.
     * 
     * <p>Removes the servlet association for the given HTTP method and URI.
     * Subsequent requests to this endpoint will not be handled by any servlet.</p>
     * 
     * @param httpCommanmd the HTTP method (e.g., "GET", "POST", "DELETE")
     * @param uri the URI pattern to remove (e.g., "/api/data", "/upload")
     */
    public void removeServlet(String httpCommanmd, String uri);
    
    /**
     * Starts the HTTP server.
     * 
     * <p>Begins accepting and processing HTTP requests. The server will
     * continue running until {@link #close()} is called.</p>
     */
    public void start();
    
    /**
     * Stops the HTTP server and releases resources.
     * 
     * <p>Gracefully shuts down the server, stops accepting new requests,
     * and cleans up any allocated resources.</p>
     */
    public void close();
}