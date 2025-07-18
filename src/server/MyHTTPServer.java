package server;

import server.RequestParser.RequestInfo;
import servlets.Servlet;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * A multi-threaded HTTP server implementation that handles servlet-based request processing.
 * 
 * <p>This server listens on a specified port and routes HTTP requests to registered servlets
 * based on HTTP method (GET, POST, DELETE) and URI patterns. It uses a thread pool to
 * handle multiple concurrent connections.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * MyHTTPServer server = new MyHTTPServer(8080, 10);
 * server.addServlet("GET", "/api/data", new DataServlet());
 * server.addServlet("POST", "/upload", new UploadServlet());
 * server.start();
 * // Server runs until close() is called
 * server.close();
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see HTTPServer
 * @see Servlet
 */
public class MyHTTPServer extends Thread implements HTTPServer{
    
    /** The port number this server listens on */
    private final int port;
    
    /** The number of threads in the thread pool for handling requests */
    private final int nThreads;
    
    /** The server socket that accepts incoming connections */
    private ServerSocket serverSocket;
    
    /** Thread pool for handling client requests concurrently */
    private ExecutorService threadPool;
    
    /** Flag indicating whether the server is currently running */
    private volatile boolean running = false;
    
    /** Thread-safe map of servlets for GET requests */
    private final ConcurrentHashMap<String, Servlet> getServlets;
    
    /** Thread-safe map of servlets for POST requests */
    private final ConcurrentHashMap<String, Servlet> postServlets;
    
    /** Thread-safe map of servlets for DELETE requests */
    private final ConcurrentHashMap<String, Servlet> deleteServlets;
    
    /**
     * Creates a new HTTP server that will listen on the specified port.
     * 
     * @param port the port number to listen on (
     * @param nThreads the max number of threads in the pool for handling requests
     */
    public MyHTTPServer(int port, int nThreads){
        this.port = port;
        this.nThreads = nThreads;
        this.getServlets = new ConcurrentHashMap<>();
        this.postServlets = new ConcurrentHashMap<>();
        this.deleteServlets = new ConcurrentHashMap<>();
    }

    /**
     * Registers a servlet to handle requests for a specific HTTP method and URI.
     * 
     * <p>The HTTP command is case-insensitive. Supported methods are GET, POST, and DELETE.</p>
     * 
     * @param httpCommand the HTTP method ("GET", "POST", or "DELETE")
     * @param uri the URI pattern to match
     * @param s the servlet to handle matching requests
     */
    public void addServlet(String httpCommand, String uri, Servlet s){
        String command = httpCommand.toUpperCase();
        switch (command) {
            case "GET":
                getServlets.put(uri, s);
                break;
            case "POST":
                postServlets.put(uri, s);
                break;
            case "DELETE":
                deleteServlets.put(uri, s);
                break;
        }
    }

    /**
     * Removes a servlet mapping for a specific HTTP method and URI.
     * 
     * @param httpCommand the HTTP method ("GET", "POST", or "DELETE")
     * @param uri the URI pattern to remove
     */
    public void removeServlet(String httpCommand, String uri){
        String command = httpCommand.toUpperCase();
        switch (command) {
            case "GET":
                getServlets.remove(uri);
                break;
            case "POST":
                postServlets.remove(uri);
                break;
            case "DELETE":
                deleteServlets.remove(uri);
                break;
        }
    }

