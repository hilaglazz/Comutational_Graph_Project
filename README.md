# Java Topic Graph Server

## Background

This project was conducted as part of an Advanced Programming course. It implements a computational graph server and web-based visualization system, allowing users to upload configuration files, visualize computational graphs, and interact with them in real time. The system is designed for educational purposes and demonstrates concepts in concurrent programming, HTTP servers, and interactive data visualization.

## Installation

1. **Clone or download the repository** to your local machine.
2. Make sure you have **Java 11 or higher** installed.
3. (Optional) Use an IDE like Eclipse or IntelliJ IDEA for easier project management.

## Features

- **Upload configuration files** to define computational graphs.
- **Interactive graph visualization** using D3.js (nodes can be dragged, zoomed and dragged).
- **Send messages to topics** and observe real-time updates in the graph.
- **Export graph** as SVG.
- **Modern, user-friendly web interface**.

## Project Structure

```
java-topic-graph-server/
│
├── bin/                # Compiled Java classes
├── config_files/       # Example configuration files for graphs
├── html_files/         # Frontend HTML files (graph visualization, forms, etc.)
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

## Getting Started

### Prerequisites

- Java 11 or higher
- (Optional) Eclipse or IntelliJ IDEA for easier project management

### Build & Run

1. **Compile the project** (if not already compiled):

   ```sh
   javac -d bin src/**/*.java
   ```

2. **Run the server:**

   ```sh
   java -cp bin test.Main
   ```

   The server will start on port 8080.

3. **Open the web interface:**

   - Go to [http://localhost:8080/app/graph.html](http://localhost:8080/app/graph.html) for the graph visualization.
   - Or use [http://localhost:8080/app/index.html](http://localhost:8080/app/index.html) for the full dashboard.

### Usage

- **Upload a configuration file** (`.conf`) using the form on the left panel.
- **Visualize the computational graph** in the center panel.
- **Send messages to topics** using the form to see how the graph updates.
- **Use the Help button** for quick guidance.

### Configuration Files

- Example configuration files are provided in the `config_files/` directory.
- The format describes topics, agents, and their connections.

### Project Components

- **Java Backend:** Handles HTTP requests, file uploads, and serves the graph data.
- **Frontend (HTML + D3.js):** Renders the graph and provides user controls.

## Customization

- Modify `html_files/graph.html` for visualization tweaks.
- Add or edit configuration files in `config_files/`.

## Troubleshooting

- If nodes can't be moved, ensure the animation is running (not paused).
- If the server doesn't start, check your Java version and classpath.

## License

This project is for educational purposes (Advanced Programming course). 