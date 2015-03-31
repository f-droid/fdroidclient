// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns;

import java.util.EventListener;

/**
 * Listener for service types.
 * 
 * @author Arthur van Hoff, Werner Randelshofer
 */
public interface ServiceTypeListener extends EventListener {
    /**
     * A new service type was discovered.
     * 
     * @param event
     *            The service event providing the fully qualified type of the service.
     */
    void serviceTypeAdded(ServiceEvent event);

    /**
     * A new subtype for the service type was discovered.
     * 
     * <pre>
     * &lt;sub&gt;._sub.&lt;app&gt;.&lt;protocol&gt;.&lt;servicedomain&gt;.&lt;parentdomain&gt;.
     * </pre>
     * 
     * @param event
     *            The service event providing the fully qualified type of the service with subtype.
     * @since 3.2.0
     */
    void subTypeForServiceTypeAdded(ServiceEvent event);
}
