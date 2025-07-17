# Computational Graph Runner

## Overview

This project is a **Computational Graph Runner** designed for an advanced programming course assignment. It provides a server-side Java application that allows users to upload configuration files describing computational graphs, visualize and interact with these graphs via a web interface, and publish messages to topics in real time.

### Key Features

- **Upload and parse configuration files** describing computational graphs (topics and agents).
- **Visualize the computational graph** and monitor topic values in real time.
- **Publish messages** to topics, triggering agent computations and graph updates.
- **RESTful HTTP server** with custom servlet routing for extensibility.
- **Modern, interactive web UI** for configuration, control, and monitoring.

---

## Project Structure

```
java-topic-graph-server/
│
├── bin/                # Compiled Java classes
├── config_files/       # Example configuration files for graphs
├── html_files/         # Frontend HTML files (graph visualization, forms, etc.)
├── doc/                # Javadoc-generated documentation and resources
├── src/                # Java source code
│   ├── configs/        # Configuration and graph logic
│   ├── graph/          # Core graph and agent classes
│   ├── server/         # HTTP server and request handling
│   ├── servlets/       # Servlets for file upload, HTML serving, etc.
│   ├── test/           # Main entry point for running the server
│   └── views/          # HTML graph writer
├── .classpath          # Eclipse/IDEA classpath file
├── .project            # Eclipse project file
```
---

## Main Components

### 1. HTTP Server

- **MyHTTPServer**: Custom multi-threaded HTTP server supporting GET, POST, DELETE, and dynamic servlet routing.
- **RequestParser**: Parses HTTP requests, headers, parameters, and multipart file uploads.
- **Servlets**: Each endpoint (e.g., `/upload`, `/publish`, `/app/`) is handled by a specific servlet class.

### 2. Servlets

- **ConfLoader**: Handles POST requests to `/upload`, saves and parses configuration files, builds the computational graph, and returns a visualization.
- **HtmlLoader**: Serves static HTML, CSS, JS, and text files for the web UI.
- **TopicDisplayer**: Handles GET requests to `/publish` (publishing messages to topics) and `/topic-values` (returns current topic values as JSON or HTML).

### 3. Computational Graph

- **Graph/Node**: Represents the computational graph as nodes (topics and agents) and edges (subscriptions/publications).
- **Agent**: Interface for computational units (e.g., PlusAgent, MulAgent, IncAgent, BinOpAgent, IncAgent).
- **Topic**: Represents a named channel for message passing; supports publish/subscribe.
- **Message**: Encapsulates data sent between topics and agents.
- **TopicManagerSingleton**: Manages all topics globally, ensuring unique topic instances.

### 4. Configuration

- **GenericConfig**: Loads and validates configuration files, instantiates agents and topics, and builds the graph.
- **Config**: Interface for configuration objects.

### 5. Web UI

- **index.html**: Main dashboard with panels for configuration upload, graph visualization, and real-time value monitoring.
- **form.html**: Upload form for configuration files and message publishing form.
- **temp.html**: Welcome and instructions page.
- **graph.html**: (If present) Used for graph visualization.

---

## Getting Started

### Installation

1. Clone or download the repository to your local machine.
2. Make sure you have Java 24 installed.

### Running the Server

1. Compile the Java source code.
2. Run the `test.Main` class (e.g., `java test.Main`).
3. The server starts on `localhost:8080`.

### Web Interface

- Open `http://localhost:8080/app/index.html` in your browser.
- Use the left panel to upload a configuration file (`.conf` or `.txt`).
- The center panel visualizes the computational graph.
- The right panel displays real-time topic values.
- Use the "Send Message" form to publish values to topics.
- Use the Help button for quick guidance.

### Troubleshooting
- If nodes can't be moved, ensure the animation is running (not paused).
- If the server doesn't start, check your Java version and classpath.

---

## Example Configuration

Configuration files describe the agents, topics, and their connections. See the `config_files/` directory for examples.

---

## Extensibility

- Add new agent types by implementing the `Agent` interface.
- Add new servlets for additional endpoints.
- Customize the web UI by editing files in `html_files/`.

---

## Documentation

- Javadoc documentation is available in the `doc/` directory.

--- 