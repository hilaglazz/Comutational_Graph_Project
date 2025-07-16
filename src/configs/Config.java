package configs;

/**
 * Interface for configuration objects in the computational graph system.
 * Implementations provide parsing, validation, and access to config properties.
 */
public interface Config {
    // Create the configuration
    void create();
    // Get the name of the configuration
    String getName();
    // Get the type of the configuration
    String getType();
    // Check if the configuration is valid
    boolean isValid();
}
