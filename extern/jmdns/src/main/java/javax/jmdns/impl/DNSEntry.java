// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.jmdns.ServiceInfo.Fields;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * DNS entry with a name, type, and class. This is the base class for questions and records.
 *
 * @author Arthur van Hoff, Pierre Frisch, Rick Blair
 */
public abstract class DNSEntry {
    // private static Logger logger = Logger.getLogger(DNSEntry.class.getName());
    private final String         _key;

    private final String         _name;

    private final String         _type;

    private final DNSRecordType  _recordType;

    private final DNSRecordClass _dnsClass;

    private final boolean        _unique;

    final Map<Fields, String>    _qualifiedNameMap;

    /**
     * Create an entry.
     */
    DNSEntry(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
        _name = name;
        // _key = (name != null ? name.trim().toLowerCase() : null);
        _recordType = type;
        _dnsClass = recordClass;
        _unique = unique;
        _qualifiedNameMap = ServiceInfoImpl.decodeQualifiedNameMapForType(this.getName());
        String domain = _qualifiedNameMap.get(Fields.Domain);
        String protocol = _qualifiedNameMap.get(Fields.Protocol);
        String application = _qualifiedNameMap.get(Fields.Application);
        String instance = _qualifiedNameMap.get(Fields.Instance).toLowerCase();
        _type = (application.length() > 0 ? "_" + application + "." : "") + (protocol.length() > 0 ? "_" + protocol + "." : "") + domain + ".";
        _key = ((instance.length() > 0 ? instance + "." : "") + _type).toLowerCase();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof DNSEntry) {
            DNSEntry other = (DNSEntry) obj;
            result = this.getKey().equals(other.getKey()) && this.getRecordType().equals(other.getRecordType()) && this.getRecordClass() == other.getRecordClass();
        }
        return result;
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     *
     * @param entry
     * @return <code>true</code> if the two entries have are for the same record, <code>false</code> otherwise
     */
    public boolean isSameEntry(DNSEntry entry) {
        return this.getKey().equals(entry.getKey()) && this.matchRecordType(entry.getRecordType()) && this.matchRecordClass(entry.getRecordClass());
    }

    /**
     * Check if two entries have the same subtype.
     *
     * @param other
     * @return <code>true</code> if the two entries have are for the same subtype, <code>false</code> otherwise
     */
    public boolean sameSubtype(DNSEntry other) {
        return this.getSubtype().equals(other.getSubtype());
    }

    /**
     * Check if the requested record class match the current record class
     *
     * @param recordClass
     * @return <code>true</code> if the two entries have compatible class, <code>false</code> otherwise
     */
    public boolean matchRecordClass(DNSRecordClass recordClass) {
        return (DNSRecordClass.CLASS_ANY == recordClass) || (DNSRecordClass.CLASS_ANY == this.getRecordClass()) || this.getRecordClass().equals(recordClass);
    }

    /**
     * Check if the requested record tyep match the current record type
     *
     * @param recordType
     * @return <code>true</code> if the two entries have compatible type, <code>false</code> otherwise
     */
    public boolean matchRecordType(DNSRecordType recordType) {
        return this.getRecordType().equals(recordType);
    }

    /**
     * Returns the subtype of this entry
     *
     * @return subtype of this entry
     */
    public String getSubtype() {
        String subtype = this.getQualifiedNameMap().get(Fields.Subtype);
        return (subtype != null ? subtype : "");
    }

    /**
     * Returns the name of this entry
     *
     * @return name of this entry
     */
    public String getName() {
        return (_name != null ? _name : "");
    }

    /**
     * @return the type
     */
    public String getType() {
        return (_type != null ? _type : "");
    }

    /**
     * Returns the key for this entry. The key is the lower case name.
     *
     * @return key for this entry
     */
    public String getKey() {
        return (_key != null ? _key : "");
    }

    /**
     * @return record type
     */
    public DNSRecordType getRecordType() {
        return (_recordType != null ? _recordType : DNSRecordType.TYPE_IGNORE);
    }

    /**
     * @return record class
     */
    public DNSRecordClass getRecordClass() {
        return (_dnsClass != null ? _dnsClass : DNSRecordClass.CLASS_UNKNOWN);
    }

    /**
     * @return true if unique
     */
    public boolean isUnique() {
        return _unique;
    }

