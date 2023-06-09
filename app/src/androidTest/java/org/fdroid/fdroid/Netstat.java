package org.fdroid.fdroid;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replacer for the netstat utility, by reading the /proc filesystem it can find out the
 * open connections of the system
 * From http://www.ussg.iu.edu/hypermail/linux/kernel/0409.1/2166.html :
 * It will first list all listening TCP sockets, and next list all established
 * TCP connections. A typical entry of /proc/net/tcp would look like this (split
 * up into 3 parts because of the length of the line):
 * <p>
 * 46: 010310AC:9C4C 030310AC:1770 01
 * | | | | | |--> connection state
 * | | | | |------> remote TCP port number
 * | | | |-------------> remote IPv4 address
 * | | |--------------------> local TCP port number
 * | |---------------------------> local IPv4 address
 * |----------------------------------> number of entry
 * <p>
 * 00000150:00000000 01:00000019 00000000
 * | | | | |--> number of unrecovered RTO timeouts
 * | | | |----------> number of jiffies until timer expires
 * | | |----------------> timer_active (see below)
 * | |----------------------> receive-queue
 * |-------------------------------> transmit-queue
 * <p>
 * 1000 0 54165785 4 cd1e6040 25 4 27 3 -1
 * | | | | | | | | | |--> slow start size threshold,
 * | | | | | | | | | or -1 if the threshold
 * | | | | | | | | | is >= 0xFFFF
 * | | | | | | | | |----> sending congestion window
 * | | | | | | | |-------> (ack.quick<<1)|ack.pingpong
 * | | | | | | |---------> Predicted tick of soft clock
 * | | | | | | (delayed ACK control data)
 * | | | | | |------------> retransmit timeout
 * | | | | |------------------> location of socket in memory
 * | | | |-----------------------> socket reference count
 * | | |-----------------------------> inode
 * | |----------------------------------> unanswered 0-window probes
 * |---------------------------------------------> uid
 *
 * @author Ciprian Dobre
 */
public class Netstat {

    /**
     * Possible values for states in /proc/net/tcp
     */
    private static final String[] STATES = {
            "ESTBLSH", "SYNSENT", "SYNRECV", "FWAIT1", "FWAIT2", "TMEWAIT",
            "CLOSED", "CLSWAIT", "LASTACK", "LISTEN", "CLOSING", "UNKNOWN",
    };
    /**
     * Pattern used when parsing through /proc/net/tcp
     */
    private static final Pattern NET_PATTERN = Pattern.compile(
            "\\d+:\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+)\\s+" +
                    "[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+\\s+([\\d]+)\\s+[\\d]+\\s+([\\d]+)");

    /**
     * Utility method that converts an address from a hex representation as founded in /proc to String representation
     */
    private static String getAddress(final String hexa) {
        try {
            // first let's convert the address to Integer
            final long v = Long.parseLong(hexa, 16);
            // in /proc the order is little endian and java uses big endian order we also need to invert the order
            final long adr = (v >>> 24) | (v << 24) |
                    ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
            // and now it's time to output the result
            return ((adr >> 24) & 0xff) + "." + ((adr >> 16) & 0xff) + "." + ((adr >> 8) & 0xff) + "." + (adr & 0xff);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "0.0.0.0";  // NOPMD
        }
    }

