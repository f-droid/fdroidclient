/**
 *
 */
package javax.jmdns.impl;

import java.util.EventListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

/**
 * This class track the status of listener.<br/>
 * The main purpose of this class is to collapse consecutive events so that we can guarantee the correct call back sequence.
 * 
 * @param <T>
 *            listener type
 */
public class ListenerStatus<T extends EventListener> {

    public static class ServiceListenerStatus extends ListenerStatus<ServiceListener> {
        private static Logger                            logger = Logger.getLogger(ServiceListenerStatus.class.getName());

        private final ConcurrentMap<String, ServiceInfo> _addedServices;

        /**
         * @param listener
         *            listener being tracked.
         * @param synch
         *            true if that listener can be called asynchronously
         */
        public ServiceListenerStatus(ServiceListener listener, boolean synch) {
            super(listener, synch);
            _addedServices = new ConcurrentHashMap<String, ServiceInfo>(32);
        }

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
        void serviceAdded(ServiceEvent event) {
            String qualifiedName = event.getName() + "." + event.getType();
            if (null == _addedServices.putIfAbsent(qualifiedName, event.getInfo().clone())) {
                this.getListener().serviceAdded(event);
                ServiceInfo info = event.getInfo();
                if ((info != null) && (info.hasData())) {
                    this.getListener().serviceResolved(event);
                }
            } else {
                logger.finer("Service Added called for a service already added: " + event);
            }
        }

        /**
         * A service has been removed.
         * 
         * @param event
         *            The ServiceEvent providing the name and fully qualified type of the service.
         */
        void serviceRemoved(ServiceEvent event) {
            String qualifiedName = event.getName() + "." + event.getType();
            if (_addedServices.remove(qualifiedName, _addedServices.get(qualifiedName))) {
                this.getListener().serviceRemoved(event);
            } else {
                logger.finer("Service Removed called for a service already removed: " + event);
            }
        }

        /**
         * A service has been resolved. Its details are now available in the ServiceInfo record.<br/>
         * <b>Note:</b>This call back will never be called if the service does not resolve.<br/>
         * 
         * @param event
         *            The ServiceEvent providing the name, the fully qualified type of the service, and the service info record.
         */
        synchronized void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            if ((info != null) && (info.hasData())) {
                String qualifiedName = event.getName() + "." + event.getType();
                ServiceInfo previousServiceInfo = _addedServices.get(qualifiedName);
                if (!_sameInfo(info, previousServiceInfo)) {
                    if (null == previousServiceInfo) {
                        if (null == _addedServices.putIfAbsent(qualifiedName, info.clone())) {
                            this.getListener().serviceResolved(event);
                        }
                    } else {
                        if (_addedServices.replace(qualifiedName, previousServiceInfo, info.clone())) {
                            this.getListener().serviceResolved(event);
                        }
                    }
                } else {
                    logger.finer("Service Resolved called for a service already resolved: " + event);
                }
            } else {
                logger.warning("Service Resolved called for an unresolved event: " + event);

            }
        }

        private static final boolean _sameInfo(ServiceInfo info, ServiceInfo lastInfo) {
            if (info == null) return false;
            if (lastInfo == null) return false;
            if (!info.equals(lastInfo)) return false;
            byte[] text = info.getTextBytes();
            byte[] lastText = lastInfo.getTextBytes();
            if (text.length != lastText.length) return false;
            for (int i = 0; i < text.length; i++) {
                if (text[i] != lastText[i]) return false;
            }
            return true;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder aLog = new StringBuilder(2048);
            aLog.append("[Status for ");
            aLog.append(this.getListener().toString());
            if (_addedServices.isEmpty()) {
                aLog.append(" no type event ");
            } else {
                aLog.append(" (");
                for (String service : _addedServices.keySet()) {
                    aLog.append(service + ", ");
                }
                aLog.append(") ");
            }
            aLog.append("]");
            return aLog.toString();
        }

    }

    public static class ServiceTypeListenerStatus extends ListenerStatus<ServiceTypeListener> {
        private static Logger                       logger = Logger.getLogger(ServiceTypeListenerStatus.class.getName());

        private final ConcurrentMap<String, String> _addedTypes;

        /**
         * @param listener
         *            listener being tracked.
         * @param synch
         *            true if that listener can be called asynchronously
         */
        public ServiceTypeListenerStatus(ServiceTypeListener listener, boolean synch) {
            super(listener, synch);
            _addedTypes = new ConcurrentHashMap<String, String>(32);
        }

        /**
         * A new service type was discovered.
         * 
         * @param event
         *            The service event providing the fully qualified type of the service.
         */
        void serviceTypeAdded(ServiceEvent event) {
            if (null == _addedTypes.putIfAbsent(event.getType(), event.getType())) {
                this.getListener().serviceTypeAdded(event);
            } else {
                logger.finest("Service Type Added called for a service type already added: " + event);
            }
        }

        /**
         * A new subtype for the service type was discovered.
         * 
         * <pre>
         * &lt;sub&gt;._sub.&lt;app&gt;.&lt;protocol&gt;.&lt;servicedomain&gt;.&lt;parentdomain&gt;.
         * </pre>
         * 
         * @param event
         *            The service event providing the fully qualified type of the service with subtype.
         */
        void subTypeForServiceTypeAdded(ServiceEvent event) {
            if (null == _addedTypes.putIfAbsent(event.getType(), event.getType())) {
                this.getListener().subTypeForServiceTypeAdded(event);
            } else {
                logger.finest("Service Sub Type Added called for a service sub type already added: " + event);
            }
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder aLog = new StringBuilder(2048);
            aLog.append("[Status for ");
            aLog.append(this.getListener().toString());
            if (_addedTypes.isEmpty()) {
                aLog.append(" no type event ");
            } else {
                aLog.append(" (");
                for (String type : _addedTypes.keySet()) {
                    aLog.append(type + ", ");
                }
                aLog.append(") ");
            }
            aLog.append("]");
            return aLog.toString();
        }

    }

    public final static boolean SYNCHONEOUS  = true;
    public final static boolean ASYNCHONEOUS = false;

    private final T             _listener;

    private final boolean       _synch;

    /**
     * @param listener
     *            listener being tracked.
     * @param synch
     *            true if that listener can be called asynchronously
     */
    public ListenerStatus(T listener, boolean synch) {
        super();
        _listener = listener;
        _synch = synch;
    }

    /**
     * @return the listener
     */
    public T getListener() {
        return _listener;
    }

    /**
     * Return <cod>true</code> if the listener must be called synchronously.
     * 
     * @return the synch
     */
    public boolean isSynchronous() {
        return _synch;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getListener().hashCode();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ListenerStatus) && this.getListener().equals(((ListenerStatus<?>) obj).getListener());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "[Status for " + this.getListener().toString() + "]";
    }
}
