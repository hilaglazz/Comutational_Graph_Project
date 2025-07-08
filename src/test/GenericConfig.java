/*
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays; // Add this import for Arrays.toString()

public class GenericConfig implements Config {

    private String configFile = "";
    private List<ParallelAgent> agents = new ArrayList<>();

    public void setConfFile(String name) {
        this.configFile = name;
    }

    @Override
    public void create() {
        try {
            // Read all lines from the file into a list
            List<String> lines = Files.readAllLines(Paths.get(configFile));

            // Check if the list size is divisible by 3
            if (lines.size() % 3 != 0) {
                System.out.println("Input is incorrect. List size: " + lines.size());
                return;
            }

            // Process each agent definition - every 3 lines
            for (int i = 0; i < lines.size(); i += 3) {
                String[] agentInfo = lines.get(i).split("\\."); // Use "\\." to split by a literal dot
                if (agentInfo.length < 2) {
                    throw new IllegalArgumentException("Class name must have at least two parts: " + lines.get(i));
                }
                String agentType = agentInfo[agentInfo.length - 2] + "." + agentInfo[agentInfo.length - 1]; ////////////
                ArrayList<String> subs = new ArrayList<>(Arrays.asList(lines.get(i + 1).split(","))); // Convert to ArrayList
                ArrayList<String> pubs = new ArrayList<>(Arrays.asList(lines.get(i + 2).split(","))); // Convert to ArrayList
                System.out.println("subs: " + subs);
                System.out.println("pubs: " + pubs);

                try {
                    // Create an Agent instance from the list data
                    Class<?> agentClass = Class.forName(agentType);
                    Agent agent = (Agent) agentClass.getConstructor(ArrayList.class, ArrayList.class)
                            .newInstance(subs, pubs);

                    // Wrap the agent with ParallelAgent
                    ParallelAgent parallelAgent = new ParallelAgent(agent, 1);
                    agents.add(parallelAgent);

                } catch (Exception e) {
                    System.err.println("Error creating agent of type " + agentType + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Handle file reading exceptions
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // Close all agents in the list
        for (ParallelAgent agent : agents) {
            try {
                agent.close();
            } catch (Exception e) {
                System.err.println("Error closing agent: " + e.getMessage());
            }
        }
        // Clear the list after closing all agents
        agents.clear(); 
    }

    @Override
    public String getName() {
        return "Generic Config";
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
*/
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class GenericConfig implements Config {
    private String configFile = "";
    private List<ParallelAgent> agents = new ArrayList<>();

    public static GenericConfig load(String filePath) throws IOException {
        GenericConfig config = new GenericConfig();
        config.setConfFile(filePath);
        config.create();
        return config;
    }

    public void setConfFile(String name) {
        this.configFile = name;
    }

    @Override
    public void create() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(configFile));
            
            // Remove empty lines and trim whitespace
            lines.removeIf(line -> line.trim().isEmpty());
            
            if (lines.size() % 3 != 0) {
                System.out.println("Input is incorrect. List size: " + lines.size());
                return;
            }

            for (int i = 0; i < lines.size(); i += 3) {
                String fullClassName = lines.get(i).trim();
                String shortClassName = fullClassName.replace("AP_ex6.src.", "");
                System.out.println("Loading agent: " + shortClassName);
                
                List<String> subs = Arrays.asList(lines.get(i+1).trim().split("\\s*,\\s*"));
                List<String> pubs = Arrays.asList(lines.get(i+2).trim().split("\\s*,\\s*"));
                
                System.out.println("Subscribers: " + subs);
                System.out.println("Publishers: " + pubs);

                try {
                    Class<?> agentClass = Class.forName(shortClassName);
                    
                     // Convert List<String> to String[] for constructor
                    String[] subsArray = subs.toArray(new String[0]);
                    String[] pubsArray = pubs.toArray(new String[0]);
                    // Try different constructor signatures
                    Agent agent;
                    try {
                        // Try (List, List) constructor first
                        agent = (Agent) agentClass.getConstructor(List.class, List.class)
                                .newInstance(subs, pubs);
                    } catch (NoSuchMethodException e) {
                        // Fall back to (String, List, List) constructor
                        agent = (Agent) agentClass.getConstructor(String[].class, String[].class)
                        .newInstance(subsArray, pubsArray);
                    }
                    
                    agents.add(new ParallelAgent(agent, 1));
                    System.out.println("Successfully created agent: " + shortClassName);
                    
                } catch (ClassNotFoundException e) {
                    System.err.println("Agent class not found: " + shortClassName);
                    System.err.println("Current classpath: " + System.getProperty("java.class.path"));
                } catch (Exception e) {
                    System.err.println("Error creating agent " + shortClassName + ": " + e);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        agents.forEach(agent -> {
            try {
                agent.close();
            } catch (Exception e) {
                System.err.println("Error closing agent: " + e.getMessage());
            }
        });
        agents.clear();
    }

    @Override
    public String getName() {
        return "GenericConfig: " + configFile;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}