    private static int getInt16(final String hexa) {
        try {
            return Integer.parseInt(hexa, 16);
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    /*
    private static String getPName(final int pid) {
        final Pattern pattern = Pattern.compile("Name:\\s*(\\S+)");
        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/" + pid + "/status"));
            String line;
            while ((line = in.readLine()) != null) {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            in.close();
        } catch (Throwable t) {
            // ignored
        }
        return "UNKNOWN";
    }
    */

    /**
     * Method used to question for the connections currently opened
     *
     * @return The list of connections (as Connection objects)
     */
    public static List<Connection> getConnections() {

        final ArrayList<Connection> net = new ArrayList<>();

        // read from /proc/net/tcp the list of currently opened socket connections
        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/net/tcp"));
            String line;
            while ((line = in.readLine()) != null) { // NOPMD
                Matcher matcher = NET_PATTERN.matcher(line);
                if (matcher.find()) {
                    final Connection c = new Connection();
                    c.setProtocol(Connection.TCP_CONNECTION);
                    net.add(c);
                    final String localPortHexa = matcher.group(2);
                    final String remoteAddressHexa = matcher.group(3);
                    final String remotePortHexa = matcher.group(4);
                    final String statusHexa = matcher.group(5);
                    //final String uid = matcher.group(6);
                    //final String inode = matcher.group(7);
                    c.setLocalPort(getInt16(localPortHexa));
                    c.setRemoteAddress(getAddress(remoteAddressHexa));
                    c.setRemotePort(getInt16(remotePortHexa));
                    try {
                        c.setStatus(STATES[Integer.parseInt(statusHexa, 16) - 1]);
                    } catch (Exception ex) {
                        c.setStatus(STATES[11]); // unknown
                    }
                    c.setPID(-1); // unknown
                    c.setPName("UNKNOWN");
                }
            }
            in.close();
        } catch (Throwable t) { // NOPMD
            // ignored
        }

        // read from /proc/net/udp the list of currently opened socket connections
        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/net/udp"));
            String line;
            while ((line = in.readLine()) != null) { // NOPMD
                Matcher matcher = NET_PATTERN.matcher(line);
                if (matcher.find()) {
                    final Connection c = new Connection();
                    c.setProtocol(Connection.UDP_CONNECTION);
                    net.add(c);
                    final String localPortHexa = matcher.group(2);
                    final String remoteAddressHexa = matcher.group(3);
                    final String remotePortHexa = matcher.group(4);
                    final String statusHexa = matcher.group(5);
                    //final String uid = matcher.group(6);
                    //final String inode = matcher.group(7);
                    c.setLocalPort(getInt16(localPortHexa));
                    c.setRemoteAddress(getAddress(remoteAddressHexa));
                    c.setRemotePort(getInt16(remotePortHexa));
                    try {
                        c.setStatus(STATES[Integer.parseInt(statusHexa, 16) - 1]);
                    } catch (Exception ex) {
                        c.setStatus(STATES[11]); // unknown
                    }
                    c.setPID(-1); // unknown
                    c.setPName("UNKNOWN");
                }
            }
            in.close();
        } catch (Throwable t) { // NOPMD
            // ignored
        }

        // read from /proc/net/raw the list of currently opened socket connections
        try {
            BufferedReader in = new BufferedReader(new FileReader("/proc/net/raw"));
            String line;
            while ((line = in.readLine()) != null) { // NOPMD
                Matcher matcher = NET_PATTERN.matcher(line);
                if (matcher.find()) {
                    final Connection c = new Connection();
                    c.setProtocol(Connection.RAW_CONNECTION);
                    net.add(c);
                    //final String localAddressHexa = matcher.group(1);
                    final String localPortHexa = matcher.group(2);
                    final String remoteAddressHexa = matcher.group(3);
                    final String remotePortHexa = matcher.group(4);
                    final String statusHexa = matcher.group(5);
                    //final String uid = matcher.group(6);
                    //final String inode = matcher.group(7);
                    c.setLocalPort(getInt16(localPortHexa));
                    c.setRemoteAddress(getAddress(remoteAddressHexa));
                    c.setRemotePort(getInt16(remotePortHexa));
                    try {
                        c.setStatus(STATES[Integer.parseInt(statusHexa, 16) - 1]);
                    } catch (Exception ex) {
                        c.setStatus(STATES[11]); // unknown
                    }
                    c.setPID(-1); // unknown
                    c.setPName("UNKNOWN");
                }
            }
            in.close();
        } catch (Throwable t) { // NOPMD
            // ignored
        }
        return net;
    }

    /**
     * Information about a given connection
     *
     * @author Ciprian Dobre
     */
    public static class Connection {

        /**
         * Types of connection protocol
         ***/
        static final byte TCP_CONNECTION = 0;
        static final byte UDP_CONNECTION = 1;
        static final byte RAW_CONNECTION = 2;
        /**
         * <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 1988671591829311032L;
        /**
         * The protocol of the connection (can be tcp, udp or raw)
         */
        protected byte protocol;

        /**
         * The owner of the connection (username)
         */
        protected String powner;

        /**
         * The pid of the owner process
         */
        protected int pid;

        /**
         * The name of the program owning the connection
         */
        protected String pname;

        /**
         * Local port
         */
        protected int localPort;

        /**
         * Remote address of the connection
         */
        protected String remoteAddress;

        /**
         * Remote port
         */
        protected int remotePort;

        /**
         * Status of the connection
         */
        protected String status;

        public final byte getProtocol() {
            return protocol;
        }

        final void setProtocol(final byte protocol) {
            this.protocol = protocol;
        }

        final String getProtocolAsString() {
            switch (protocol) {
                case TCP_CONNECTION:
                    return "TCP";
                case UDP_CONNECTION:
                    return "UDP";
                case RAW_CONNECTION:
                    return "RAW";
            }
            return "UNKNOWN";
        }

        public final String getPOwner() {
            return powner;
        }

        public final void setPOwner(final String owner) {
            this.powner = owner;
        }

        public final int getPID() {
            return pid;
        }

        final void setPID(final int pid) {
            this.pid = pid;
        }

        public final String getPName() {
            return pname;
        }

        final void setPName(final String pname) {
            this.pname = pname;
        }

        public final int getLocalPort() {
            return localPort;
        }

        final void setLocalPort(final int localPort) {
            this.localPort = localPort;
        }

        public final String getRemoteAddress() {
            return remoteAddress;
        }

        final void setRemoteAddress(final String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public final int getRemotePort() {
            return remotePort;
        }

        final void setRemotePort(final int remotePort) {
            this.remotePort = remotePort;
        }

        public final String getStatus() {
            return status;
        }

        final void setStatus(final String status) {
            this.status = status;
        }

        @NonNull
        public String toString() {
            return "[Prot=" + getProtocolAsString() +
                    ",POwner=" + powner +
                    ",PID=" + pid +
                    ",PName=" + pname +
                    ",LPort=" + localPort +
                    ",RAddress=" + remoteAddress +
                    ",RPort=" + remotePort +
                    ",Status=" + status +
                    "]";
        }

    }
}