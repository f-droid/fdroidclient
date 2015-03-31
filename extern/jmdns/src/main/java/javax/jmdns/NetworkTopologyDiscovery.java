package javax.jmdns;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicReference;

import javax.jmdns.impl.NetworkTopologyDiscoveryImpl;

/**
 * This class is used to resolve the list of Internet address to use when attaching JmDNS to the network.
 * <p>
 * To create you own filtering class for Internet Addresses you will need to implement the class and the factory delegate. These must be called before any other call to JmDNS.
 *
 * <pre>
 * public static class MyNetworkTopologyDiscovery implements NetworkTopologyDiscovery {
 *
 *     &#064;Override
 *     public InetAddress[] getInetAddresses() {
 *         // TODO Auto-generated method stub
 *         return null;
 *     }
 *
 *     &#064;Override
 *     public boolean useInetAddress(NetworkInterface networkInterface, InetAddress interfaceAddress) {
 *         // TODO Auto-generated method stub
 *         return false;
 *     }
 *
 * }
 *
 * public static class MyClass implements NetworkTopologyDiscovery.Factory.ClassDelegate {
 *     public MyClass() {
 *         super();
 *         NetworkTopologyDiscovery.Factory.setClassDelegate(this);
 *
 *         // Access JmDNS or JmmDNS
 *     }
 *
 *     &#064;Override
 *     public NetworkTopologyDiscovery newNetworkTopologyDiscovery() {
 *         return new MyNetworkTopologyDiscovery();
 *     }
 *
 * }
 * </pre>
 *
 * </p>
 *
 * @author Pierre Frisch
 */
public interface NetworkTopologyDiscovery {

    /**
     * NetworkTopologyDiscovery.Factory enable the creation of new instance of NetworkTopologyDiscovery.
     */
    public static final class Factory {
        private static volatile NetworkTopologyDiscovery _instance;

        /**
         * This interface defines a delegate to the NetworkTopologyDiscovery.Factory class to enable subclassing.
         */
        public static interface ClassDelegate {

            /**
             * Allows the delegate the opportunity to construct and return a different NetworkTopologyDiscovery.
             *
             * @return Should return a new NetworkTopologyDiscovery Object.
             * @see #classDelegate()
             * @see #setClassDelegate(ClassDelegate anObject)
             */
            public NetworkTopologyDiscovery newNetworkTopologyDiscovery();
        }

        private static final AtomicReference<Factory.ClassDelegate> _databaseClassDelegate = new AtomicReference<Factory.ClassDelegate>();

        private Factory() {
            super();
        }

        /**
         * Assigns <code>delegate</code> as NetworkTopologyDiscovery's class delegate. The class delegate is optional.
         *
         * @param delegate
         *            The object to set as NetworkTopologyDiscovery's class delegate.
         * @see #classDelegate()
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static void setClassDelegate(Factory.ClassDelegate delegate) {
            _databaseClassDelegate.set(delegate);
        }

        /**
         * Returns NetworkTopologyDiscovery's class delegate.
         *
         * @return NetworkTopologyDiscovery's class delegate.
         * @see #setClassDelegate(ClassDelegate anObject)
         * @see JmmDNS.Factory.ClassDelegate
         */
        public static Factory.ClassDelegate classDelegate() {
            return _databaseClassDelegate.get();
        }

        /**
         * Returns a new instance of NetworkTopologyDiscovery using the class delegate if it exists.
         *
         * @return new instance of NetworkTopologyDiscovery
         */
        protected static NetworkTopologyDiscovery newNetworkTopologyDiscovery() {
            NetworkTopologyDiscovery instance = null;
            Factory.ClassDelegate delegate = _databaseClassDelegate.get();
            if (delegate != null) {
                instance = delegate.newNetworkTopologyDiscovery();
            }
            return (instance != null ? instance : new NetworkTopologyDiscoveryImpl());
        }

        /**
         * Return the instance of the Multihomed Multicast DNS.
         *
         * @return the JmmDNS
         */
        public static NetworkTopologyDiscovery getInstance() {
            if (_instance == null) {
                synchronized (NetworkTopologyDiscovery.Factory.class) {
                    if (_instance == null) {
                        _instance = NetworkTopologyDiscovery.Factory.newNetworkTopologyDiscovery();
                    }
                }
            }
            return _instance;
        }
    }

    /**
     * Get all local Internet Addresses for the machine.
     *
     * @return Set of InetAddress
     */
    public abstract InetAddress[] getInetAddresses();

    /**
     * Check if a given InetAddress should be used for mDNS
     *
     * @param networkInterface
     * @param interfaceAddress
     * @return <code>true</code> is the address is to be used, <code>false</code> otherwise.
     */
    public boolean useInetAddress(NetworkInterface networkInterface, InetAddress interfaceAddress);

    /**
     * Locks the given InetAddress if the device requires it.
     *
     * @param interfaceAddress
     */
    public void lockInetAddress(InetAddress interfaceAddress);

    /**
     * Locks the given InetAddress if the device requires it.
     *
     * @param interfaceAddress
     */
    public void unlockInetAddress(InetAddress interfaceAddress);

}