    /**
     * Main server loop that accepts and processes client connections.
     * 
     * <p>Creates a server socket, thread pool, and continuously accepts
     * incoming connections until the server is stopped.</p>
     */
    public void run(){
        try {
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newFixedThreadPool(nThreads);
            running = true;
            
            System.out.println("Server listening on port " + port);
            
            while(running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(10000); // 10 second timeout
                    
                    // Handle client in thread pool
                    threadPool.submit(() -> handleClient(clientSocket));
                    
                } catch (SocketTimeoutException e) {
                	continue;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Server startup failed: " + e.getMessage());
        }
    }
    
    /**
     * Handles an individual client connection.
     * 
     * <p>Parses the HTTP request, finds the appropriate servlet, and
     * delegates request processing. Handles errors by sending appropriate
     * HTTP error responses.</p>
     * 
     * @param clientSocket the connected client socket
     */
    private void handleClient(Socket clientSocket) {
        try {
            System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream outputStream = clientSocket.getOutputStream();
            
            try {
                RequestInfo requestInfo = RequestParser.parseRequest(reader);
                
                System.out.println("Request: " + requestInfo.getHttpCommand() + " " + requestInfo.getUri());
                
                // Find right servlet with longest URI match
                Servlet servlet = findServlet(requestInfo.getHttpCommand(), requestInfo.getUri());
                
                if (servlet != null) {
                    servlet.handle(requestInfo, outputStream);
                } else {
                    send404Response(outputStream, requestInfo.getUri());
                }
                
                // Ensure response is flushed
                outputStream.flush();
                
            } catch (IOException e) {
                System.err.println("Error parsing request: " + e.getMessage());
                send400Response(outputStream, e.getMessage());
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
                send500Response(outputStream, e.getMessage());
            }
            
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sends a 404 Not Found response to the client.
     * 
     * @param outputStream the client output stream
     * @param uri the requested URI that was not found
     * @throws IOException if writing to the output stream fails
     */
    private void send404Response(OutputStream outputStream, String uri) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\n" +
                         "Content-Type: text/html\r\n" +
                         "Connection: close\r\n" +
                         "\r\n" +
                         "<html><body><h1>404 Not Found</h1>" +
                         "<p>The requested resource " + uri + " was not found on this server.</p>" +
                         "</body></html>";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    /**
     * Sends a 400 Bad Request response to the client.
     * 
     * @param outputStream the client output stream
     * @param error the error message to include in the response
     * @throws IOException if writing to the output stream fails
     */
    private void send400Response(OutputStream outputStream, String error) throws IOException {
        String response = "HTTP/1.1 400 Bad Request\r\n" +
                         "Content-Type: text/html\r\n" +
                         "Connection: close\r\n" +
                         "\r\n" +
                         "<html><body><h1>400 Bad Request</h1>" +
                         "<p>Error: " + error + "</p>" +
                         "</body></html>";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    /**
     * Sends a 500 Internal Server Error response to the client.
     * 
     * @param outputStream the client output stream
     * @param error the error message to include in the response
     * @throws IOException if writing to the output stream fails
     */
    private void send500Response(OutputStream outputStream, String error) throws IOException {
        String response = "HTTP/1.1 500 Internal Server Error\r\n" +
                         "Content-Type: text/html\r\n" +
                         "Connection: close\r\n" +
                         "\r\n" +
                         "<html><body><h1>500 Internal Server Error</h1>" +
                         "<p>Error: " + error + "</p>" +
                         "</body></html>";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
    
    /**
     * Finds the best matching servlet for the given HTTP command and URI.
     * 
     * <p>Uses longest prefix matching to find the most specific servlet
     * for the requested URI.</p>
     * 
     * @param httpCommand the HTTP method
     * @param uri the requested URI
     * @return the matching servlet, or null if no match found
     */
    private Servlet findServlet(String httpCommand, String uri) {
        ConcurrentHashMap<String, Servlet> servlets;
        String command = httpCommand.toUpperCase();
        
        switch (command) {
            case "GET":
                servlets = getServlets;
                break;
            case "POST":
                servlets = postServlets;
                break;
            case "DELETE":
                servlets = deleteServlets;
                break;
            default:
                return null;
        }
        
        // Find longest match
        String bestMatch = null;
        int matchLength = 0;
        
        for (String savedUri : servlets.keySet()) {
            if (uri.startsWith(savedUri) && savedUri.length() > matchLength) {
                bestMatch = savedUri;
                matchLength = savedUri.length();
            }
        }
        
        return bestMatch != null ? servlets.get(bestMatch) : null;
    }
    
    /**
     * Starts the HTTP server in a separate thread.
     */
    @Override
    public void start(){
        super.start();
    }

    /**
     * Stops the HTTP server and cleans up all resources.
     * 
     * <p>Gracefully shuts down the server socket, thread pool, and all servlets.</p>
     */
    public void close(){
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        closeServlets(getServlets);
        closeServlets(postServlets);
        closeServlets(deleteServlets);
    }
    
    /**
     * Closes all servlets in the given map and clears the map.
     * 
     * @param servlets the servlet map to close and clear
     */
    private void closeServlets(ConcurrentHashMap<String, Servlet> servlets) {
        for (Servlet servlet : servlets.values()) {
            try {
                servlet.close();
            } catch (IOException e) {
                System.err.println("Error closing servlet: " + e.getMessage());
            }
        }
        servlets.clear();
    }
}