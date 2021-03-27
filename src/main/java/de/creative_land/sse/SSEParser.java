package de.creative_land.sse;

import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import de.creative_land.Controller;

/**
 * Parses an SSE stream and passes messages to a listener.
 * 
 * @author cenodis
 *
 */
public class SSEParser implements Subscriber<String> {

    /**
     * The UTF-8 BOM.
     */
    private static final String UTF_8_BOM = "\uFEFF";

    /**
     * Whether or not the current character is the first character in the stream.
     */
    private boolean firstChar;
    
    /**
     * Has the stream of this parser been closed.
     */
    private boolean closed;

    /**
     * The current reconnect timeout in milliseconds.
     */
    private int timeout;

    /**
     * The event listener to pass messages to.
     */
    private SSEListener listener;

    /**
     * The subscription to the source stream.
     */
    private Subscription sub;

    /**
     * The current event.
     */
    private String event;

    /**
     * The current data.
     */
    private StringBuilder data;

    /**
     * The current id.
     */
    private String id;

    /**
     * Constructs a new SSEParser with a default timeout.
     * <p>
     * Note that the server can override this timeout with a custom one.
     * 
     * @param timeout The timeout.
     */
    public SSEParser(int timeout) {
        this.firstChar = true;
        this.timeout = timeout;
        this.closed = false;

        this.event = "";
        this.data = new StringBuilder();
        this.id = "";
    }

    /**
     * Sets the listener to receive new messages.
     * 
     * @param listener The listener.
     */
    public void setListener(SSEListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    /**
     * Retrieves the current timeout value.
     * 
     * @return The timeout in milliseconds.
     */
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * Closes the parser. This is irreversible and means no new messages or subscriptions will be accepted.
     * <p>
     * Any currently outstanding messages or errors will be suppressed.
     */
    public void close() {
        closed = true;
        sub.cancel();
    }

    /**
     * Dispatches the current event and passes it to the listener.
     */
    private void dispatch() {
        if (data.isEmpty()) {
            this.event = "";
            this.data.setLength(0);
            return;
        }

        data.deleteCharAt(data.length() - 1);

        listener.onMessage(SSEMessage.of(event, data.toString(), id));
        
        data.setLength(0);
        this.event = "";
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (listener == null) {
            throw new IllegalStateException("A listener must be specified.");
        }
        if (closed) {
            throw new IllegalStateException("Parser has been closed.");
        }
        if (sub != null) {
            sub.cancel();
        }
        this.sub = subscription;
        this.event = "";
        this.data.setLength(0);
        this.id = "";
        sub.request(1);
    }

    @Override
    public void onNext(String item) {
        if (closed) {
            sub.request(1);
            return;
        }
        if (firstChar && !item.isEmpty() && item.startsWith(UTF_8_BOM)) {
            item = item.substring(1);
        }
        firstChar = false;
        if (item.isEmpty()) {
            this.dispatch();
            sub.request(1);
            return;
        }
        if (item.charAt(0) == ':') {
            sub.request(1);
            return;
        }
        String[] values;
        if (item.contains(":")) {
            values = item.split(":", 2);
            if (values[1].startsWith(" ")) {
                values[1] = values[1].substring(1);
            }
        } else {
            values = new String[]{item, ""};
        }

        switch (values[0]) {
        case "event":
            this.event = values[1];
            break;
        case "id":
            this.id = values[1];
            break;
        case "data":
            this.data.append(values[1]);
            this.data.append("\n");
            break;
        case "retry":
            try {
                this.timeout = Integer.parseInt(values[1]);
            } catch (NumberFormatException e) {
            }
            break;
        }
        sub.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        if (closed) {
            return;
        }
        this.close();
        this.data.setLength(0);
        this.event = "";
    }

    @Override
    public void onComplete() {
        if (closed) {
            return;
        }
        try {
            listener.onComplete();
        } catch (Exception e) {
            Controller.INSTANCE.log
                    .addLogEntry("SSEParser: listener onComplete event has thrown an exception. That should not happen.");
        }
    }
}