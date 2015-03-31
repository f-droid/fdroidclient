// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns;

import java.util.EventObject;

/**
 *
 */
public abstract class ServiceEvent extends EventObject implements Cloneable {

    /**
     *
     */
    private static final long serialVersionUID = -8558445644541006271L;

    /**
     * Constructs a Service Event.
     * 
     * @param eventSource
     *            The object on which the Event initially occurred.
     * @exception IllegalArgumentException
     *                if source is null.
     */
    public ServiceEvent(final Object eventSource) {
        super(eventSource);
    }

    /**
     * Returns the JmDNS instance which originated the event.
     * 
     * @return JmDNS instance
     */
    public abstract JmDNS getDNS();

    /**
     * Returns the fully qualified type of the service.
     * 
     * @return type of the service.
     */
    public abstract String getType();

    /**
     * Returns the instance name of the service. Always returns null, if the event is sent to a service type listener.
     * 
     * @return name of the service
     */
    public abstract String getName();

    /**
     * Returns the service info record, or null if the service could not be resolved. Always returns null, if the event is sent to a service type listener.
     * 
     * @return service info record
     * @see javax.jmdns.ServiceEvent#getInfo()
     */
    public abstract ServiceInfo getInfo();

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ServiceEvent clone() {
        try {
            return (ServiceEvent) super.clone();
        } catch (CloneNotSupportedException exception) {
            // clone is supported
            return null;
        }
    }

}