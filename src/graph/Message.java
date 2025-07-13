package graph;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Message {
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    private static final int MAX_MESSAGE_LENGTH = 10000; // 10KB limit for message content
    private static final int MAX_BYTE_ARRAY_SIZE = 1024 * 1024; // 1MB limit for byte arrays
    
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

    // Constructor with only String parameter
    public Message(String input) {
        if (input == null) {
            LOGGER.warning("Attempted to create message with null input");
            throw new IllegalArgumentException("Message input cannot be null");
        }
        
        if (input.length() > MAX_MESSAGE_LENGTH) {
            LOGGER.warning("Message too long: " + input.length() + " characters, truncating");
            input = input.substring(0, MAX_MESSAGE_LENGTH);
        }
        
        try {
            this.data = input.getBytes(StandardCharsets.UTF_8);
            this.asText = input;
            
            // Parse double value
            double tempDouble;
            try {
                tempDouble = Double.parseDouble(asText.trim());
                if (Double.isInfinite(tempDouble) || Double.isNaN(tempDouble)) {
                    LOGGER.warning("Invalid double value: " + asText + ", using NaN");
                    tempDouble = Double.NaN;
                }
            } catch (NumberFormatException e) {
                LOGGER.fine("Could not parse as double: " + asText + ", using NaN");
                tempDouble = Double.NaN;
            }
            this.asDouble = tempDouble;
            this.date = new Date();
            
            LOGGER.fine("Message created: " + input.length() + " characters, double: " + tempDouble);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating message from string: " + input, e);
            throw new RuntimeException("Error creating message from string", e);
        }
    }

    // Constructor with String and double parameters
    public Message(double asDouble) {
        this(validateAndConvertDouble(asDouble));
    }
    
    public Message(byte[] input) {
        this(validateAndConvertByteArray(input));
    }
    
    public Message(byte input) {
        this(Byte.toString(input));
    }
    
    // Helper methods for validation
    private static String validateAndConvertDouble(double asDouble) {
        if (Double.isInfinite(asDouble) || Double.isNaN(asDouble)) {
            LOGGER.warning("Attempted to create message with invalid double: " + asDouble);
            throw new IllegalArgumentException("Double value cannot be infinite or NaN");
        }
        return Double.toString(asDouble);
    }
    
    private static String validateAndConvertByteArray(byte[] input) {
        if (input == null) {
            LOGGER.warning("Attempted to create message with null byte array");
            throw new IllegalArgumentException("Byte array cannot be null");
        }
        
        if (input.length > MAX_BYTE_ARRAY_SIZE) {
            LOGGER.warning("Byte array too large: " + input.length + " bytes, truncating");
            byte[] truncated = new byte[MAX_BYTE_ARRAY_SIZE];
            System.arraycopy(input, 0, truncated, 0, MAX_BYTE_ARRAY_SIZE);
            input = truncated;
        }
        
        return new String(input, StandardCharsets.UTF_8);
    }
    
    // Additional utility methods
    public boolean isValidDouble() {
        return !Double.isNaN(asDouble) && !Double.isInfinite(asDouble);
    }
    
    public boolean isEmpty() {
        return asText == null || asText.trim().isEmpty();
    }
    
    public int getLength() {
        return asText != null ? asText.length() : 0;
    }
    
    public byte[] getData() {
        return data != null ? data.clone() : null; // Return defensive copy
    }
    
    public String getAsText() {
        return asText;
    }
    
    public double getAsDouble() {
        return asDouble;
    }
    
    public Date getDate() {
        return date != null ? new Date(date.getTime()) : null; // Return defensive copy
    }
    
    @Override
    public String toString() {
        return "Message{" +
               "text='" + (asText != null ? asText.substring(0, Math.min(50, asText.length())) + "..." : "null") + '\'' +
               ", double=" + asDouble +
               ", date=" + date +
               ", length=" + getLength() +
               '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Message message = (Message) obj;
        
        if (Double.compare(message.asDouble, asDouble) != 0) return false;
        if (asText != null ? !asText.equals(message.asText) : message.asText != null) return false;
        return date != null ? date.equals(message.date) : message.date == null;
    }
    
    @Override
    public int hashCode() {
        int result;
        long temp;
        result = asText != null ? asText.hashCode() : 0;
        temp = Double.doubleToLongBits(asDouble);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}
