/**
 *
 */
package javax.jmdns.impl.constants;

/**
 * DNS result code.
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSResultCode {
    /**
     * Token
     */
    Unknown("Unknown", 65535),
    /**
     * No Error [RFC1035]
     */
    NoError("No Error", 0),
    /**
     * Format Error [RFC1035]
     */
    FormErr("Format Error", 1),
    /**
     * Server Failure [RFC1035]
     */
    ServFail("Server Failure", 2),
    /**
     * Non-Existent Domain [RFC1035]
     */
    NXDomain("Non-Existent Domain", 3),
    /**
     * Not Implemented [RFC1035]
     */
    NotImp("Not Implemented", 4),
    /**
     * Query Refused [RFC1035]
     */
    Refused("Query Refused", 5),
    /**
     * Name Exists when it should not [RFC2136]
     */
    YXDomain("Name Exists when it should not", 6),
    /**
     * RR Set Exists when it should not [RFC2136]
     */
    YXRRSet("RR Set Exists when it should not", 7),
    /**
     * RR Set that should exist does not [RFC2136]
     */
    NXRRSet("RR Set that should exist does not", 8),
    /**
     * Server Not Authoritative for zone [RFC2136]]
     */
    NotAuth("Server Not Authoritative for zone", 9),
    /**
     * Name not contained in zone [RFC2136]
     */
    NotZone("NotZone Name not contained in zone", 10),

    ;

    // 0 NoError No Error [RFC1035]
    // 1 FormErr Format Error [RFC1035]
    // 2 ServFail Server Failure [RFC1035]
    // 3 NXDomain Non-Existent Domain [RFC1035]
    // 4 NotImp Not Implemented [RFC1035]
    // 5 Refused Query Refused [RFC1035]
    // 6 YXDomain Name Exists when it should not [RFC2136]
    // 7 YXRRSet RR Set Exists when it should not [RFC2136]
    // 8 NXRRSet RR Set that should exist does not [RFC2136]
    // 9 NotAuth Server Not Authoritative for zone [RFC2136]
    // 10 NotZone Name not contained in zone [RFC2136]
    // 11-15 Unassigned
    // 16 BADVERS Bad OPT Version [RFC2671]
    // 16 BADSIG TSIG Signature Failure [RFC2845]
    // 17 BADKEY Key not recognized [RFC2845]
    // 18 BADTIME Signature out of time window [RFC2845]
    // 19 BADMODE Bad TKEY Mode [RFC2930]
    // 20 BADNAME Duplicate key name [RFC2930]
    // 21 BADALG Algorithm not supported [RFC2930]
    // 22 BADTRUNC Bad Truncation [RFC4635]
    // 23-3840 Unassigned
    // 3841-4095 Reserved for Private Use [RFC5395]
    // 4096-65534 Unassigned
    // 65535 Reserved, can be allocated by Standards Action [RFC5395]

    /**
     * DNS Result Code types are encoded on the last 4 bits
     */
    final static int     RCode_MASK         = 0x0F;
    /**
     * DNS Extended Result Code types are encoded on the first 8 bits
     */
    final static int     ExtendedRCode_MASK = 0xFF;

    private final String _externalName;

    private final int    _index;

    DNSResultCode(String name, int index) {
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
    public static DNSResultCode resultCodeForFlags(int flags) {
        int maskedIndex = flags & RCode_MASK;
        for (DNSResultCode aCode : DNSResultCode.values()) {
            if (aCode._index == maskedIndex) return aCode;
        }
        return Unknown;
    }

    public static DNSResultCode resultCodeForFlags(int flags, int extendedRCode) {
        int maskedIndex = ((extendedRCode >> 28) & ExtendedRCode_MASK) | (flags & RCode_MASK);
        for (DNSResultCode aCode : DNSResultCode.values()) {
            if (aCode._index == maskedIndex) return aCode;
        }
        return Unknown;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
