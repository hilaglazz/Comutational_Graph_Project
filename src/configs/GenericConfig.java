package configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import graph.Agent;
import graph.ParallelAgent;

import java.util.ArrayList;

/**
 * Represents a generic configuration for the computational graph.
 * Handles parsing, validation, and access to config properties.
 */
public class GenericConfig implements Config {
    private String configFile = "";
    private List<ParallelAgent> agents = new ArrayList<>();

    // Load a generic configuration from a file.
    public static GenericConfig load(String filePath) throws IOException {
        GenericConfig config = new GenericConfig();
        config.setConfFile(filePath);
        config.create();
        return config;
    }

    // Set the configuration file.
    public void setConfFile(String name) {
        this.configFile = name;
    }

    // Create the configuration.
    @Override
    public void create() {
        try {
            agents.clear();
            List<String> lines = Files.readAllLines(Paths.get(configFile));
            lines.removeIf(line -> line.trim().isEmpty());

            // Handle the case where missing part of the block
            if (lines.size() % 3 != 0) { 
                int blockStart = (lines.size() / 3) * 3 + 1; // Get the start of the block that is not complete
                int rem = 3 - lines.size() % 3; // Get the number of lines that are missing
                throw new IllegalArgumentException(
                    ("Malformed config: Agent block starting at line " + blockStart + " is incomplete. This block is missing " + rem + " line(s).<br>" +
                    "Each agent block must have exactly 3 lines in this order:<br>" +
                    "  1. Agent class name<br>" +
                    "  2. Subscribers (input topics)<br>" +
                    "  3. Publishers (output topics)<br>" +
                    "Please check your file format and ensure all required lines are present.")
                );
            }

            java.util.Map<String, Integer> agentTypeCounts = new java.util.HashMap<>(); // Create a map to count the number of each agent type

            for (int i = 0; i < lines.size(); i += 3) { // Loop through the lines in groups of 3
                int blockStart = i + 1;
                String classLine = lines.get(i).trim();
                String pubsLine = lines.get(i+1).trim();
                String subsLine = lines.get(i+2).trim();

                List<String> missing = new ArrayList<>(); // Create a list to store the missing lines
                if (classLine.isEmpty()) missing.add("class"); // Add "class" to the list if the class line is empty
                if (subsLine.isEmpty()) missing.add("subs"); // Add "subs" to the list if the subs line is empty
                if (pubsLine.isEmpty()) missing.add("pubs"); // Add "pubs" to the list if the pubs line is empty
                if (!missing.isEmpty()) { // If there are missing lines, throw an exception
                    throw new IllegalArgumentException(
                        "Malformed config: Agent block starting at line " + blockStart +
                        " is missing: " + String.join(", ", missing) + " line(s)."
                    );
                }

                String fullClassName = classLine; // Get the full class name
                String shortClassName = fullClassName.replace("AP_ex6.src.", ""); // Remove the package name from the class name
                String[] pubsArray = pubsLine.split("\\s*,\\s*"); // Split the pubs line into an array of strings
                String[] subsArray = subsLine.split("\\s*,\\s*"); // Split the subs line into an array of strings

                // Auto-generate unique agent name
                String simpleName = shortClassName.substring(shortClassName.lastIndexOf('.') + 1); // Get the simple name of the class
                int count = agentTypeCounts.getOrDefault(simpleName, 0) + 1; // Get the count of the agent type
                agentTypeCounts.put(simpleName, count); // Add the agent type to the map
                String agentName = simpleName + count; // Create the agent name

                try {
                    Class<?> agentClass = Class.forName(shortClassName); // Get the class of the agent
                    java.lang.reflect.Constructor<?> ctor = agentClass.getConstructor(String.class, String[].class, String[].class); // Get the constructor of the agent
                    Agent agent = (Agent) ctor.newInstance(agentName, pubsArray, subsArray); // Create a new instance of the agent
                    agents.add(new ParallelAgent(agent, 1)); // Add the agent to the list
                } catch (ClassNotFoundException e) { // If the class is not found, throw an exception
                    throw new IllegalArgumentException("Agent class not found: " + shortClassName, e);
                } catch (Exception e) { // If there is an error creating the agent, throw an exception
                    throw new IllegalArgumentException("Error creating agent of type '" + shortClassName + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading config file: " + e.getMessage(), e);
        }
    }

    // Get the name of the configuration.
    @Override
    public String getName() {
        return "GenericConfig: " + configFile;
    }

    // Get the type of the configuration.
    @Override
    public String getType() {
        // Return the type of this config (e.g., "generic")
        return "generic";
    }

    // Check if the configuration is valid.
    @Override
    public boolean isValid() {
        // Implement validation logic for the config
        return true; // Placeholder, update with real validation
    }
}