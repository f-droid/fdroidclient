// /Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSLabel;
import javax.jmdns.impl.constants.DNSOptionCode;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSResultCode;

/**
 * Parse an incoming DNS message into its components.
 *
 * @author Arthur van Hoff, Werner Randelshofer, Pierre Frisch, Daniel Bobbert
 */
public final class DNSIncoming extends DNSMessage {
    private static Logger logger                                = Logger.getLogger(DNSIncoming.class.getName());

    // This is a hack to handle a bug in the BonjourConformanceTest
    // It is sending out target strings that don't follow the "domain name" format.
    public static boolean USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET = true;

    public static class MessageInputStream extends ByteArrayInputStream {
        private static Logger      logger1 = Logger.getLogger(MessageInputStream.class.getName());

        final Map<Integer, String> _names;

        public MessageInputStream(byte[] buffer, int length) {
            this(buffer, 0, length);
        }

        /**
         * @param buffer
         * @param offset
         * @param length
         */
        public MessageInputStream(byte[] buffer, int offset, int length) {
            super(buffer, offset, length);
            _names = new HashMap<Integer, String>();
        }

        public int readByte() {
            return this.read();
        }

        public int readUnsignedByte() {
            return (this.read() & 0xFF);
        }

        public int readUnsignedShort() {
            return (this.readUnsignedByte() << 8) | this.readUnsignedByte();
        }

        public int readInt() {
            return (this.readUnsignedShort() << 16) | this.readUnsignedShort();
        }

        public byte[] readBytes(int len) {
            byte bytes[] = new byte[len];
            this.read(bytes, 0, len);
            return bytes;
        }

        public String readUTF(int len) {
            StringBuilder buffer = new StringBuilder(len);
            for (int index = 0; index < len; index++) {
                int ch = this.readUnsignedByte();
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
                        // 110x xxxx 10xx xxxx
                        ch = ((ch & 0x1F) << 6) | (this.readUnsignedByte() & 0x3F);
                        index++;
                        break;
                    case 14:
                        // 1110 xxxx 10xx xxxx 10xx xxxx
                        ch = ((ch & 0x0f) << 12) | ((this.readUnsignedByte() & 0x3F) << 6) | (this.readUnsignedByte() & 0x3F);
                        index++;
                        index++;
                        break;
                    default:
                        // 10xx xxxx, 1111 xxxx
                        ch = ((ch & 0x3F) << 4) | (this.readUnsignedByte() & 0x0f);
                        index++;
                        break;
                }
                buffer.append((char) ch);
            }
            return buffer.toString();
        }

        protected synchronized int peek() {
            return (pos < count) ? (buf[pos] & 0xff) : -1;
        }

        public String readName() {
            Map<Integer, StringBuilder> names = new HashMap<Integer, StringBuilder>();
            StringBuilder buffer = new StringBuilder();
            boolean finished = false;
            while (!finished) {
                int len = this.readUnsignedByte();
                if (len == 0) {
                    finished = true;
                    break;
                }
                switch (DNSLabel.labelForByte(len)) {
                    case Standard:
                        int offset = pos - 1;
                        String label = this.readUTF(len) + ".";
                        buffer.append(label);
                        for (StringBuilder previousLabel : names.values()) {
                            previousLabel.append(label);
                        }
                        names.put(Integer.valueOf(offset), new StringBuilder(label));
                        break;
                    case Compressed:
                        int index = (DNSLabel.labelValue(len) << 8) | this.readUnsignedByte();
                        String compressedLabel = _names.get(Integer.valueOf(index));
                        if (compressedLabel == null) {
                            logger1.severe("bad domain name: possible circular name detected. Bad offset: 0x" + Integer.toHexString(index) + " at 0x" + Integer.toHexString(pos - 2));
                            compressedLabel = "";
                        }
                        buffer.append(compressedLabel);
                        for (StringBuilder previousLabel : names.values()) {
                            previousLabel.append(compressedLabel);
                        }
                        finished = true;
                        break;
                    case Extended:
                        // int extendedLabelClass = DNSLabel.labelValue(len);
                        logger1.severe("Extended label are not currently supported.");
                        break;
                    case Unknown:
                    default:
                        logger1.severe("unsupported dns label type: '" + Integer.toHexString(len & 0xC0) + "'");
                }
            }
            for (Integer index : names.keySet()) {
                _names.put(index, names.get(index).toString());
            }
            return buffer.toString();
        }

