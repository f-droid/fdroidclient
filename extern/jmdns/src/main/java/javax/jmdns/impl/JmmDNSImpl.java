/**
 *
 */
package javax.jmdns.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.util.NamedThreadFactory;

/**
 * This class enable multihoming mDNS. It will open a mDNS per IP address of the machine.
 *
 * @author C&eacute;drik Lime, Pierre Frisch
 */
public class JmmDNSImpl implements JmmDNS, NetworkTopologyListener, ServiceInfoImpl.Delegate {
    private static Logger                                      logger = Logger.getLogger(JmmDNSImpl.class.getName());

    private final Set<NetworkTopologyListener>                 _networkListeners;

    /**
     * Every JmDNS created.
     */
    private final ConcurrentMap<InetAddress, JmDNS>            _knownMDNS;

    /**
     * This enable the service info text update.
     */
    private final ConcurrentMap<String, ServiceInfo>           _services;

    /**
     * List of registered services
     */
    private final Set<String>                                  _serviceTypes;

    /**
     * Holds instances of ServiceListener's. Keys are Strings holding a fully qualified service type. Values are LinkedList's of ServiceListener's.
     */
    private final ConcurrentMap<String, List<ServiceListener>> _serviceListeners;

    /**
     * Holds instances of ServiceTypeListener's.
     */
    private final Set<ServiceTypeListener>                     _typeListeners;

    private final ExecutorService                              _listenerExecutor;

    private final ExecutorService                              _jmDNSExecutor;

    private final Timer                                        _timer;

    private final AtomicBoolean                                _isClosing;

    private final AtomicBoolean                                _closed;

