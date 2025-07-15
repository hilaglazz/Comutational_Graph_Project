package configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import graph.Agent;
import graph.ParallelAgent;

import java.util.ArrayList;

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
            agents.clear();
            List<String> lines = Files.readAllLines(Paths.get(configFile));
            lines.removeIf(line -> line.trim().isEmpty());

            if (lines.size() % 3 != 0) {
                throw new IllegalArgumentException("Malformed config: Each agent must have 3 lines (class, pubs, subs). Found " + lines.size() + " lines.");
            }

            java.util.Map<String, Integer> agentTypeCounts = new java.util.HashMap<>();

            for (int i = 0; i < lines.size(); i += 3) {
                int blockStart = i + 1;
                String classLine = lines.get(i).trim();
                String pubsLine = lines.get(i+1).trim();
                String subsLine = lines.get(i+2).trim();

                List<String> missing = new ArrayList<>();
                if (classLine.isEmpty()) missing.add("class");
                if (pubsLine.isEmpty()) missing.add("pubs");
                if (subsLine.isEmpty()) missing.add("subs");
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Malformed config: Agent block starting at line " + blockStart +
                        " is missing: " + String.join(", ", missing) + " line(s)."
                    );
                }

                String fullClassName = classLine;
                String shortClassName = fullClassName.replace("AP_ex6.src.", "");
                String[] pubsArray = pubsLine.split("\\s*,\\s*");
                String[] subsArray = subsLine.split("\\s*,\\s*");

                // Auto-generate unique agent name
                String simpleName = shortClassName.substring(shortClassName.lastIndexOf('.') + 1);
                int count = agentTypeCounts.getOrDefault(simpleName, 0) + 1;
                agentTypeCounts.put(simpleName, count);
                String agentName = simpleName + count;

                try {
                    Class<?> agentClass = Class.forName(shortClassName);
                    java.lang.reflect.Constructor<?> ctor = agentClass.getConstructor(String.class, String[].class, String[].class);
                    Agent agent = (Agent) ctor.newInstance(agentName, pubsArray, subsArray);
                    agents.add(new ParallelAgent(agent, 1));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Agent class not found: " + shortClassName, e);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error creating agent of type '" + shortClassName + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading config file: " + e.getMessage(), e);
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