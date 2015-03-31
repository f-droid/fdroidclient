// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns;

import java.util.EventListener;

/**
 * Listener for service updates.
 *
 * @author Arthur van Hoff, Werner Randelshofer, Pierre Frisch
 */
public interface ServiceListener extends EventListener {
    /**
     * A service has been added.<br/>
     * <b>Note:</b>This event is only the service added event. The service info associated with this event does not include resolution information.<br/>
     * To get the full resolved information you need to listen to {@link #serviceResolved(ServiceEvent)} or call {@link JmDNS#getServiceInfo(String, String, long)}
     *
     * <pre>
     *  ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName())
     * </pre>
     * <p>
     * Please note that service resolution may take a few second to resolve.
     * </p>
     *
     * @param event
     *            The ServiceEvent providing the name and fully qualified type of the service.
     */
    void serviceAdded(ServiceEvent event);

    /**
     * A service has been removed.
     *
     * @param event
     *            The ServiceEvent providing the name and fully qualified type of the service.
     */
    void serviceRemoved(ServiceEvent event);

    /**
     * A service has been resolved. Its details are now available in the ServiceInfo record.<br/>
     * <b>Note:</b>This call back will never be called if the service does not resolve.<br/>
     *
     * @param event
     *            The ServiceEvent providing the name, the fully qualified type of the service, and the service info record.
     */
    void serviceResolved(ServiceEvent event);

}
