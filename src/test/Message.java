package test;

import java.util.Date;

public class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

    // Constructor with only String parameter
    public Message(String input) {
		this.data = input.getBytes();
    	this.asText = input;
    	double tempDouble;
        try {
            tempDouble = Double.parseDouble(asText.trim());
        } catch (NumberFormatException e) {
            tempDouble = Double.NaN;
        }
        this.asDouble = tempDouble;
        this.date = new Date();
    }

    // Constructor with String and double parameters
    public Message(double asDouble) {
        this(Double.toString(asDouble));
    }
    
    public Message(byte[] input) {
        this(new String(input));
    }
    
    public Message(byte input) {
        this(Byte.toString(input));
    }
}
