/**
 *
 */
package javax.jmdns.impl.constants;

import java.util.logging.Logger;

/**
 * DNS Record Type
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSRecordType {
    /**
     * Address
     */
    TYPE_IGNORE("ignore", 0),
    /**
     * Address
     */
    TYPE_A("a", 1),
    /**
     * Name Server
     */
    TYPE_NS("ns", 2),
    /**
     * Mail Destination
     */
    TYPE_MD("md", 3),
    /**
     * Mail Forwarder
     */
    TYPE_MF("mf", 4),
    /**
     * Canonical Name
     */
    TYPE_CNAME("cname", 5),
    /**
     * Start of Authority
     */
    TYPE_SOA("soa", 6),
    /**
     * Mailbox
     */
    TYPE_MB("mb", 7),
    /**
     * Mail Group
     */
    TYPE_MG("mg", 8),
    /**
     * Mail Rename
     */
    TYPE_MR("mr", 9),
    /**
     * NULL RR
     */
    TYPE_NULL("null", 10),
    /**
     * Well-known-service
     */
    TYPE_WKS("wks", 11),
    /**
     * Domain Name pointer
     */
    TYPE_PTR("ptr", 12),
    /**
     * Host information
     */
    TYPE_HINFO("hinfo", 13),
    /**
     * Mailbox information
     */
    TYPE_MINFO("minfo", 14),
    /**
     * Mail exchanger
     */
    TYPE_MX("mx", 15),
    /**
     * Arbitrary text string
     */
    TYPE_TXT("txt", 16),
    /**
     * for Responsible Person [RFC1183]
     */
    TYPE_RP("rp", 17),
    /**
     * for AFS Data Base location [RFC1183]
     */
    TYPE_AFSDB("afsdb", 18),
    /**
     * for X.25 PSDN address [RFC1183]
     */
    TYPE_X25("x25", 19),
    /**
     * for ISDN address [RFC1183]
     */
    TYPE_ISDN("isdn", 20),
    /**
     * for Route Through [RFC1183]
     */
    TYPE_RT("rt", 21),
    /**
     * for NSAP address, NSAP style A record [RFC1706]
     */
    TYPE_NSAP("nsap", 22),
    /**
     *
     */
    TYPE_NSAP_PTR("nsap-otr", 23),
    /**
     * for security signature [RFC2931]
     */
    TYPE_SIG("sig", 24),
    /**
     * for security key [RFC2535]
     */
    TYPE_KEY("key", 25),
    /**
     * X.400 mail mapping information [RFC2163]
     */
    TYPE_PX("px", 26),
    /**
     * Geographical Position [RFC1712]
     */
    TYPE_GPOS("gpos", 27),
    /**
     * IP6 Address [Thomson]
     */
    TYPE_AAAA("aaaa", 28),
    /**
     * Location Information [Vixie]
     */
    TYPE_LOC("loc", 29),
    /**
     * Next Domain - OBSOLETE [RFC2535, RFC3755]
     */
    TYPE_NXT("nxt", 30),
    /**
     * Endpoint Identifier [Patton]
     */
    TYPE_EID("eid", 31),
    /**
     * Nimrod Locator [Patton]
     */
    TYPE_NIMLOC("nimloc", 32),
    /**
     * Server Selection [RFC2782]
     */
    TYPE_SRV("srv", 33),
    /**
     * ATM Address [Dobrowski]
     */
    TYPE_ATMA("atma", 34),
    /**
     * Naming Authority Pointer [RFC2168, RFC2915]
     */
    TYPE_NAPTR("naptr", 35),
    /**
     * Key Exchanger [RFC2230]
     */
    TYPE_KX("kx", 36),
    /**
     * CERT [RFC2538]
     */
    TYPE_CERT("cert", 37),
    /**
     * A6 [RFC2874]
     */
    TYPE_A6("a6", 38),
    /**
     * DNAME [RFC2672]
     */
    TYPE_DNAME("dname", 39),
    /**
     * SINK [Eastlake]
     */
    TYPE_SINK("sink", 40),
    /**
     * OPT [RFC2671]
     */
    TYPE_OPT("opt", 41),
    /**
     * APL [RFC3123]
     */
    TYPE_APL("apl", 42),
    /**
     * Delegation Signer [RFC3658]
     */
    TYPE_DS("ds", 43),
    /**
     * SSH Key Fingerprint [RFC-ietf-secsh-dns-05.txt]
     */
    TYPE_SSHFP("sshfp", 44),
    /**
     * RRSIG [RFC3755]
     */
    TYPE_RRSIG("rrsig", 46),
    /**
     * NSEC [RFC3755]
     */
    TYPE_NSEC("nsec", 47),
    /**
     * DNSKEY [RFC3755]
     */
    TYPE_DNSKEY("dnskey", 48),
    /**
     * [IANA-Reserved]
     */
    TYPE_UINFO("uinfo", 100),
    /**
     * [IANA-Reserved]
     */
    TYPE_UID("uid", 101),
    /**
     * [IANA-Reserved]
     */
    TYPE_GID("gid", 102),
    /**
     * [IANA-Reserved]
     */
    TYPE_UNSPEC("unspec", 103),
    /**
     * Transaction Key [RFC2930]
     */
    TYPE_TKEY("tkey", 249),
    /**
     * Transaction Signature [RFC2845]
     */
    TYPE_TSIG("tsig", 250),
    /**
     * Incremental transfer [RFC1995]
     */
    TYPE_IXFR("ixfr", 251),
    /**
     * Transfer of an entire zone [RFC1035]
     */
    TYPE_AXFR("axfr", 252),
    /**
     * Mailbox-related records (MB, MG or MR) [RFC1035]
     */
    TYPE_MAILA("mails", 253),
    /**
     * Mail agent RRs (Obsolete - see MX) [RFC1035]
     */
    TYPE_MAILB("mailb", 254),
    /**
     * Request for all records [RFC1035]
     */
    TYPE_ANY("any", 255);

    private static Logger logger = Logger.getLogger(DNSRecordType.class.getName());

    private final String  _externalName;

    private final int     _index;

    DNSRecordType(String name, int index) {
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
     * @param name
     * @return type for name
     */
    public static DNSRecordType typeForName(String name) {
        if (name != null) {
            String aName = name.toLowerCase();
            for (DNSRecordType aType : DNSRecordType.values()) {
                if (aType._externalName.equals(aName)) return aType;
            }
        }
        logger.severe("Could not find record type for name: " + name);
        return TYPE_IGNORE;
    }

    /**
     * @param index
     * @return type for name
     */
    public static DNSRecordType typeForIndex(int index) {
        for (DNSRecordType aType : DNSRecordType.values()) {
            if (aType._index == index) return aType;
        }
        logger.severe("Could not find record type for index: " + index);
        return TYPE_IGNORE;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
