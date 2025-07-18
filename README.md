Computational Graph System

A web-based computational graph system that allows users to create, visualize, and interact with mathematical computation graphs through a browser interface.

Background

This project implements a distributed computational graph system where:
- Agents perform operations on data
- Topics serve as communication channels between agents
- Messages carry data through the system
- A web interface allows remote management and visualization

The system follows a publish-subscribe pattern where agents can subscribe to topics to receive data, process it, and publish results to other topics, creating complex computational workflows.

Architecture
Core Components
- graph - Core graph infrastructure (Agent, Topic, Message, TopicManager)
- configs - Configuration management and agent implementations
- server - HTTP server and request handling
- servlets - Web request processors
- views - HTML generation and visualization
- main - Application entry point

 Key Features
- Thread-safe operations using ParallelAgent wrapper
- Dynamic configuration loading from text files
- Real-time visualization of computational graphs
- Interactive web interface for system control
- Cycle detection for graph validation

Installation

 Prerequisites
- Java 11 or higher
- Web browser 

 Setup
1. Clone the repository

2. Ensure the project structure includes:

project_root/
├── src/
│   ├── main/
│   ├── graph/
│   ├── configs/
│   ├── server/
│   ├── servlets/
│   └── views/
├── html_files/
│   ├── index.html
│   ├── form.html
│   ├── graph.html
│   └── temp.html
└── config_files/


3. Compile the project:
bash
javac -d bin src//*.java


Run the Application
Start the Server
bash
java -cp bin main.Main

The server will start on http://localhost:8080

Access the Web Interface
Open your browser and navigate to:
http://localhost:8080/app/index.html


To stop the Server
Press Enter in the terminal where the server is running.

Usage Guide

 1. Configuration File Format
Create configuration files with groups of 3 lines:

package.ClassName
input1,input2
output1,output2


Example Configuration:

configs.PlusAgent
A,B
Sum
configs.IncAgent
Sum
Result
configs.BinOpAgent
Result,C
FinalOutput


 2. Web Interface Components

 Left Panel - Controls
- Configuration Upload: Deploy new computational graphs
- Message Publisher: Send data to topics

 Center Panel - Graph Visualization
- Visual representation of the computational graph
- Topics shown as rectangles (green)
- Agents shown as circles (blue)
- Arrows indicate data flow direction

 Right Panel - Topic Values
- Real-time table of current topic values
- Shows subscriber and publisher counts
- Updates automatically when messages are sent

 3. Example Workflow
1. Upload a configuration file using the left panel
2. View the generated graph in the center panel
3. Send test messages to input topics
4. Watch values propagate through the system
5. Monitor results in the topics table

Available Agent Types

 Built-in Agents
- PlusAgent - Adds two input values
- IncAgent - Increments input value by 1
- BinOpAgent - Performs custom binary operations

 Creating Custom Agents
Implement the Agent interface:
java
public class MyAgent implements Agent {
    public MyAgent(String[] subs, String[] pubs) {
        // Constructor with input/output topics
    }
    
    public void callback(String topic, Message msg) {
        // Process incoming messages
    }
    
    // Implement other Agent methods...
}


Key Features

 Thread Safety
- All agents wrapped in ParallelAgent for concurrent execution
- Thread-safe topic management with ConcurrentHashMap
- Asynchronous message processing with bounded queues

 Graph Validation
- Automatic cycle detection to prevent infinite loops
- Visual feedback for problematic configurations
- Graceful error handling and reporting

 Web Interface
- Responsive design with iframe-based layout
- Real-time updates without page refresh
- Drag-and-drop graph visualization (when supported)

 Configuration Management
- Dynamic loading of computational graphs
- Support for complex multi-agent workflows
- Automatic resource cleanup on configuration changes

 Advanced Features
- Comprehensive Documentation: Full Javadoc for all public APIs
- Error Handling: Robust error handling with user-friendly messages
- Performance Optimization: Efficient graph algorithms and memory management
- Extensibility: Clean interfaces for adding new agent types

 UI Enhancements
- Interactive Graph: Visual graph representation with positioning algorithms
- Real-time Updates: Live topic value monitoring
- Responsive Design: Professional-looking web interface
- Status Indicators: Clear feedback for all operations

 Technical Elements
- SOLID Principles: Clean, maintainable code architecture
- Design Patterns: Proper use of Singleton, Factory, and Observer patterns
- Resource Management: Automatic cleanup and lifecycle management
- Scalability: Support for large, complex computational graphs

Development Notes

 Code Quality
- Extensive JavaDoc documentation
- Following SOLID principles
- Comprehensive error handling
- Thread-safe design patterns

 Testing
- Manual testing through web interface
- Configuration validation
- Cycle detection verification
- Concurrent operation testing


This project was developed as part of an advanced programming course. 

Demo Video

 Watch the system in action
[![Watch the demo](https://img.youtube.com/vi/3W9yqXC8z-g/0.jpg)](https://youtu.be/3W9yqXC8z-g)

5-minute demonstration covering:
- Project overview and bakcground
- System architecture and design
- Live demonstration of key features
- Advanced functionality beyond requirements
- Key learnings and takeaways


This project demonstrates advanced Java programming concepts including web servers, concurrent processing, and dynamic system configuration.
