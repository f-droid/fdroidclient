// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * HostInfo information on the local host to be able to cope with change of addresses.
 *
 * @author Pierre Frisch, Werner Randelshofer
 */
public class HostInfo implements DNSStatefulObject {
    private static Logger       logger = Logger.getLogger(HostInfo.class.getName());

    protected String            _name;

    protected InetAddress       _address;

    protected NetworkInterface  _interfaze;

    private final HostInfoState _state;

    private final static class HostInfoState extends DNSStatefulObject.DefaultImplementation {

        private static final long serialVersionUID = -8191476803620402088L;

        /**
         * @param dns
         */
        public HostInfoState(JmDNSImpl dns) {
            super();
            this.setDns(dns);
        }

    }

    /**
     * @param address
     *            IP address to bind
     * @param dns
     *            JmDNS instance
     * @param jmdnsName
     *            JmDNS name
     * @return new HostInfo
     */
    public static HostInfo newHostInfo(InetAddress address, JmDNSImpl dns, String jmdnsName) {
        HostInfo localhost = null;
        String aName = (jmdnsName != null ? jmdnsName : "");
        InetAddress addr = address;
        try {
            if (addr == null) {
                String ip = System.getProperty("net.mdns.interface");
                if (ip != null) {
                    addr = InetAddress.getByName(ip);
                } else {
                    addr = InetAddress.getLocalHost();
                    if (addr.isLoopbackAddress()) {
                        // Find local address that isn't a loopback address
                        InetAddress[] addresses = NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses();
                        if (addresses.length > 0) {
                            addr = addresses[0];
                        }
                    }
                }
                if (addr.isLoopbackAddress()) {
                    logger.warning("Could not find any address beside the loopback.");
                }
            }
            if (aName.length() == 0) {
                aName = addr.getHostName();
            }
            if (aName.contains("in-addr.arpa") || (aName.equals(addr.getHostAddress()))) {
                aName = ((jmdnsName != null) && (jmdnsName.length() > 0) ? jmdnsName : addr.getHostAddress());
            }
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Could not intialize the host network interface on " + address + "because of an error: " + e.getMessage(), e);
            // This is only used for running unit test on Debian / Ubuntu
            addr = loopbackAddress();
            aName = ((jmdnsName != null) && (jmdnsName.length() > 0) ? jmdnsName : "computer");
        }
        // A host name with "." is illegal. so strip off everything and append .local.
        // We also need to be carefull that the .local may already be there
        int index = aName.indexOf(".local");
        if (index > 0) {
            aName = aName.substring(0, index);
        }
        aName = aName.replace('.', '-');
        aName += ".local.";
        localhost = new HostInfo(addr, aName, dns);
        return localhost;
    }

