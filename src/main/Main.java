package main;

import server.HTTPServer;
import server.MyHTTPServer;
import servlets.ConfLoader;
import servlets.HtmlLoader;
import servlets.TopicDisplayer;


import java.io.FileWriter;
import java.io.IOException;

import configs.GenericConfig;


public class Main {
    public static void main(String[] args) throws Exception {
        
        System.out.println("Starting Computational Graph Server...");
        System.out.println("Server will be available at: http://localhost:8080/app/index.html");
        System.out.println("Press Enter to stop the server.");
        
        // Create HTTP server
        HTTPServer server = new MyHTTPServer(8080, 5);
        
        //Add servlets
        server.addServlet("GET", "/publish", new TopicDisplayer());
        server.addServlet("POST", "/upload", new ConfLoader());
        server.addServlet("GET", "/app/", new HtmlLoader("html_files"));
        
        // Start server
        server.start();        
        System.out.println("Server started successfully!");
        
        // Wait for user input to stop
        System.in.read();
        
        // Close server
        server.close();
        System.out.println("Server stopped. Goodbye!");
    }
}