    /**
     *
     */
    public JmmDNSImpl() {
        super();
        _networkListeners = Collections.synchronizedSet(new HashSet<NetworkTopologyListener>());
        _knownMDNS = new ConcurrentHashMap<InetAddress, JmDNS>();
        _services = new ConcurrentHashMap<String, ServiceInfo>(20);
        _listenerExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("JmmDNS Listeners"));
        _jmDNSExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("JmmDNS"));
        _timer = new Timer("Multihomed mDNS.Timer", true);
        _serviceListeners = new ConcurrentHashMap<String, List<ServiceListener>>();
        _typeListeners = Collections.synchronizedSet(new HashSet<ServiceTypeListener>());
        _serviceTypes = Collections.synchronizedSet(new HashSet<String>());
        (new NetworkChecker(this, NetworkTopologyDiscovery.Factory.getInstance())).start(_timer);
        _isClosing = new AtomicBoolean(false);
        _closed = new AtomicBoolean(false);
    }

    /*
     * (non-Javadoc)
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        if (_isClosing.compareAndSet(false, true)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Cancelling JmmDNS: " + this);
            }
            _timer.cancel();
            _listenerExecutor.shutdown();
            _jmDNSExecutor.shutdown();
            // We need to cancel all the DNS
            ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("JmmDNS.close"));
            try {
                for (final JmDNS mDNS : this.getDNS()) {
                    executor.submit(new Runnable() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void run() {
                            try {
                                mDNS.close();
                            } catch (IOException exception) {
                                // JmDNS never throws this is only because of the closeable interface
                            }
                        }
                    });
                }
            } finally {
                executor.shutdown();
            }
            try {
                executor.awaitTermination(DNSConstants.CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                logger.log(Level.WARNING, "Exception ", exception);
            }
            _knownMDNS.clear();
            _services.clear();
            _serviceListeners.clear();
            _typeListeners.clear();
            _serviceTypes.clear();
            _closed.set(true);
            JmmDNS.Factory.close();
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getNames()
     */
    @Override
    public String[] getNames() {
        Set<String> result = new HashSet<String>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getHostNames()
     */
    @Override
    public String[] getHostNames() {
        Set<String> result = new HashSet<String>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getHostName());
        }
        return result.toArray(new String[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getInetAddresses()
     */
    @Override
    public InetAddress[] getInetAddresses() throws IOException {
        Set<InetAddress> result = new HashSet<InetAddress>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getInetAddress());
        }
        return result.toArray(new InetAddress[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getDNS()
     */
    @Override
    public JmDNS[] getDNS() {
        synchronized (_knownMDNS) {
            return _knownMDNS.values().toArray(new JmDNS[_knownMDNS.size()]);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getInterfaces()
     */
    @Override
    @Deprecated
    public InetAddress[] getInterfaces() throws IOException {
        Set<InetAddress> result = new HashSet<InetAddress>();
        for (JmDNS mDNS : this.getDNS()) {
            result.add(mDNS.getInterface());
        }
        return result.toArray(new InetAddress[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name) {
        return this.getServiceInfos(type, name, false, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, long)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name, long timeout) {
        return this.getServiceInfos(type, name, false, timeout);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public ServiceInfo[] getServiceInfos(String type, String name, boolean persistent) {
        return this.getServiceInfos(type, name, persistent, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#getServiceInfos(java.lang.String, java.lang.String, boolean, long)
     */
    @Override
    public ServiceInfo[] getServiceInfos(final String type, final String name, final boolean persistent, final long timeout) {
        // We need to run this in parallel to respect the timeout.
        final JmDNS[] dnsArray = this.getDNS();
        final Set<ServiceInfo> result = new HashSet<ServiceInfo>(dnsArray.length);
        if (dnsArray.length > 0) {
            List<Callable<ServiceInfo>> tasks = new ArrayList<Callable<ServiceInfo>>(dnsArray.length);
            for (final JmDNS mDNS : dnsArray) {
                tasks.add(new Callable<ServiceInfo>() {

                    @Override
                    public ServiceInfo call() throws Exception {
                        return mDNS.getServiceInfo(type, name, persistent, timeout);
                    }

                });
            }

            ExecutorService executor = Executors.newFixedThreadPool(tasks.size(), new NamedThreadFactory("JmmDNS.getServiceInfos"));
            try {
                List<Future<ServiceInfo>> results = Collections.emptyList();
                try {
                    results = executor.invokeAll(tasks, timeout + 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    logger.log(Level.FINE, "Interrupted ", exception);
                    Thread.currentThread().interrupt();
                    // Will terminate next loop early.
                }

                for (Future<ServiceInfo> future : results) {
                    if (future.isCancelled()) {
                        continue;
                    }
                    try {
                        ServiceInfo info = future.get();
                        if (info != null) {
                            result.add(info);
                        }
                    } catch (InterruptedException exception) {
                        logger.log(Level.FINE, "Interrupted ", exception);
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException exception) {
                        logger.log(Level.WARNING, "Exception ", exception);
                    }
                }
            } finally {
                executor.shutdown();
            }
        }
        return result.toArray(new ServiceInfo[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void requestServiceInfo(String type, String name) {
        this.requestServiceInfo(type, name, false, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void requestServiceInfo(String type, String name, boolean persistent) {
        this.requestServiceInfo(type, name, persistent, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, long)
     */
    @Override
    public void requestServiceInfo(String type, String name, long timeout) {
        this.requestServiceInfo(type, name, false, timeout);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#requestServiceInfo(java.lang.String, java.lang.String, boolean, long)
     */
    @Override
    public void requestServiceInfo(final String type, final String name, final boolean persistent, final long timeout) {
        // We need to run this in parallel to respect the timeout.
        for (final JmDNS mDNS : this.getDNS()) {
            _jmDNSExecutor.submit(new Runnable() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    mDNS.requestServiceInfo(type, name, persistent, timeout);
                }
            });
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    @Override
    public void addServiceTypeListener(ServiceTypeListener listener) throws IOException {
        _typeListeners.add(listener);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.addServiceTypeListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeServiceTypeListener(javax.jmdns.ServiceTypeListener)
     */
    @Override
    public void removeServiceTypeListener(ServiceTypeListener listener) {
        _typeListeners.remove(listener);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.removeServiceTypeListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void addServiceListener(String type, ServiceListener listener) {
        final String loType = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(loType);
        if (list == null) {
            _serviceListeners.putIfAbsent(loType, new LinkedList<ServiceListener>());
            list = _serviceListeners.get(loType);
        }
        if (list != null) {
            synchronized (list) {
                if (!list.contains(listener)) {
                    list.add(listener);
                }
            }
        }
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.addServiceListener(type, listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeServiceListener(java.lang.String, javax.jmdns.ServiceListener)
     */
    @Override
    public void removeServiceListener(String type, ServiceListener listener) {
        String loType = type.toLowerCase();
        List<ServiceListener> list = _serviceListeners.get(loType);
        if (list != null) {
            synchronized (list) {
                list.remove(listener);
                if (list.isEmpty()) {
                    _serviceListeners.remove(loType, list);
                }
            }
        }
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.removeServiceListener(type, listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.ServiceInfoImpl.Delegate#textValueUpdated(javax.jmdns.ServiceInfo, byte[])
     */
    @Override
    public void textValueUpdated(ServiceInfo target, byte[] value) {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            for (JmDNS mDNS : dnsArray) {
                ServiceInfo info = ((JmDNSImpl) mDNS).getServices().get(target.getQualifiedName());
                if (info != null) {
                    info.setText(value);
                } else {
                    logger.warning("We have a mDNS that does not know about the service info being updated.");
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#registerService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void registerService(ServiceInfo info) throws IOException {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        // This is really complex. We need to clone the service info for each DNS but then we loose the ability to update it.
        synchronized (_services) {
            for (JmDNS mDNS : dnsArray) {
                mDNS.registerService(info.clone());
            }
            ((ServiceInfoImpl) info).setDelegate(this);
            _services.put(info.getQualifiedName(), info);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#unregisterService(javax.jmdns.ServiceInfo)
     */
    @Override
    public void unregisterService(ServiceInfo info) {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            _services.remove(info.getQualifiedName());
            for (JmDNS mDNS : dnsArray) {
                mDNS.unregisterService(info);
            }
            ((ServiceInfoImpl) info).setDelegate(null);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#unregisterAllServices()
     */
    @Override
    public void unregisterAllServices() {
        // We need to get the list out of the synchronized block to prevent dead locks
        final JmDNS[] dnsArray = this.getDNS();
        synchronized (_services) {
            _services.clear();
            for (JmDNS mDNS : dnsArray) {
                mDNS.unregisterAllServices();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#registerServiceType(java.lang.String)
     */
    @Override
    public void registerServiceType(String type) {
        _serviceTypes.add(type);
        for (JmDNS mDNS : this.getDNS()) {
            mDNS.registerServiceType(type);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#list(java.lang.String)
     */
    @Override
    public ServiceInfo[] list(String type) {
        return this.list(type, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#list(java.lang.String, long)
     */
    @Override
    public ServiceInfo[] list(final String type, final long timeout) {
        final JmDNS[] dnsArray = this.getDNS();
        // We need to run this in parallel to respect the timeout.
        final Set<ServiceInfo> result = new HashSet<ServiceInfo>(dnsArray.length * 5);
        if (dnsArray.length > 0) {
            List<Callable<List<ServiceInfo>>> tasks = new ArrayList<Callable<List<ServiceInfo>>>(dnsArray.length);
            for (final JmDNS mDNS : dnsArray) {
                tasks.add(new Callable<List<ServiceInfo>>() {
                    @Override
                    public List<ServiceInfo> call() throws Exception {
                        return Arrays.asList(mDNS.list(type, timeout));
                    }
                });
            }

            ExecutorService executor = Executors.newFixedThreadPool(tasks.size(), new NamedThreadFactory("JmmDNS.list"));
            try {
                List<Future<List<ServiceInfo>>> results = Collections.emptyList();
                try {
                    results = executor.invokeAll(tasks, timeout + 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    logger.log(Level.FINE, "Interrupted ", exception);
                    Thread.currentThread().interrupt();
                    // Will terminate next loop early.
                }

                for (Future<List<ServiceInfo>> future : results) {
                    if (future.isCancelled()) {
                        continue;
                    }
                    try {
                        result.addAll(future.get());
                    } catch (InterruptedException exception) {
                        logger.log(Level.FINE, "Interrupted ", exception);
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException exception) {
                        logger.log(Level.WARNING, "Exception ", exception);
                    }
                }
            } finally {
                executor.shutdown();
            }
        }
        return result.toArray(new ServiceInfo[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#listBySubtype(java.lang.String)
     */
    @Override
    public Map<String, ServiceInfo[]> listBySubtype(String type) {
        return this.listBySubtype(type, DNSConstants.SERVICE_INFO_TIMEOUT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#listBySubtype(java.lang.String, long)
     */
    @Override
    public Map<String, ServiceInfo[]> listBySubtype(final String type, final long timeout) {
        Map<String, List<ServiceInfo>> map = new HashMap<String, List<ServiceInfo>>(5);
        for (ServiceInfo info : this.list(type, timeout)) {
            String subtype = info.getSubtype();
            if (!map.containsKey(subtype)) {
                map.put(subtype, new ArrayList<ServiceInfo>(10));
            }
            map.get(subtype).add(info);
        }

        Map<String, ServiceInfo[]> result = new HashMap<String, ServiceInfo[]>(map.size());
        for (String subtype : map.keySet()) {
            List<ServiceInfo> infoForSubType = map.get(subtype);
            result.put(subtype, infoForSubType.toArray(new ServiceInfo[infoForSubType.size()]));
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#addNetworkTopologyListener(javax.jmdns.NetworkTopologyListener)
     */
    @Override
    public void addNetworkTopologyListener(NetworkTopologyListener listener) {
        _networkListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#removeNetworkTopologyListener(javax.jmdns.NetworkTopologyListener)
     */
    @Override
    public void removeNetworkTopologyListener(NetworkTopologyListener listener) {
        _networkListeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS#networkListeners()
     */
    @Override
    public NetworkTopologyListener[] networkListeners() {
        return _networkListeners.toArray(new NetworkTopologyListener[_networkListeners.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyListener#inetAddressAdded(javax.jmdns.NetworkTopologyEvent)
     */
    @Override
    public void inetAddressAdded(NetworkTopologyEvent event) {
        InetAddress address = event.getInetAddress();
        try {
            if (!_knownMDNS.containsKey(address)) {
                synchronized (_knownMDNS) {
                    if (!_knownMDNS.containsKey(address)) {
                        final JmDNS dns = JmDNS.create(address);
                        if (_knownMDNS.putIfAbsent(address, dns) == null) {
                            // We need to register the services and listeners with the new JmDNS
                            final Collection<String> types = _serviceTypes;
                            final Collection<ServiceInfo> infos = _services.values();
                            final Collection<ServiceTypeListener> typeListeners = _typeListeners;
                            final Map<String, List<ServiceListener>> serviceListeners = _serviceListeners;
                            _jmDNSExecutor.submit(new Runnable() {
                                /**
                                 * {@inheritDoc}
                                 */
                                @Override
                                public void run() {
                                    // Register Types
                                    for (String type : types) {
                                        dns.registerServiceType(type);
                                    }
                                    // Register services
                                    for (ServiceInfo info : infos) {
                                        try {
                                            dns.registerService(info.clone());
                                        } catch (IOException exception) {
                                            // logger.warning("Unexpected unhandled exception: " + exception);
                                        }
                                    }
                                    // Add ServiceType Listeners
                                    for (ServiceTypeListener listener : typeListeners) {
                                        try {
                                            dns.addServiceTypeListener(listener);
                                        } catch (IOException exception) {
                                            // logger.warning("Unexpected unhandled exception: " + exception);
                                        }
                                    }
                                    // Add Service Listeners
                                    for (String type : serviceListeners.keySet()) {
                                        List<ServiceListener> listeners = serviceListeners.get(type);
                                        synchronized (listeners) {
                                            for (ServiceListener listener : listeners) {
                                                dns.addServiceListener(type, listener);
                                            }
                                        }
                                    }
                                }
                            });
                            final NetworkTopologyEvent jmdnsEvent = new NetworkTopologyEventImpl(dns, address);
                            for (final NetworkTopologyListener listener : this.networkListeners()) {
                                _listenerExecutor.submit(new Runnable() {
                                    /**
                                     * {@inheritDoc}
                                     */
                                    @Override
                                    public void run() {
                                        listener.inetAddressAdded(jmdnsEvent);
                                    }
                                });
                            }
                        } else {
                            dns.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Unexpected unhandled exception: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyListener#inetAddressRemoved(javax.jmdns.NetworkTopologyEvent)
     */
    @Override
    public void inetAddressRemoved(NetworkTopologyEvent event) {
        InetAddress address = event.getInetAddress();
        try {
            if (_knownMDNS.containsKey(address)) {
                synchronized (_knownMDNS) {
                    if (_knownMDNS.containsKey(address)) {
                        JmDNS mDNS = _knownMDNS.remove(address);
                        mDNS.close();
                        final NetworkTopologyEvent jmdnsEvent = new NetworkTopologyEventImpl(mDNS, address);
                        for (final NetworkTopologyListener listener : this.networkListeners()) {
                            _listenerExecutor.submit(new Runnable() {
                                /**
                                 * {@inheritDoc}
                                 */
                                @Override
                                public void run() {
                                    listener.inetAddressRemoved(jmdnsEvent);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Unexpected unhandled exception: " + e);
        }
    }

    /**
     * Checks the network state.<br/>
     * If the network change, this class will reconfigure the list of DNS do adapt to the new configuration.
     */
    static class NetworkChecker extends TimerTask {
        private static Logger                  logger1 = Logger.getLogger(NetworkChecker.class.getName());

        private final NetworkTopologyListener  _mmDNS;

        private final NetworkTopologyDiscovery _topology;

        private Set<InetAddress>               _knownAddresses;

        public NetworkChecker(NetworkTopologyListener mmDNS, NetworkTopologyDiscovery topology) {
            super();
            this._mmDNS = mmDNS;
            this._topology = topology;
            _knownAddresses = Collections.synchronizedSet(new HashSet<InetAddress>());
        }

        public void start(Timer timer) {
            // Run once up-front otherwise the list of servers will only appear after a delay.
            run();
            timer.schedule(this, DNSConstants.NETWORK_CHECK_INTERVAL, DNSConstants.NETWORK_CHECK_INTERVAL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                InetAddress[] curentAddresses = _topology.getInetAddresses();
                Set<InetAddress> current = new HashSet<InetAddress>(curentAddresses.length);
                for (InetAddress address : curentAddresses) {
                    current.add(address);
                    if (!_knownAddresses.contains(address)) {
                        final NetworkTopologyEvent event = new NetworkTopologyEventImpl(_mmDNS, address);
                        _mmDNS.inetAddressAdded(event);
                    }
                }
                for (InetAddress address : _knownAddresses) {
                    if (!current.contains(address)) {
                        final NetworkTopologyEvent event = new NetworkTopologyEventImpl(_mmDNS, address);
                        _mmDNS.inetAddressRemoved(event);
                    }
                }
                _knownAddresses = current;
            } catch (Exception e) {
                logger1.warning("Unexpected unhandled exception: " + e);
            }
        }

    }

}