    public Map<Fields, String> getQualifiedNameMap() {
        return Collections.unmodifiableMap(_qualifiedNameMap);
    }

    public boolean isServicesDiscoveryMetaQuery() {
        return _qualifiedNameMap.get(Fields.Application).equals("dns-sd") && _qualifiedNameMap.get(Fields.Instance).equals("_services");
    }

    public boolean isDomainDiscoveryQuery() {
        // b._dns-sd._udp.<domain>.
        // db._dns-sd._udp.<domain>.
        // r._dns-sd._udp.<domain>.
        // dr._dns-sd._udp.<domain>.
        // lb._dns-sd._udp.<domain>.

        if (_qualifiedNameMap.get(Fields.Application).equals("dns-sd")) {
            String name = _qualifiedNameMap.get(Fields.Instance);
            return "b".equals(name) || "db".equals(name) || "r".equals(name) || "dr".equals(name) || "lb".equals(name);
        }
        return false;
    }

    public boolean isReverseLookup() {
        return this.isV4ReverseLookup() || this.isV6ReverseLookup();
    }

    public boolean isV4ReverseLookup() {
        return _qualifiedNameMap.get(Fields.Domain).endsWith("in-addr.arpa");
    }

    public boolean isV6ReverseLookup() {
        return _qualifiedNameMap.get(Fields.Domain).endsWith("ip6.arpa");
    }

    /**
     * Check if the record is stale, i.e. it has outlived more than half of its TTL.
     *
     * @param now
     *            update date
     * @return <code>true</code> is the record is stale, <code>false</code> otherwise.
     */
    public abstract boolean isStale(long now);

    /**
     * Check if the record is expired.
     *
     * @param now
     *            update date
     * @return <code>true</code> is the record is expired, <code>false</code> otherwise.
     */
    public abstract boolean isExpired(long now);

    /**
     * Check that 2 entries are of the same class.
     *
     * @param entry
     * @return <code>true</code> is the two class are the same, <code>false</code> otherwise.
     */
    public boolean isSameRecordClass(DNSEntry entry) {
        return (entry != null) && (entry.getRecordClass() == this.getRecordClass());
    }

    /**
     * Check that 2 entries are of the same type.
     *
     * @param entry
     * @return <code>true</code> is the two type are the same, <code>false</code> otherwise.
     */
    public boolean isSameType(DNSEntry entry) {
        return (entry != null) && (entry.getRecordType() == this.getRecordType());
    }

    /**
     * @param dout
     * @exception IOException
     */
    protected void toByteArray(DataOutputStream dout) throws IOException {
        dout.write(this.getName().getBytes("UTF8"));
        dout.writeShort(this.getRecordType().indexValue());
        dout.writeShort(this.getRecordClass().indexValue());
    }

    /**
     * Creates a byte array representation of this record. This is needed for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @return byte array representation
     */
    protected byte[] toByteArray() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            this.toByteArray(dout);
            dout.close();
            return bout.toByteArray();
        } catch (IOException e) {
            throw new InternalError();
        }
    }

    /**
     * Does a lexicographic comparison of the byte array representation of this record and that record. This is needed for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @param that
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    public int compareTo(DNSEntry that) {
        byte[] thisBytes = this.toByteArray();
        byte[] thatBytes = that.toByteArray();
        for (int i = 0, n = Math.min(thisBytes.length, thatBytes.length); i < n; i++) {
            if (thisBytes[i] > thatBytes[i]) {
                return 1;
            } else if (thisBytes[i] < thatBytes[i]) {
                return -1;
            }
        }
        return thisBytes.length - thatBytes.length;
    }

    /**
     * Overriden, to return a value which is consistent with the value returned by equals(Object).
     */
    @Override
    public int hashCode() {
        return this.getKey().hashCode() + this.getRecordType().indexValue() + this.getRecordClass().indexValue();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder aLog = new StringBuilder(200);
        aLog.append("[" + this.getClass().getSimpleName() + "@" + System.identityHashCode(this));
        aLog.append(" type: " + this.getRecordType());
        aLog.append(", class: " + this.getRecordClass());
        aLog.append((_unique ? "-unique," : ","));
        aLog.append(" name: " + _name);
        this.toString(aLog);
        aLog.append("]");
        return aLog.toString();
    }

    /**
     * @param aLog
     */
    protected void toString(StringBuilder aLog) {
        // Stub
    }

}
