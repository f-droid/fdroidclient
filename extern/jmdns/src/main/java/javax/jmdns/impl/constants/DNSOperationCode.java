/**
 *
 */
package javax.jmdns.impl.constants;

/**
 * DNS operation code.
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSOperationCode {
    /**
     * Query [RFC1035]
     */
    Query("Query", 0),
    /**
     * IQuery (Inverse Query, Obsolete) [RFC3425]
     */
    IQuery("Inverse Query", 1),
    /**
     * Status [RFC1035]
     */
    Status("Status", 2),
    /**
     * Unassigned
     */
    Unassigned("Unassigned", 3),
    /**
     * Notify [RFC1996]
     */
    Notify("Notify", 4),
    /**
     * Update [RFC2136]
     */
    Update("Update", 5);

    /**
     * DNS RCode types are encoded on the last 4 bits
     */
    static final int     OpCode_MASK = 0x7800;

    private final String _externalName;

    private final int    _index;

    DNSOperationCode(String name, int index) {
        _externalName = name;
        _index = index;
    }

    /**
     * Return the string representation of this type
     * 
     * @return String
     */
    public String externalName() {
        return _externalName;
    }

    /**
     * Return the numeric value of this type
     * 
     * @return String
     */
    public int indexValue() {
        return _index;
    }

    /**
     * @param flags
     * @return label
     */
    public static DNSOperationCode operationCodeForFlags(int flags) {
        int maskedIndex = (flags & OpCode_MASK) >> 11;
        for (DNSOperationCode aCode : DNSOperationCode.values()) {
            if (aCode._index == maskedIndex) return aCode;
        }
        return Unassigned;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
