package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for parsing HTTP requests into structured data.
 * 
 * <p>This class provides static methods to parse HTTP requests from a BufferedReader
 * and convert them into RequestInfo objects with request components.</p>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class RequestParser {

    /**
     * Parses an HTTP request from a BufferedReader into a RequestInfo object.
     * 
     * <p>Extracts the HTTP method, URI, URI segments, parameters, headers, and content
     * from the incoming request stream.</p>
     * 
     * @param reader the BufferedReader containing the HTTP request
     * @return a RequestInfo object containing all parsed request data
     * @throws IOException if the request cannot be parsed or is malformed
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {        
        // Parse first line
        String firstLine = reader.readLine();
        if (firstLine == null || firstLine.trim().isEmpty()) {
            throw new IOException("Empty request");
        }
                
        String[] parts = firstLine.split(" ");
        if (parts.length < 2) {
            throw new IOException("Invalid request line: " + firstLine);
        }
        
        String httpCommand = parts[0].toUpperCase();
        String uri = parts[1];
        
        // Parse URI, extract segments and parameters
        String[] uriParts = uri.split("\\?", 2);
        String path = uriParts[0];
        
        // Find URI segments and count them
        String[] segments = path.split("/");
        int segmentCount = 0;
        for (String segment : segments) {
            if (!segment.trim().isEmpty()) {
                segmentCount++;
            }
        }
        
        // Save URI segments in array
        String[] uriSegments = new String[segmentCount];
        int index = 0;
        for (String segment : segments) {
            if (!segment.trim().isEmpty()) {
                uriSegments[index++] = segment;
            }
        }
        
        // Parse URI parameters (GET parameters)
        Map<String, String> parameters = new HashMap<>();
        if (uriParts.length > 1) {
            String queryString = uriParts[1];
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    // URL decode the values
                    parameters.put(urlDecode(keyValue[0]), urlDecode(keyValue[1]));
                }
            }
        }
        
        // Parse headers
        String line;
        int contentLength = 0;
        String contentType = "";
        Map<String, String> headers = new HashMap<>();
        
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            System.out.println("Header: " + line); // Debug output
            
            if (line.toLowerCase().startsWith("content-length:")) {
                String lengthStr = line.substring(15).trim();
                try {
                    contentLength = Integer.parseInt(lengthStr);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid content-length: " + lengthStr);
                }
            } else if (line.toLowerCase().startsWith("content-type:")) {
                contentType = line.substring(13).trim();
            }
            
            // Store all headers
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                String headerValue = line.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }
        
        // Read content 
        StringBuilder contentBuilder = new StringBuilder();
        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = reader.read(buffer, totalRead, contentLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            contentBuilder.append(buffer, 0, totalRead);
        }
        
        // For POST requests with form data, also parse form parameters
        String contentStr = contentBuilder.toString();
        if ("POST".equals(httpCommand) && contentType.contains("application/x-www-form-urlencoded")) {
            String[] params = contentStr.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    parameters.put(urlDecode(keyValue[0]), urlDecode(keyValue[1]));
                }
            }
        }
        
        byte[] content = contentStr.getBytes();
        
        return new RequestInfo(httpCommand, uri, uriSegments, parameters, content);    
    }
    
    /**
     * URL decodes a string using UTF-8 encoding.
     * 
     * @param encoded the URL-encoded string
     * @return the decoded string, or original string if decoding fails
     */
    private static String urlDecode(String encoded) {
        try {
            return java.net.URLDecoder.decode(encoded, "UTF-8");
        } catch (Exception e) {
            return encoded;
        }
    }
    
    /**
     * Container class for parsed HTTP request information.
     * 
     * <p>This immutable class holds all the components of a parsed HTTP request
     * including the method, URI, parameters, and content.</p>
     */
    public static class RequestInfo {
        /** The HTTP command (GET, POST, DELETE, etc.) */
        private final String httpCommand;
        
        /** The full URI from the request */
        private final String uri;
        
        /** The URI path segments (excluding empty segments) */
        private final String[] uriSegments;
        
        /** Map of request parameters from query string and form data */
        private final Map<String, String> parameters;
        
        /** The raw content body of the request */
        private final byte[] content;

        /**
         * Creates a new RequestInfo with the specified components.
         * 
         * @param httpCommand the HTTP method
         * @param uri the request URI
         * @param uriSegments the URI path segments
         * @param parameters the request parameters
         * @param content the request body content
         */
        public RequestInfo(String httpCommand, String uri, String[] uriSegments, Map<String, String> parameters, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        /**
         * Returns the HTTP command (method).
         * 
         * @return the HTTP command in uppercase (e.g., "GET", "POST")
         */
        public String getHttpCommand() {
            return httpCommand;
        }

        /**
         * Returns the full URI from the request.
         * 
         * @return the complete URI including query string if present
         */
        public String getUri() {
            return uri;
        }

        /**
         * Returns the URI path segments.
         * 
         * @return array of non-empty path segments from the URI
         */
        public String[] getUriSegments() {
            return uriSegments;
        }

        /**
         * Returns the request parameters.
         * 
         * @return map containing query string and form parameters
         */
        public Map<String, String> getParameters() {
            return parameters;
        }

        /**
         * Returns the raw content body of the request.
         * 
         * @return byte array containing the request body
         */
        public byte[] getContent() {
            return content;
        }
    }
}