    private static InetAddress loopbackAddress() {
        try {
            return InetAddress.getByName(null);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private HostInfo(final InetAddress address, final String name, final JmDNSImpl dns) {
        super();
        this._state = new HostInfoState(dns);
        this._address = address;
        this._name = name;
        if (address != null) {
            try {
                _interfaze = NetworkInterface.getByInetAddress(address);
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "LocalHostInfo() exception ", exception);
            }
        }
    }

    public String getName() {
        return _name;
    }

    public InetAddress getInetAddress() {
        return _address;
    }

    Inet4Address getInet4Address() {
        if (this.getInetAddress() instanceof Inet4Address) {
            return (Inet4Address) _address;
        }
        return null;
    }

    Inet6Address getInet6Address() {
        if (this.getInetAddress() instanceof Inet6Address) {
            return (Inet6Address) _address;
        }
        return null;
    }

    public NetworkInterface getInterface() {
        return _interfaze;
    }

    public boolean conflictWithRecord(DNSRecord.Address record) {
        DNSRecord.Address hostAddress = this.getDNSAddressRecord(record.getRecordType(), record.isUnique(), DNSConstants.DNS_TTL);
        if (hostAddress != null) {
            return hostAddress.sameType(record) && hostAddress.sameName(record) && (!hostAddress.sameValue(record));
        }
        return false;
    }

    synchronized String incrementHostName() {
        _name = NameRegister.Factory.getRegistry().incrementName(this.getInetAddress(), _name, NameRegister.NameType.HOST);
        return _name;
    }

    boolean shouldIgnorePacket(DatagramPacket packet) {
        boolean result = false;
        if (this.getInetAddress() != null) {
            InetAddress from = packet.getAddress();
            if (from != null) {
                if ((this.getInetAddress().isLinkLocalAddress() || this.getInetAddress().isMCLinkLocal()) && (!from.isLinkLocalAddress())) {
                    // A host sending Multicast DNS queries to a link-local destination
                    // address (including the 224.0.0.251 and FF02::FB link-local multicast
                    // addresses) MUST only accept responses to that query that originate
                    // from the local link, and silently discard any other response packets.
                    // Without this check, it could be possible for remote rogue hosts to
                    // send spoof answer packets (perhaps unicast to the victim host) which
                    // the receiving machine could misinterpret as having originated on the
                    // local link.
                    result = true;
                }
                // if (from.isLinkLocalAddress() && (!this.getInetAddress().isLinkLocalAddress())) {
                // // Ignore linklocal packets on regular interfaces, unless this is
                // // also a linklocal interface. This is to avoid duplicates. This is
                // // a terrible hack caused by the lack of an API to get the address
                // // of the interface on which the packet was received.
                // result = true;
                // }
                if (from.isLoopbackAddress() && (!this.getInetAddress().isLoopbackAddress())) {
                    // Ignore loopback packets on a regular interface unless this is also a loopback interface.
                    result = true;
                }
            }
        }
        return result;
    }

    DNSRecord.Address getDNSAddressRecord(DNSRecordType type, boolean unique, int ttl) {
        switch (type) {
            case TYPE_A:
                return this.getDNS4AddressRecord(unique, ttl);
            case TYPE_A6:
            case TYPE_AAAA:
                return this.getDNS6AddressRecord(unique, ttl);
            default:
        }
        return null;
    }

    private DNSRecord.Address getDNS4AddressRecord(boolean unique, int ttl) {
        if (this.getInetAddress() instanceof Inet4Address) {
            return new DNSRecord.IPv4Address(this.getName(), DNSRecordClass.CLASS_IN, unique, ttl, this.getInetAddress());
        }
        return null;
    }

    private DNSRecord.Address getDNS6AddressRecord(boolean unique, int ttl) {
        if (this.getInetAddress() instanceof Inet6Address) {
            return new DNSRecord.IPv6Address(this.getName(), DNSRecordClass.CLASS_IN, unique, ttl, this.getInetAddress());
        }
        return null;
    }

    DNSRecord.Pointer getDNSReverseAddressRecord(DNSRecordType type, boolean unique, int ttl) {
        switch (type) {
            case TYPE_A:
                return this.getDNS4ReverseAddressRecord(unique, ttl);
            case TYPE_A6:
            case TYPE_AAAA:
                return this.getDNS6ReverseAddressRecord(unique, ttl);
            default:
        }
        return null;
    }

    private DNSRecord.Pointer getDNS4ReverseAddressRecord(boolean unique, int ttl) {
        if (this.getInetAddress() instanceof Inet4Address) {
            return new DNSRecord.Pointer(this.getInetAddress().getHostAddress() + ".in-addr.arpa.", DNSRecordClass.CLASS_IN, unique, ttl, this.getName());
        }
        return null;
    }

    private DNSRecord.Pointer getDNS6ReverseAddressRecord(boolean unique, int ttl) {
        if (this.getInetAddress() instanceof Inet6Address) {
            return new DNSRecord.Pointer(this.getInetAddress().getHostAddress() + ".ip6.arpa.", DNSRecordClass.CLASS_IN, unique, ttl, this.getName());
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(1024);
        buf.append("local host info[");
        buf.append(getName() != null ? getName() : "no name");
        buf.append(", ");
        buf.append(getInterface() != null ? getInterface().getDisplayName() : "???");
        buf.append(":");
        buf.append(getInetAddress() != null ? getInetAddress().getHostAddress() : "no address");
        buf.append(", ");
        buf.append(_state);
        buf.append("]");
        return buf.toString();
    }

    public Collection<DNSRecord> answers(DNSRecordClass recordClass, boolean unique, int ttl) {
        List<DNSRecord> list = new ArrayList<DNSRecord>();
        DNSRecord answer = this.getDNS4AddressRecord(unique, ttl);
        if ((answer != null) && answer.matchRecordClass(recordClass)) {
            list.add(answer);
        }
        answer = this.getDNS6AddressRecord(unique, ttl);
        if ((answer != null) && answer.matchRecordClass(recordClass)) {
            list.add(answer);
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmDNSImpl getDns() {
        return this._state.getDns();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean advanceState(DNSTask task) {
        return this._state.advanceState(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAssociationWithTask(DNSTask task) {
        this._state.removeAssociationWithTask(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean revertState() {
        return this._state.revertState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void associateWithTask(DNSTask task, DNSState state) {
        this._state.associateWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAssociatedWithTask(DNSTask task, DNSState state) {
        return this._state.isAssociatedWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancelState() {
        return this._state.cancelState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeState() {
        return this._state.closeState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean recoverState() {
        return this._state.recoverState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProbing() {
        return this._state.isProbing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnouncing() {
        return this._state.isAnnouncing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnounced() {
        return this._state.isAnnounced();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceling() {
        return this._state.isCanceling();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled() {
        return this._state.isCanceled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosing() {
        return this._state.isClosing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return this._state.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForAnnounced(long timeout) {
        return _state.waitForAnnounced(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForCanceled(long timeout) {
        if (_address == null) {
            // No need to wait this was never announced.
            return true;
        }
        return _state.waitForCanceled(timeout);
    }

}
