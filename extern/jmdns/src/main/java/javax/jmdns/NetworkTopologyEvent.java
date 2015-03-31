/**
 *
 */
package javax.jmdns;

import java.net.InetAddress;
import java.util.EventObject;

/**
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public abstract class NetworkTopologyEvent extends EventObject {

    /**
     *
     */
    private static final long serialVersionUID = -8630033521752540987L;

    /**
     * Constructs a Service Event.
     * 
     * @param eventSource
     *            The DNS on which the Event initially occurred.
     * @exception IllegalArgumentException
     *                if source is null.
     */
    protected NetworkTopologyEvent(final Object eventSource) {
        super(eventSource);
    }

    /**
     * Returns the JmDNS instance associated with the event or null if it is a generic event.
     * 
     * @return JmDNS instance
     */
    public abstract JmDNS getDNS();

    /**
     * The Internet address affected by this event.
     * 
     * @return InetAddress
     */
    public abstract InetAddress getInetAddress();

}
