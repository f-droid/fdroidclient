// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSRecord.Pointer;
import javax.jmdns.impl.DNSRecord.Service;
import javax.jmdns.impl.DNSRecord.Text;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * JmDNS service information.
 *
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer
 */
public class ServiceInfoImpl extends ServiceInfo implements DNSListener, DNSStatefulObject {
    private static Logger           logger = Logger.getLogger(ServiceInfoImpl.class.getName());

    private String                  _domain;
    private String                  _protocol;
    private String                  _application;
    private String                  _name;
    private String                  _subtype;
    private String                  _server;
    private int                     _port;
    private int                     _weight;
    private int                     _priority;
    private byte                    _text[];
    private Map<String, byte[]>     _props;
    private final Set<Inet4Address> _ipv4Addresses;
    private final Set<Inet6Address> _ipv6Addresses;

    private transient String        _key;

    private boolean                 _persistent;
    private boolean                 _needTextAnnouncing;

    private final ServiceInfoState  _state;

    private Delegate                _delegate;

    public static interface Delegate {

        public void textValueUpdated(ServiceInfo target, byte[] value);

    }

    private final static class ServiceInfoState extends DNSStatefulObject.DefaultImplementation {

        private static final long     serialVersionUID = 1104131034952196820L;

        private final ServiceInfoImpl _info;

        /**
         * @param info
         */
        public ServiceInfoState(ServiceInfoImpl info) {
            super();
            _info = info;
        }

        @Override
        protected void setTask(DNSTask task) {
            super.setTask(task);
            if ((this._task == null) && _info.needTextAnnouncing()) {
                this.lock();
                try {
                    if ((this._task == null) && _info.needTextAnnouncing()) {
                        if (this._state.isAnnounced()) {
                            this.setState(DNSState.ANNOUNCING_1);
                            if (this.getDns() != null) {
                                this.getDns().startAnnouncer();
                            }
                        }
                        _info.setNeedTextAnnouncing(false);
                    }
                } finally {
                    this.unlock();
                }
            }
        }

        @Override
        public void setDns(JmDNSImpl dns) {
            super.setDns(dns);
        }

    }

