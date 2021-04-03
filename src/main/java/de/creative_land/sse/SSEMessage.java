package de.creative_land.sse;

import java.util.Objects;

/**
 * Represents the content of an SSE Message.
 * <p>
 * This object is immutable.
 * 
 * @author cenodis
 *
 */
public class SSEMessage {
    
    /**
     * The event of this message.
     */
    public final String event;
    
    /**
     * The data of this message. Individual data segments are seperated by newlines.
     */
    public final String data;
    
    /**
     * The id of this message.
     */
    public final String id;
    
    /**
     * Constructor
     * 
     * @param event
     *          the event
     * @param data
     *          the data
     * @param id
     *          the id
     */
    private SSEMessage(String event, String data, String id) {
        this.event = event;
        this.data = data;
        this.id = id;
    }
    
    /**
     * Constructs a new message object.
     * 
     * @param event
     *          The event.
     * @param data
     *          The data.
     * @param id
     *          The id.
     * @return The message object.
     */
    public static SSEMessage of(String event, String data, String id) {
        return new SSEMessage(Objects.requireNonNull(event), Objects.requireNonNull(data), Objects.requireNonNull(id));
    }
}