        public String readNonNameString() {
            int len = this.readUnsignedByte();
            return this.readUTF(len);
        }

    }

    private final DatagramPacket     _packet;

    private final long               _receivedTime;

    private final MessageInputStream _messageInputStream;

    private int                      _senderUDPPayload;

    /**
     * Parse a message from a datagram packet.
     *
     * @param packet
     * @exception IOException
     */
    public DNSIncoming(DatagramPacket packet) throws IOException {
        super(0, 0, packet.getPort() == DNSConstants.MDNS_PORT);
        this._packet = packet;
        InetAddress source = packet.getAddress();
        this._messageInputStream = new MessageInputStream(packet.getData(), packet.getLength());
        this._receivedTime = System.currentTimeMillis();
        this._senderUDPPayload = DNSConstants.MAX_MSG_TYPICAL;

        try {
            this.setId(_messageInputStream.readUnsignedShort());
            this.setFlags(_messageInputStream.readUnsignedShort());
            if (this.getOperationCode() > 0) {
                throw new IOException("Received a message with a non standard operation code. Currently unsupported in the specification.");
            }
            int numQuestions = _messageInputStream.readUnsignedShort();
            int numAnswers = _messageInputStream.readUnsignedShort();
            int numAuthorities = _messageInputStream.readUnsignedShort();
            int numAdditionals = _messageInputStream.readUnsignedShort();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("DNSIncoming() questions:" + numQuestions + " answers:" + numAnswers + " authorities:" + numAuthorities + " additionals:" + numAdditionals);
            }

            // We need some sanity checks
            // A question is at least 5 bytes and answer 11 so check what we have

            if ((numQuestions * 5 + (numAnswers + numAuthorities + numAdditionals) * 11) > packet.getLength()) {
                throw new IOException("questions:" + numQuestions + " answers:" + numAnswers + " authorities:" + numAuthorities + " additionals:" + numAdditionals);
            }

            // parse questions
            if (numQuestions > 0) {
                for (int i = 0; i < numQuestions; i++) {
                    _questions.add(this.readQuestion());
                }
            }

            // parse answers
            if (numAnswers > 0) {
                for (int i = 0; i < numAnswers; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _answers.add(rec);
                    }
                }
            }

            if (numAuthorities > 0) {
                for (int i = 0; i < numAuthorities; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _authoritativeAnswers.add(rec);
                    }
                }
            }

            if (numAdditionals > 0) {
                for (int i = 0; i < numAdditionals; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _additionals.add(rec);
                    }
                }
            }
            // We should have drained the entire stream by now
            if (_messageInputStream.available() > 0) {
                throw new IOException("Received a message with the wrong length.");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "DNSIncoming() dump " + print(true) + "\n exception ", e);
            // This ugly but some JVM don't implement the cause on IOException
            IOException ioe = new IOException("DNSIncoming corrupted message");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private DNSIncoming(int flags, int id, boolean multicast, DatagramPacket packet, long receivedTime) {
        super(flags, id, multicast);
        this._packet = packet;
        this._messageInputStream = new MessageInputStream(packet.getData(), packet.getLength());
        this._receivedTime = receivedTime;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public DNSIncoming clone() {
        DNSIncoming in = new DNSIncoming(this.getFlags(), this.getId(), this.isMulticast(), this._packet, this._receivedTime);
        in._senderUDPPayload = this._senderUDPPayload;
        in._questions.addAll(this._questions);
        in._answers.addAll(this._answers);
        in._authoritativeAnswers.addAll(this._authoritativeAnswers);
        in._additionals.addAll(this._additionals);
        return in;
    }

    private DNSQuestion readQuestion() {
        String domain = _messageInputStream.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(_messageInputStream.readUnsignedShort());
        if (type == DNSRecordType.TYPE_IGNORE) {
            logger.log(Level.SEVERE, "Could not find record type: " + this.print(true));
        }
        int recordClassIndex = _messageInputStream.readUnsignedShort();
        DNSRecordClass recordClass = DNSRecordClass.classForIndex(recordClassIndex);
        boolean unique = recordClass.isUnique(recordClassIndex);
        return DNSQuestion.newQuestion(domain, type, recordClass, unique);
    }

    private DNSRecord readAnswer(InetAddress source) {
        String domain = _messageInputStream.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(_messageInputStream.readUnsignedShort());
        if (type == DNSRecordType.TYPE_IGNORE) {
            logger.log(Level.SEVERE, "Could not find record type. domain: " + domain + "\n" + this.print(true));
        }
        int recordClassIndex = _messageInputStream.readUnsignedShort();
        DNSRecordClass recordClass = (type == DNSRecordType.TYPE_OPT ? DNSRecordClass.CLASS_UNKNOWN : DNSRecordClass.classForIndex(recordClassIndex));
        if ((recordClass == DNSRecordClass.CLASS_UNKNOWN) && (type != DNSRecordType.TYPE_OPT)) {
            logger.log(Level.SEVERE, "Could not find record class. domain: " + domain + " type: " + type + "\n" + this.print(true));
        }
        boolean unique = recordClass.isUnique(recordClassIndex);
        int ttl = _messageInputStream.readInt();
        int len = _messageInputStream.readUnsignedShort();
        DNSRecord rec = null;

        switch (type) {
            case TYPE_A: // IPv4
                rec = new DNSRecord.IPv4Address(domain, recordClass, unique, ttl, _messageInputStream.readBytes(len));
                break;
            case TYPE_AAAA: // IPv6
                rec = new DNSRecord.IPv6Address(domain, recordClass, unique, ttl, _messageInputStream.readBytes(len));
                break;
            case TYPE_CNAME:
            case TYPE_PTR:
                String service = "";
                service = _messageInputStream.readName();
                if (service.length() > 0) {
                    rec = new DNSRecord.Pointer(domain, recordClass, unique, ttl, service);
                } else {
                    logger.log(Level.WARNING, "PTR record of class: " + recordClass + ", there was a problem reading the service name of the answer for domain:" + domain);
                }
                break;
            case TYPE_TXT:
                rec = new DNSRecord.Text(domain, recordClass, unique, ttl, _messageInputStream.readBytes(len));
                break;
            case TYPE_SRV:
                int priority = _messageInputStream.readUnsignedShort();
                int weight = _messageInputStream.readUnsignedShort();
                int port = _messageInputStream.readUnsignedShort();
                String target = "";
                // This is a hack to handle a bug in the BonjourConformanceTest
                // It is sending out target strings that don't follow the "domain name" format.
                if (USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET) {
                    target = _messageInputStream.readName();
                } else {
                    // [PJYF Nov 13 2010] Do we still need this? This looks really bad. All label are supposed to start by a length.
                    target = _messageInputStream.readNonNameString();
                }
                rec = new DNSRecord.Service(domain, recordClass, unique, ttl, priority, weight, port, target);
                break;
            case TYPE_HINFO:
                StringBuilder buf = new StringBuilder();
                buf.append(_messageInputStream.readUTF(len));
                int index = buf.indexOf(" ");
                String cpu = (index > 0 ? buf.substring(0, index) : buf.toString()).trim();
                String os = (index > 0 ? buf.substring(index + 1) : "").trim();
                rec = new DNSRecord.HostInformation(domain, recordClass, unique, ttl, cpu, os);
                break;
            case TYPE_OPT:
                DNSResultCode extendedResultCode = DNSResultCode.resultCodeForFlags(this.getFlags(), ttl);
                int version = (ttl & 0x00ff0000) >> 16;
                if (version == 0) {
                    _senderUDPPayload = recordClassIndex;
                    while (_messageInputStream.available() > 0) {
                        // Read RDData
                        int optionCodeInt = 0;
                        DNSOptionCode optionCode = null;
                        if (_messageInputStream.available() >= 2) {
                            optionCodeInt = _messageInputStream.readUnsignedShort();
                            optionCode = DNSOptionCode.resultCodeForFlags(optionCodeInt);
                        } else {
                            logger.log(Level.WARNING, "There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        int optionLength = 0;
                        if (_messageInputStream.available() >= 2) {
                            optionLength = _messageInputStream.readUnsignedShort();
                        } else {
                            logger.log(Level.WARNING, "There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        byte[] optiondata = new byte[0];
                        if (_messageInputStream.available() >= optionLength) {
                            optiondata = _messageInputStream.readBytes(optionLength);
                        }
                        //
                        // We should really do something with those options.
                        switch (optionCode) {
                            case Owner:
                                // Valid length values are 8, 14, 18 and 20
                                // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                // |Opt|Len|V|S|Primary MAC|Wakeup MAC | Password |
                                // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                //
                                int ownerVersion = 0;
                                int ownerSequence = 0;
                                byte[] ownerPrimaryMacAddress = null;
                                byte[] ownerWakeupMacAddress = null;
                                byte[] ownerPassword = null;
                                try {
                                    ownerVersion = optiondata[0];
                                    ownerSequence = optiondata[1];
                                    ownerPrimaryMacAddress = new byte[] { optiondata[2], optiondata[3], optiondata[4], optiondata[5], optiondata[6], optiondata[7] };
                                    ownerWakeupMacAddress = ownerPrimaryMacAddress;
                                    if (optiondata.length > 8) {
                                        // We have a wakeupMacAddress.
                                        ownerWakeupMacAddress = new byte[] { optiondata[8], optiondata[9], optiondata[10], optiondata[11], optiondata[12], optiondata[13] };
                                    }
                                    if (optiondata.length == 18) {
                                        // We have a short password.
                                        ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17] };
                                    }
                                    if (optiondata.length == 22) {
                                        // We have a long password.
                                        ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17], optiondata[18], optiondata[19], optiondata[20], optiondata[21] };
                                    }
                                } catch (Exception exception) {
                                    logger.warning("Malformed OPT answer. Option code: Owner data: " + this._hexString(optiondata));
                                }
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.fine("Unhandled Owner OPT version: " + ownerVersion + " sequence: " + ownerSequence + " MAC address: " + this._hexString(ownerPrimaryMacAddress)
                                            + (ownerWakeupMacAddress != ownerPrimaryMacAddress ? " wakeup MAC address: " + this._hexString(ownerWakeupMacAddress) : "") + (ownerPassword != null ? " password: " + this._hexString(ownerPassword) : ""));
                                }
                                break;
                            case LLQ:
                            case NSID:
                            case UL:
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, "There was an OPT answer. Option code: " + optionCode + " data: " + this._hexString(optiondata));
                                }
                                break;
                            case Unknown:
                                logger.log(Level.WARNING, "There was an OPT answer. Not currently handled. Option code: " + optionCodeInt + " data: " + this._hexString(optiondata));
                                break;
                            default:
                                // This is to keep the compiler happy.
                                break;
                        }
                    }
                } else {
                    logger.log(Level.WARNING, "There was an OPT answer. Wrong version number: " + version + " result code: " + extendedResultCode);
                }
                break;
            default:
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("DNSIncoming() unknown type:" + type);
                }
                _messageInputStream.skip(len);
                break;
        }
        if (rec != null) {
            rec.setRecordSource(source);
        }
        return rec;
    }

    /**
     * Debugging.
     */
    String print(boolean dump) {
        StringBuilder buf = new StringBuilder();
        buf.append(this.print());
        if (dump) {
            byte[] data = new byte[_packet.getLength()];
            System.arraycopy(_packet.getData(), 0, data, 0, data.length);
            buf.append(this.print(data));
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(isQuery() ? "dns[query," : "dns[response,");
        if (_packet.getAddress() != null) {
            buf.append(_packet.getAddress().getHostAddress());
        }
        buf.append(':');
        buf.append(_packet.getPort());
        buf.append(", length=");
        buf.append(_packet.getLength());
        buf.append(", id=0x");
        buf.append(Integer.toHexString(this.getId()));
        if (this.getFlags() != 0) {
            buf.append(", flags=0x");
            buf.append(Integer.toHexString(this.getFlags()));
            if ((this.getFlags() & DNSConstants.FLAGS_QR_RESPONSE) != 0) {
                buf.append(":r");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_AA) != 0) {
                buf.append(":aa");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_TC) != 0) {
                buf.append(":tc");
            }
        }
        if (this.getNumberOfQuestions() > 0) {
            buf.append(", questions=");
            buf.append(this.getNumberOfQuestions());
        }
        if (this.getNumberOfAnswers() > 0) {
            buf.append(", answers=");
            buf.append(this.getNumberOfAnswers());
        }
        if (this.getNumberOfAuthorities() > 0) {
            buf.append(", authorities=");
            buf.append(this.getNumberOfAuthorities());
        }
        if (this.getNumberOfAdditionals() > 0) {
            buf.append(", additionals=");
            buf.append(this.getNumberOfAdditionals());
        }
        if (this.getNumberOfQuestions() > 0) {
            buf.append("\nquestions:");
            for (DNSQuestion question : _questions) {
                buf.append("\n\t");
                buf.append(question);
            }
        }
        if (this.getNumberOfAnswers() > 0) {
            buf.append("\nanswers:");
            for (DNSRecord record : _answers) {
                buf.append("\n\t");
                buf.append(record);
            }
        }
        if (this.getNumberOfAuthorities() > 0) {
            buf.append("\nauthorities:");
            for (DNSRecord record : _authoritativeAnswers) {
                buf.append("\n\t");
                buf.append(record);
            }
        }
        if (this.getNumberOfAdditionals() > 0) {
            buf.append("\nadditionals:");
            for (DNSRecord record : _additionals) {
                buf.append("\n\t");
                buf.append(record);
            }
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Appends answers to this Incoming.
     *
     * @exception IllegalArgumentException
     *                If not a query or if Truncated.
     */
    void append(DNSIncoming that) {
        if (this.isQuery() && this.isTruncated() && that.isQuery()) {
            this._questions.addAll(that.getQuestions());
            this._answers.addAll(that.getAnswers());
            this._authoritativeAnswers.addAll(that.getAuthorities());
            this._additionals.addAll(that.getAdditionals());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int elapseSinceArrival() {
        return (int) (System.currentTimeMillis() - _receivedTime);
    }

    /**
     * This will return the default UDP payload except if an OPT record was found with a different size.
     *
     * @return the senderUDPPayload
     */
    public int getSenderUDPPayload() {
        return this._senderUDPPayload;
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Returns a hex-string for printing
     *
     * @param bytes
     * @return Returns a hex-string which can be used within a SQL expression
     */
    private String _hexString(byte[] bytes) {

        StringBuilder result = new StringBuilder(2 * bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }

}
