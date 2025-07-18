package graph;
import java.util.Date;

/**
 * Represents a message that can be passed between agents in the computational graph.
 * Messages are immutable data containers that automatically convert between different
 * data types (byte array, text, and numeric) for convenience.
 * 
 * <p>The Message class provides automatic type conversion:
 * <ul>
 *   <li>Raw data as byte array</li>
 *   <li>Text representation as String</li>
 *   <li>Numeric representation as double (NaN if not parsable)</li>
 *   <li>Timestamp of creation</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Message textMsg = new Message("Hello World");
 * Message numMsg = new Message(42.5);
 * Message dataMsg = new Message(new byte[]{1, 2, 3});
 * 
 * // Access different representations
 * String text = numMsg.asText;        // "42.5"
 * double value = numMsg.asDouble;     // 42.5
 * Date created = numMsg.date;         // creation timestamp
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 */
public class Message {

    /** The raw data as a byte array */
    public final byte[] data;
    
    /** The data interpreted as a UTF-8 string */
    public final String asText;
    
    /** The data interpreted as a double, or NaN if not parsable */
    public final double asDouble;
    
    /** The timestamp when this message was created */
    public final Date date;
    
    /**
     * Constructs a message from raw byte data.
     * 
     * @param data the raw byte data, not null
     */
    public Message(byte[] data){
        this.data = data;
        this.asText = new String(data);
        this.asDouble = tryDouble(asText);
        this.date = new Date();
    }
    
    /**
     * Constructs a message from string data.
     * 
     * @param data the string data, not null
     */
    public Message(String data){
        this(data.getBytes());
    }
    
    /**
     * Constructs a message from numeric data.
     * 
     * @param data the numeric value
     */
    public Message(double data){
        this(Double.toString(data));
    }

    /**
     * Attempts to parse the given text as a double value.
     * 
     * @param text the text to parse
     * @return the parsed double value, or NaN if parsing fails
     */
    private double tryDouble(String text) {
        try {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}