    /**
     * @param type
     * @param name
     * @param subtype
     * @param port
     * @param weight
     * @param priority
     * @param persistent
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, String)
     */
    public ServiceInfoImpl(String type, String name, String subtype, int port, int weight, int priority, boolean persistent, String text) {
        this(ServiceInfoImpl.decodeQualifiedNameMap(type, name, subtype), port, weight, priority, persistent, (byte[]) null);
        _server = text;

        byte[] encodedText = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(256);
            ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
            writeUTF(out2, text);
            byte data[] = out2.toByteArray();
            if (data.length > 255) {
                throw new IOException("Cannot have individual values larger that 255 chars. Offending value: " + text);
            }
            out.write((byte) data.length);
            out.write(data, 0, data.length);
            encodedText = out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("unexpected exception: " + e);
        }
        this._text = (encodedText != null && encodedText.length > 0 ? encodedText : DNSRecord.EMPTY_TXT);
    }

    /**
     * @param type
     * @param name
     * @param subtype
     * @param port
     * @param weight
     * @param priority
     * @param persistent
     * @param props
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, Map)
     */
    public ServiceInfoImpl(String type, String name, String subtype, int port, int weight, int priority, boolean persistent, Map<String, ?> props) {
        this(ServiceInfoImpl.decodeQualifiedNameMap(type, name, subtype), port, weight, priority, persistent, textFromProperties(props));
    }

    /**
     * @param type
     * @param name
     * @param subtype
     * @param port
     * @param weight
     * @param priority
     * @param persistent
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, byte[])
     */
    public ServiceInfoImpl(String type, String name, String subtype, int port, int weight, int priority, boolean persistent, byte text[]) {
        this(ServiceInfoImpl.decodeQualifiedNameMap(type, name, subtype), port, weight, priority, persistent, text);
    }

    public ServiceInfoImpl(Map<Fields, String> qualifiedNameMap, int port, int weight, int priority, boolean persistent, Map<String, ?> props) {
        this(qualifiedNameMap, port, weight, priority, persistent, textFromProperties(props));
    }

    ServiceInfoImpl(Map<Fields, String> qualifiedNameMap, int port, int weight, int priority, boolean persistent, String text) {
        this(qualifiedNameMap, port, weight, priority, persistent, (byte[]) null);
        _server = text;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            writeUTF(out, text);
            this._text = out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("unexpected exception: " + e);
        }
    }

    ServiceInfoImpl(Map<Fields, String> qualifiedNameMap, int port, int weight, int priority, boolean persistent, byte text[]) {
        Map<Fields, String> map = ServiceInfoImpl.checkQualifiedNameMap(qualifiedNameMap);

        this._domain = map.get(Fields.Domain);
        this._protocol = map.get(Fields.Protocol);
        this._application = map.get(Fields.Application);
        this._name = map.get(Fields.Instance);
        this._subtype = map.get(Fields.Subtype);

        this._port = port;
        this._weight = weight;
        this._priority = priority;
        this._text = text;
        this.setNeedTextAnnouncing(false);
        this._state = new ServiceInfoState(this);
        this._persistent = persistent;
        this._ipv4Addresses = Collections.synchronizedSet(new LinkedHashSet<Inet4Address>());
        this._ipv6Addresses = Collections.synchronizedSet(new LinkedHashSet<Inet6Address>());
    }

    /**
     * During recovery we need to duplicate service info to reregister them
     *
     * @param info
     */
    ServiceInfoImpl(ServiceInfo info) {
        this._ipv4Addresses = Collections.synchronizedSet(new LinkedHashSet<Inet4Address>());
        this._ipv6Addresses = Collections.synchronizedSet(new LinkedHashSet<Inet6Address>());
        if (info != null) {
            this._domain = info.getDomain();
            this._protocol = info.getProtocol();
            this._application = info.getApplication();
            this._name = info.getName();
            this._subtype = info.getSubtype();
            this._port = info.getPort();
            this._weight = info.getWeight();
            this._priority = info.getPriority();
            this._text = info.getTextBytes();
            this._persistent = info.isPersistent();
            Inet6Address[] ipv6Addresses = info.getInet6Addresses();
            for (Inet6Address address : ipv6Addresses) {
                this._ipv6Addresses.add(address);
            }
            Inet4Address[] ipv4Addresses = info.getInet4Addresses();
            for (Inet4Address address : ipv4Addresses) {
                this._ipv4Addresses.add(address);
            }
        }
        this._state = new ServiceInfoState(this);
    }

    public static Map<Fields, String> decodeQualifiedNameMap(String type, String name, String subtype) {
        Map<Fields, String> qualifiedNameMap = decodeQualifiedNameMapForType(type);

        qualifiedNameMap.put(Fields.Instance, name);
        qualifiedNameMap.put(Fields.Subtype, subtype);

        return checkQualifiedNameMap(qualifiedNameMap);
    }

    public static Map<Fields, String> decodeQualifiedNameMapForType(String type) {
        int index;

        String casePreservedType = type;

        String aType = type.toLowerCase();
        String application = aType;
        String protocol = "";
        String subtype = "";
        String name = "";
        String domain = "";

        if (aType.contains("in-addr.arpa") || aType.contains("ip6.arpa")) {
            index = (aType.contains("in-addr.arpa") ? aType.indexOf("in-addr.arpa") : aType.indexOf("ip6.arpa"));
            name = removeSeparators(casePreservedType.substring(0, index));
            domain = casePreservedType.substring(index);
            application = "";
        } else if ((!aType.contains("_")) && aType.contains(".")) {
            index = aType.indexOf('.');
            name = removeSeparators(casePreservedType.substring(0, index));
            domain = removeSeparators(casePreservedType.substring(index));
            application = "";
        } else {
            // First remove the name if it there.
            if (!aType.startsWith("_") || aType.startsWith("_services")) {
                index = aType.indexOf("._");
                if (index > 0) {
                    // We need to preserve the case for the user readable name.
                    name = casePreservedType.substring(0, index);
                    if (index + 1 < aType.length()) {
                        aType = aType.substring(index + 1);
                        casePreservedType = casePreservedType.substring(index + 1);
                    }
                }
            }

            index = aType.lastIndexOf("._");
            if (index > 0) {
                int start = index + 2;
                int end = aType.indexOf('.', start);
                protocol = casePreservedType.substring(start, end);
            }
            if (protocol.length() > 0) {
                index = aType.indexOf("_" + protocol.toLowerCase() + ".");
                int start = index + protocol.length() + 2;
                int end = aType.length() - (aType.endsWith(".") ? 1 : 0);
                if (end > start) {
                    domain = casePreservedType.substring(start, end);
                }
                if (index > 0) {
                    application = casePreservedType.substring(0, index - 1);
                } else {
                    application = "";
                }
            }
            index = application.toLowerCase().indexOf("._sub");
            if (index > 0) {
                int start = index + 5;
                subtype = removeSeparators(application.substring(0, index));
                application = application.substring(start);
            }
        }

        final Map<Fields, String> qualifiedNameMap = new HashMap<Fields, String>(5);
        qualifiedNameMap.put(Fields.Domain, removeSeparators(domain));
        qualifiedNameMap.put(Fields.Protocol, protocol);
        qualifiedNameMap.put(Fields.Application, removeSeparators(application));
        qualifiedNameMap.put(Fields.Instance, name);
        qualifiedNameMap.put(Fields.Subtype, subtype);

        return qualifiedNameMap;
    }

    protected static Map<Fields, String> checkQualifiedNameMap(Map<Fields, String> qualifiedNameMap) {
        Map<Fields, String> checkedQualifiedNameMap = new HashMap<Fields, String>(5);

        // Optional domain
        String domain = (qualifiedNameMap.containsKey(Fields.Domain) ? qualifiedNameMap.get(Fields.Domain) : "local");
        if ((domain == null) || (domain.length() == 0)) {
            domain = "local";
        }
        domain = removeSeparators(domain);
        checkedQualifiedNameMap.put(Fields.Domain, domain);
        // Optional protocol
        String protocol = (qualifiedNameMap.containsKey(Fields.Protocol) ? qualifiedNameMap.get(Fields.Protocol) : "tcp");
        if ((protocol == null) || (protocol.length() == 0)) {
            protocol = "tcp";
        }
        protocol = removeSeparators(protocol);
        checkedQualifiedNameMap.put(Fields.Protocol, protocol);
        // Application
        String application = (qualifiedNameMap.containsKey(Fields.Application) ? qualifiedNameMap.get(Fields.Application) : "");
        if ((application == null) || (application.length() == 0)) {
            application = "";
        }
        application = removeSeparators(application);
        checkedQualifiedNameMap.put(Fields.Application, application);
        // Instance
        String instance = (qualifiedNameMap.containsKey(Fields.Instance) ? qualifiedNameMap.get(Fields.Instance) : "");
        if ((instance == null) || (instance.length() == 0)) {
            instance = "";
            // throw new IllegalArgumentException("The instance name component of a fully qualified service cannot be empty.");
        }
        instance = removeSeparators(instance);
        checkedQualifiedNameMap.put(Fields.Instance, instance);
        // Optional Subtype
        String subtype = (qualifiedNameMap.containsKey(Fields.Subtype) ? qualifiedNameMap.get(Fields.Subtype) : "");
        if ((subtype == null) || (subtype.length() == 0)) {
            subtype = "";
        }
        subtype = removeSeparators(subtype);
        checkedQualifiedNameMap.put(Fields.Subtype, subtype);

        return checkedQualifiedNameMap;
    }

    private static String removeSeparators(String name) {
        if (name == null) {
            return "";
        }
        String newName = name.trim();
        if (newName.startsWith(".")) {
            newName = newName.substring(1);
        }
        if (newName.startsWith("_")) {
            newName = newName.substring(1);
        }
        if (newName.endsWith(".")) {
            newName = newName.substring(0, newName.length() - 1);
        }
        return newName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        String domain = this.getDomain();
        String protocol = this.getProtocol();
        String application = this.getApplication();
        return (application.length() > 0 ? "_" + application + "." : "") + (protocol.length() > 0 ? "_" + protocol + "." : "") + domain + ".";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeWithSubtype() {
        String subtype = this.getSubtype();
        return (subtype.length() > 0 ? "_" + subtype.toLowerCase() + "._sub." : "") + this.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return (_name != null ? _name : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        if (this._key == null) {
            this._key = this.getQualifiedName().toLowerCase();
        }
        return this._key;
    }

    /**
     * Sets the service instance name.
     *
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     */
    void setName(String name) {
        this._name = name;
        this._key = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQualifiedName() {
        String domain = this.getDomain();
        String protocol = this.getProtocol();
        String application = this.getApplication();
        String instance = this.getName();
        // String subtype = this.getSubtype();
        // return (instance.length() > 0 ? instance + "." : "") + (application.length() > 0 ? "_" + application + "." : "") + (protocol.length() > 0 ? "_" + protocol + (subtype.length() > 0 ? ",_" + subtype.toLowerCase() + "." : ".") : "") + domain
        // + ".";
        return (instance.length() > 0 ? instance + "." : "") + (application.length() > 0 ? "_" + application + "." : "") + (protocol.length() > 0 ? "_" + protocol + "." : "") + domain + ".";
    }

    /**
     * @see javax.jmdns.ServiceInfo#getServer()
     */
    @Override
    public String getServer() {
        return (_server != null ? _server : "");
    }

    /**
     * @param server
     *            the server to set
     */
    void setServer(String server) {
        this._server = server;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public String getHostAddress() {
        String[] names = this.getHostAddresses();
        return (names.length > 0 ? names[0] : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getHostAddresses() {
        Inet4Address[] ip4Aaddresses = this.getInet4Addresses();
        Inet6Address[] ip6Aaddresses = this.getInet6Addresses();
        String[] names = new String[ip4Aaddresses.length + ip6Aaddresses.length];
        for (int i = 0; i < ip4Aaddresses.length; i++) {
            names[i] = ip4Aaddresses[i].getHostAddress();
        }
        for (int i = 0; i < ip6Aaddresses.length; i++) {
            names[i + ip4Aaddresses.length] = "[" + ip6Aaddresses[i].getHostAddress() + "]";
        }
        return names;
    }

    /**
     * @param addr
     *            the addr to add
     */
    void addAddress(Inet4Address addr) {
        _ipv4Addresses.add(addr);
    }

    /**
     * @param addr
     *            the addr to add
     */
    void addAddress(Inet6Address addr) {
        _ipv6Addresses.add(addr);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public InetAddress getAddress() {
        return this.getInetAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public InetAddress getInetAddress() {
        InetAddress[] addresses = this.getInetAddresses();
        return (addresses.length > 0 ? addresses[0] : null);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public Inet4Address getInet4Address() {
        Inet4Address[] addresses = this.getInet4Addresses();
        return (addresses.length > 0 ? addresses[0] : null);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public Inet6Address getInet6Address() {
        Inet6Address[] addresses = this.getInet6Addresses();
        return (addresses.length > 0 ? addresses[0] : null);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getInetAddresses()
     */
    @Override
    public InetAddress[] getInetAddresses() {
        List<InetAddress> aList = new ArrayList<InetAddress>(_ipv4Addresses.size() + _ipv6Addresses.size());
        aList.addAll(_ipv4Addresses);
        aList.addAll(_ipv6Addresses);
        return aList.toArray(new InetAddress[aList.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getInet4Addresses()
     */
    @Override
    public Inet4Address[] getInet4Addresses() {
        return _ipv4Addresses.toArray(new Inet4Address[_ipv4Addresses.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getInet6Addresses()
     */
    @Override
    public Inet6Address[] getInet6Addresses() {
        return _ipv6Addresses.toArray(new Inet6Address[_ipv6Addresses.size()]);
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPort()
     */
    @Override
    public int getPort() {
        return _port;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPriority()
     */
    @Override
    public int getPriority() {
        return _priority;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getWeight()
     */
    @Override
    public int getWeight() {
        return _weight;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getTextBytes()
     */
    @Override
    public byte[] getTextBytes() {
        return (this._text != null && this._text.length > 0 ? this._text : DNSRecord.EMPTY_TXT);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public String getTextString() {
        Map<String, byte[]> properties = this.getProperties();
        for (String key : properties.keySet()) {
            byte[] value = properties.get(key);
            if ((value != null) && (value.length > 0)) {
                return key + "=" + new String(value);
            }
            return key;
        }
        return "";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getURL()
     */
    @Deprecated
    @Override
    public String getURL() {
        return this.getURL("http");
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getURLs()
     */
    @Override
    public String[] getURLs() {
        return this.getURLs("http");
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getURL(java.lang.String)
     */
    @Deprecated
    @Override
    public String getURL(String protocol) {
        String[] urls = this.getURLs(protocol);
        return (urls.length > 0 ? urls[0] : protocol + "://null:" + getPort());
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#getURLs(java.lang.String)
     */
    @Override
    public String[] getURLs(String protocol) {
        InetAddress[] addresses = this.getInetAddresses();
        List<String> urls = new ArrayList<String>(addresses.length);
        for (InetAddress address : addresses) {
            String hostAddress = address.getHostAddress();
            if (address instanceof Inet6Address) {
                hostAddress = "[" + hostAddress + "]";
            }
            String url = protocol + "://" + hostAddress + ":" + getPort();
            String path = getPropertyString("path");
            if (path != null) {
                if (path.indexOf("://") >= 0) {
                    url = path;
                } else {
                    url += path.startsWith("/") ? path : "/" + path;
                }
            }
            urls.add(url);
        }
        return urls.toArray(new String[urls.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized byte[] getPropertyBytes(String name) {
        return this.getProperties().get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getPropertyString(String name) {
        byte data[] = this.getProperties().get(name);
        if (data == null) {
            return null;
        }
        if (data == NO_VALUE) {
            return "true";
        }
        return readUTF(data, 0, data.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getPropertyNames() {
        Map<String, byte[]> properties = this.getProperties();
        Collection<String> names = (properties != null ? properties.keySet() : Collections.<String> emptySet());
        return new Vector<String>(names).elements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApplication() {
        return (_application != null ? _application : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomain() {
        return (_domain != null ? _domain : "local");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {
        return (_protocol != null ? _protocol : "tcp");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubtype() {
        return (_subtype != null ? _subtype : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Fields, String> getQualifiedNameMap() {
        Map<Fields, String> map = new HashMap<Fields, String>(5);

        map.put(Fields.Domain, this.getDomain());
        map.put(Fields.Protocol, this.getProtocol());
        map.put(Fields.Application, this.getApplication());
        map.put(Fields.Instance, this.getName());
        map.put(Fields.Subtype, this.getSubtype());
        return map;
    }

    /**
     * Write a UTF string with a length to a stream.
     */
    static void writeUTF(OutputStream out, String str) throws IOException {
        for (int i = 0, len = str.length(); i < len; i++) {
            int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                out.write(c);
            } else {
                if (c > 0x07FF) {
                    out.write(0xE0 | ((c >> 12) & 0x0F));
                    out.write(0x80 | ((c >> 6) & 0x3F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                } else {
                    out.write(0xC0 | ((c >> 6) & 0x1F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
            }
        }
    }

    /**
     * Read data bytes as a UTF stream.
     */
    String readUTF(byte data[], int off, int len) {
        int offset = off;
        StringBuffer buf = new StringBuffer();
        for (int end = offset + len; offset < end;) {
            int ch = data[offset++] & 0xFF;
            switch (ch >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    break;
                case 12:
                case 13:
                    if (offset >= len) {
                        return null;
                    }
                    // 110x xxxx 10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (data[offset++] & 0x3F);
                    break;
                case 14:
                    if (offset + 2 >= len) {
                        return null;
                    }
                    // 1110 xxxx 10xx xxxx 10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((data[offset++] & 0x3F) << 6) | (data[offset++] & 0x3F);
                    break;
                default:
                    if (offset + 1 >= len) {
                        return null;
                    }
                    // 10xx xxxx, 1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (data[offset++] & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
        return buf.toString();
    }

    synchronized Map<String, byte[]> getProperties() {
        if ((_props == null) && (this.getTextBytes() != null)) {
            Hashtable<String, byte[]> properties = new Hashtable<String, byte[]>();
            try {
                int off = 0;
                while (off < getTextBytes().length) {
                    // length of the next key value pair
                    int len = getTextBytes()[off++] & 0xFF;
                    if ((len == 0) || (off + len > getTextBytes().length)) {
                        properties.clear();
                        break;
                    }
                    // look for the '='
                    int i = 0;
                    for (; (i < len) && (getTextBytes()[off + i] != '='); i++) {
                        /* Stub */
                    }

                    // get the property name
                    String name = readUTF(getTextBytes(), off, i);
                    if (name == null) {
                        properties.clear();
                        break;
                    }
                    if (i == len) {
                        properties.put(name, NO_VALUE);
                    } else {
                        byte value[] = new byte[len - ++i];
                        System.arraycopy(getTextBytes(), off + i, value, 0, len - i);
                        properties.put(name, value);
                        off += len;
                    }
                }
            } catch (Exception exception) {
                // We should get better logging.
                logger.log(Level.WARNING, "Malformed TXT Field ", exception);
            }
            this._props = properties;
        }
        return (_props != null ? _props : Collections.<String, byte[]> emptyMap());
    }

    /**
     * JmDNS callback to update a DNS record.
     *
     * @param dnsCache
     * @param now
     * @param rec
     */
    @Override
    public void updateRecord(DNSCache dnsCache, long now, DNSEntry rec) {
        if ((rec instanceof DNSRecord) && !rec.isExpired(now)) {
            boolean serviceUpdated = false;
            switch (rec.getRecordType()) {
                case TYPE_A: // IPv4
                    if (rec.getName().equalsIgnoreCase(this.getServer())) {
                        _ipv4Addresses.add((Inet4Address) ((DNSRecord.Address) rec).getAddress());
                        serviceUpdated = true;
                    }
                    break;
                case TYPE_AAAA: // IPv6
                    if (rec.getName().equalsIgnoreCase(this.getServer())) {
                        _ipv6Addresses.add((Inet6Address) ((DNSRecord.Address) rec).getAddress());
                        serviceUpdated = true;
                    }
                    break;
                case TYPE_SRV:
                    if (rec.getName().equalsIgnoreCase(this.getQualifiedName())) {
                        DNSRecord.Service srv = (DNSRecord.Service) rec;
                        boolean serverChanged = (_server == null) || !_server.equalsIgnoreCase(srv.getServer());
                        _server = srv.getServer();
                        _port = srv.getPort();
                        _weight = srv.getWeight();
                        _priority = srv.getPriority();
                        if (serverChanged) {
                            _ipv4Addresses.clear();
                            _ipv6Addresses.clear();
                            for (DNSEntry entry : dnsCache.getDNSEntryList(_server, DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN)) {
                                this.updateRecord(dnsCache, now, entry);
                            }
                            for (DNSEntry entry : dnsCache.getDNSEntryList(_server, DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN)) {
                                this.updateRecord(dnsCache, now, entry);
                            }
                            // We do not want to trigger the listener in this case as it will be triggered if the address resolves.
                        } else {
                            serviceUpdated = true;
                        }
                    }
                    break;
                case TYPE_TXT:
                    if (rec.getName().equalsIgnoreCase(this.getQualifiedName())) {
                        DNSRecord.Text txt = (DNSRecord.Text) rec;
                        _text = txt.getText();
                        _props = null; // set it null for apply update text data
                        serviceUpdated = true;
                    }
                    break;
                case TYPE_PTR:
                    if ((this.getSubtype().length() == 0) && (rec.getSubtype().length() != 0)) {
                        _subtype = rec.getSubtype();
                        serviceUpdated = true;
                    }
                    break;
                default:
                    break;
            }
            if (serviceUpdated && this.hasData()) {
                JmDNSImpl dns = this.getDns();
                if (dns != null) {
                    // ServiceEvent event = ((DNSRecord) rec).getServiceEvent(dns);
                    // event = new ServiceEventImpl(dns, event.getType(), event.getName(), this);
                    // Failure to resolve services - ID: 3517826
                    ServiceEvent event = new ServiceEventImpl(dns, this.getType(), this.getName(), this);
                    dns.handleServiceResolved(event);
                }
            }
            // This is done, to notify the wait loop in method JmDNS.waitForInfoData(ServiceInfo info, int timeout);
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * Returns true if the service info is filled with data.
     *
     * @return <code>true</code> if the service info has data, <code>false</code> otherwise.
     */
    @Override
    public synchronized boolean hasData() {
        return this.getServer() != null && this.hasInetAddress() && this.getTextBytes() != null && this.getTextBytes().length > 0;
        // return this.getServer() != null && (this.getAddress() != null || (this.getTextBytes() != null && this.getTextBytes().length > 0));
    }

    private final boolean hasInetAddress() {
        return _ipv4Addresses.size() > 0 || _ipv6Addresses.size() > 0;
    }

    // State machine

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean advanceState(DNSTask task) {
        return _state.advanceState(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean revertState() {
        return _state.revertState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancelState() {
        return _state.cancelState();
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
    public void removeAssociationWithTask(DNSTask task) {
        _state.removeAssociationWithTask(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void associateWithTask(DNSTask task, DNSState state) {
        _state.associateWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAssociatedWithTask(DNSTask task, DNSState state) {
        return _state.isAssociatedWithTask(task, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProbing() {
        return _state.isProbing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnouncing() {
        return _state.isAnnouncing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnnounced() {
        return _state.isAnnounced();
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
        return _state.isCanceled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosing() {
        return _state.isClosing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return _state.isClosed();
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
        return _state.waitForCanceled(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getQualifiedName().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ServiceInfoImpl) && getQualifiedName().equals(((ServiceInfoImpl) obj).getQualifiedName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNiceTextString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0, len = this.getTextBytes().length; i < len; i++) {
            if (i >= 200) {
                buf.append("...");
                break;
            }
            int ch = getTextBytes()[i] & 0xFF;
            if ((ch < ' ') || (ch > 127)) {
                buf.append("\\0");
                buf.append(Integer.toString(ch, 8));
            } else {
                buf.append((char) ch);
            }
        }
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceInfo#clone()
     */
    @Override
    public ServiceInfoImpl clone() {
        ServiceInfoImpl serviceInfo = new ServiceInfoImpl(this.getQualifiedNameMap(), _port, _weight, _priority, _persistent, _text);
        Inet6Address[] ipv6Addresses = this.getInet6Addresses();
        for (Inet6Address address : ipv6Addresses) {
            serviceInfo._ipv6Addresses.add(address);
        }
        Inet4Address[] ipv4Addresses = this.getInet4Addresses();
        for (Inet4Address address : ipv4Addresses) {
            serviceInfo._ipv4Addresses.add(address);
        }
        return serviceInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[" + this.getClass().getSimpleName() + "@" + System.identityHashCode(this) + " ");
        buf.append("name: '");
        buf.append((this.getName().length() > 0 ? this.getName() + "." : "") + this.getTypeWithSubtype());
        buf.append("' address: '");
        InetAddress[] addresses = this.getInetAddresses();
        if (addresses.length > 0) {
            for (InetAddress address : addresses) {
                buf.append(address);
                buf.append(':');
                buf.append(this.getPort());
                buf.append(' ');
            }
        } else {
            buf.append("(null):");
            buf.append(this.getPort());
        }
        buf.append("' status: '");
        buf.append(_state.toString());
        buf.append(this.isPersistent() ? "' is persistent," : "',");
        buf.append(" has ");
        buf.append(this.hasData() ? "" : "NO ");
        buf.append("data");
        if (this.getTextBytes().length > 0) {
            // buf.append("\n");
            // buf.append(this.getNiceTextString());
            Map<String, byte[]> properties = this.getProperties();
            if (!properties.isEmpty()) {
                buf.append("\n");
                for (String key : properties.keySet()) {
                    buf.append("\t" + key + ": " + new String(properties.get(key)) + "\n");
                }
            } else {
                buf.append(" empty");
            }
        }
        buf.append(']');
        return buf.toString();
    }

    /**
     * Create a series of answer that correspond with the give service info.
     *
     * @param recordClass
     *            record class of the query
     * @param unique
     * @param ttl
     * @param localHost
     * @return collection of answers
     */
    public Collection<DNSRecord> answers(DNSRecordClass recordClass, boolean unique, int ttl, HostInfo localHost) {
        List<DNSRecord> list = new ArrayList<DNSRecord>();
        // [PJYF Dec 6 2011] This is bad hack as I don't know what the spec should really means in this case. i.e. what is the class of our registered services.
        if ((recordClass == DNSRecordClass.CLASS_ANY) || (recordClass == DNSRecordClass.CLASS_IN)) {
            if (this.getSubtype().length() > 0) {
                list.add(new Pointer(this.getTypeWithSubtype(), DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, ttl, this.getQualifiedName()));
            }
            list.add(new Pointer(this.getType(), DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, ttl, this.getQualifiedName()));
            list.add(new Service(this.getQualifiedName(), DNSRecordClass.CLASS_IN, unique, ttl, _priority, _weight, _port, localHost.getName()));
            list.add(new Text(this.getQualifiedName(), DNSRecordClass.CLASS_IN, unique, ttl, this.getTextBytes()));
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setText(byte[] text) throws IllegalStateException {
        synchronized (this) {
            this._text = text;
            this._props = null;
            this.setNeedTextAnnouncing(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setText(Map<String, ?> props) throws IllegalStateException {
        this.setText(textFromProperties(props));
    }

    /**
     * This is used internally by the framework
     *
     * @param text
     */
    void _setText(byte[] text) {
        this._text = text;
        this._props = null;
    }

    private static byte[] textFromProperties(Map<String, ?> props) {
        byte[] text = null;
        if (props != null) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                for (String key : props.keySet()) {
                    Object val = props.get(key);
                    ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
                    writeUTF(out2, key);
                    if (val == null) {
                        // Skip
                    } else if (val instanceof String) {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    } else if (val instanceof byte[]) {
                        byte[] bval = (byte[]) val;
                        if (bval.length > 0) {
                            out2.write('=');
                            out2.write(bval, 0, bval.length);
                        } else {
                            val = null;
                        }
                    } else {
                        throw new IllegalArgumentException("invalid property value: " + val);
                    }
                    byte data[] = out2.toByteArray();
                    if (data.length > 255) {
                        throw new IOException("Cannot have individual values larger that 255 chars. Offending value: " + key + (val != null ? "" : "=" + val));
                    }
                    out.write((byte) data.length);
                    out.write(data, 0, data.length);
                }
                text = out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
        return (text != null && text.length > 0 ? text : DNSRecord.EMPTY_TXT);
    }

    public void setDns(JmDNSImpl dns) {
        this._state.setDns(dns);
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
    public boolean isPersistent() {
        return _persistent;
    }

    /**
     * @param needTextAnnouncing
     *            the needTextAnnouncing to set
     */
    public void setNeedTextAnnouncing(boolean needTextAnnouncing) {
        this._needTextAnnouncing = needTextAnnouncing;
        if (this._needTextAnnouncing) {
            _state.setTask(null);
        }
    }

    /**
     * @return the needTextAnnouncing
     */
    public boolean needTextAnnouncing() {
        return _needTextAnnouncing;
    }

    /**
     * @return the delegate
     */
    Delegate getDelegate() {
        return this._delegate;
    }

    /**
     * @param delegate
     *            the delegate to set
     */
    void setDelegate(Delegate delegate) {
        this._delegate = delegate;
    }

}
