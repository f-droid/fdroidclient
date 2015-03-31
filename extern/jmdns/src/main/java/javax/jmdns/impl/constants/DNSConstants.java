// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.constants;

/**
 * DNS constants.
 *
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public final class DNSConstants {
    // http://www.iana.org/assignments/dns-parameters

    // changed to final class - jeffs
    public static final String MDNS_GROUP                     = "224.0.0.251";
    public static final String MDNS_GROUP_IPV6                = "FF02::FB";
    public static final int    MDNS_PORT                      = Integer.parseInt(System.getProperty("net.mdns.port", "5353"));
    public static final int    DNS_PORT                       = 53;
    public static final int    DNS_TTL                        = 60 * 60;                                                      // default one hour TTL
    // public static final int DNS_TTL = 120 * 60; // two hour TTL (draft-cheshire-dnsext-multicastdns.txt ch 13)

    public static final int    MAX_MSG_TYPICAL                = 1460;
    public static final int    MAX_MSG_ABSOLUTE               = 8972;

    public static final int    FLAGS_QR_MASK                  = 0x8000;                                                       // Query response mask
    public static final int    FLAGS_QR_QUERY                 = 0x0000;                                                       // Query
    public static final int    FLAGS_QR_RESPONSE              = 0x8000;                                                       // Response

    public static final int    FLAGS_OPCODE                   = 0x7800;                                                       // Operation code
    public static final int    FLAGS_AA                       = 0x0400;                                                       // Authorative answer
    public static final int    FLAGS_TC                       = 0x0200;                                                       // Truncated
    public static final int    FLAGS_RD                       = 0x0100;                                                       // Recursion desired
    public static final int    FLAGS_RA                       = 0x8000;                                                       // Recursion available

    public static final int    FLAGS_Z                        = 0x0040;                                                       // Zero
    public static final int    FLAGS_AD                       = 0x0020;                                                       // Authentic data
    public static final int    FLAGS_CD                       = 0x0010;                                                       // Checking disabled
    public static final int    FLAGS_RCODE                    = 0x000F;                                                       // Response code

    // Time Intervals for various functions

    public static final int    SHARED_QUERY_TIME              = 20;                                                           // milliseconds before send shared query
    public static final int    QUERY_WAIT_INTERVAL            = 225;                                                          // milliseconds between query loops.
    public static final int    PROBE_WAIT_INTERVAL            = 250;                                                          // milliseconds between probe loops.
    public static final int    RESPONSE_MIN_WAIT_INTERVAL     = 20;                                                           // minimal wait interval for response.
    public static final int    RESPONSE_MAX_WAIT_INTERVAL     = 115;                                                          // maximal wait interval for response
    public static final int    PROBE_CONFLICT_INTERVAL        = 1000;                                                         // milliseconds to wait after conflict.
    public static final int    PROBE_THROTTLE_COUNT           = 10;                                                           // After x tries go 1 time a sec. on probes.
    public static final int    PROBE_THROTTLE_COUNT_INTERVAL  = 5000;                                                         // We only increment the throttle count, if the previous increment is inside this interval.
    public static final int    ANNOUNCE_WAIT_INTERVAL         = 1000;                                                         // milliseconds between Announce loops.
    public static final int    RECORD_REAPER_INTERVAL         = 10000;                                                        // milliseconds between cache cleanups.
    public static final int    RECORD_EXPIRY_DELAY            = 1;                                                            // This is 1s delay used in ttl and therefore in seconds
    public static final int    KNOWN_ANSWER_TTL               = 120;
    public static final int    ANNOUNCED_RENEWAL_TTL_INTERVAL = DNS_TTL * 500;                                                // 50% of the TTL in milliseconds

    public static final long   CLOSE_TIMEOUT                  = ANNOUNCE_WAIT_INTERVAL * 5L;
    public static final long   SERVICE_INFO_TIMEOUT           = ANNOUNCE_WAIT_INTERVAL * 6L;

    public static final int    NETWORK_CHECK_INTERVAL         = 10 * 1000;                                                    // 10 secondes

}
