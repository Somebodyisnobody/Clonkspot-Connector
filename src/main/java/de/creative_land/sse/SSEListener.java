package de.creative_land.sse;

/**
 * An interface that defines an SSEListener. SSE messages are passed to this
 * listener as well as well as changes to the state of the stream.
 * 
 * @author cenodis
 *
 */
public interface SSEListener {

    /**
     * Called when the stream is opened. If the stream is reconnected this may be
     * called multiple times.
     */
    public void onOpen();

    /**
     * Called when a new message was received.
     * 
     * @param msg The message.
     */
    public void onMessage(SSEMessage msg);

    /**
     * Called when an error occurred while the stream was being read.
     * <p>
     * This indicates a terminating error. No further messages will be received.
     * 
     * @param throwable The Throwable that caused the error.
     */
    public void onError(Throwable throwable);

    /**
     * Called when the stream is closed by the server.
     */
    public void onComplete();
}
