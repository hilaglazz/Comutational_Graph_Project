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
