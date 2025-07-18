package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser.RequestInfo;

/**
 * Interface defining the contract for servlet implementations in the HTTP server.
 * Servlets are responsible for handling specific types of HTTP requests and
 * generating appropriate responses.
 * 
 * <p>Implementations should:
 * <ul>
 *   <li>Process the incoming request information</li>
 *   <li>Generate appropriate HTTP responses</li>
 *   <li>Write response data to the output stream</li>
 *   <li>Handle errors gracefully</li>
 *   <li>Clean up resources when closed</li>
 * </ul>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public interface Servlet {
    
    /**
     * Handles an HTTP request and generates a response.
     * This method should parse the request information, perform the necessary
     * processing, and write a complete HTTP response to the output stream.
     * 
     * @param ri the parsed request information containing headers, parameters, and content
     * @param toClient the output stream to write the HTTP response to
     * @throws IOException if there's an error writing to the output stream or processing the request
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    
    /**
     * Closes the servlet and releases any resources it may be holding.
     * This method is called when the servlet is being unregistered from
     * the server or when the server is shutting down.
     * 
     * @throws IOException if there's an error releasing resources
     */
    void close() throws IOException;
}