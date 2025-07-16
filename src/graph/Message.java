package graph;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Represents a message in the computational graph system.
 * Contains text, double, and date information.
 */
public class Message {
    private static final int MAX_MESSAGE_LENGTH = 10000; // 10KB limit for message content
    private static final int MAX_BYTE_ARRAY_SIZE = 1024 * 1024; // 1MB limit for byte arrays
    
    public final byte[] data; // The data of the message
    public final String asText; // The text of the message
    public final double asDouble; // The double value of the message
    public final Date date; // The date of the message

    //Constructors:

    // Constructor with only String parameter
    public Message(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Message input cannot be null");
        }
        
        if (input.length() > MAX_MESSAGE_LENGTH) {
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
                    tempDouble = Double.NaN;
                }
            } catch (NumberFormatException e) {
                tempDouble = Double.NaN;
            }
            this.asDouble = tempDouble;
            this.date = new Date();
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating message from string", e);
        }
    }

    // Constructor with double parameter
    public Message(double asDouble) {
        this(validateAndConvertDouble(asDouble));
    }
    
    // Constructor with byte array parameter
    public Message(byte[] input) {
        this(validateAndConvertByteArray(input));
    }
    
    // Constructor with byte parameter
    public Message(byte input) {
        this(Byte.toString(input));
    }
    
    // Helper methods for validation
   
    // Validate and convert a double value to a string
    private static String validateAndConvertDouble(double asDouble) {
        if (Double.isInfinite(asDouble) || Double.isNaN(asDouble)) {
            throw new IllegalArgumentException("Double value cannot be infinite or NaN");
        }
        return Double.toString(asDouble);
    }
    
    // Validate and convert a byte array to a string
    private static String validateAndConvertByteArray(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Byte array cannot be null");
        }
        
        // if the byte array is too large then truncate it
        if (input.length > MAX_BYTE_ARRAY_SIZE) { 
            byte[] truncated = new byte[MAX_BYTE_ARRAY_SIZE]; 
            System.arraycopy(input, 0, truncated, 0, MAX_BYTE_ARRAY_SIZE); 
            input = truncated; 
        }
        
        return new String(input, StandardCharsets.UTF_8); // Convert the byte array to a string
    }
    
    // Additional utility methods:
    // Check if the double value is valid
    public boolean isValidDouble() {
        return !Double.isNaN(asDouble) && !Double.isInfinite(asDouble);
    }
    
    // Check if the message is empty
    public boolean isEmpty() {
        return asText == null || asText.trim().isEmpty();
    }
    
    // Getters:
    
    // Get the length of the message
    public int getLength() {
        return asText != null ? asText.length() : 0;
    }
    
    // Get the data of the message
    public byte[] getData() {
        return data != null ? data.clone() : null; // Return defensive copy
    }
    
    // Get the text of the message
    public String getAsText() {
        return asText;
    }
    
    // Get the double value of the message
    public double getAsDouble() {
        return asDouble;
    }
    
    // Get the date of the message
    public Date getDate() {
        return date != null ? new Date(date.getTime()) : null; // Return defensive copy
    }
    
    // Convert the message to a string
    @Override
    public String toString() {
        return "Message{" +
               "text='" + (asText != null ? asText.substring(0, Math.min(50, asText.length())) + "..." : "null") + '\'' +
               ", double=" + asDouble +
               ", date=" + date +
               ", length=" + getLength() +
               '}';
    }
    
    // Check if the message is equal to another object
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Message message = (Message) obj;
        
        if (Double.compare(message.asDouble, asDouble) != 0) return false;
        if (asText != null ? !asText.equals(message.asText) : message.asText != null) return false;
        return date != null ? date.equals(message.date) : message.date == null; 
    }
    
    // Get the hash code of the message
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
