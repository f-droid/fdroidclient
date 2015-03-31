// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;

/**
 * An outgoing DNS message.
 *
 * @author Arthur van Hoff, Rick Blair, Werner Randelshofer
 */
public final class DNSOutgoing extends DNSMessage {

    public static class MessageOutputStream extends ByteArrayOutputStream {
        private final DNSOutgoing _out;

        private final int         _offset;

        /**
         * Creates a new message stream, with a buffer capacity of the specified size, in bytes.
         *
         * @param size
         *            the initial size.
         * @exception IllegalArgumentException
         *                if size is negative.
         */
        MessageOutputStream(int size, DNSOutgoing out) {
            this(size, out, 0);
        }

        MessageOutputStream(int size, DNSOutgoing out, int offset) {
            super(size);
            _out = out;
            _offset = offset;
        }

        void writeByte(int value) {
            this.write(value & 0xFF);
        }

        void writeBytes(String str, int off, int len) {
            for (int i = 0; i < len; i++) {
                writeByte(str.charAt(off + i));
            }
        }

        void writeBytes(byte data[]) {
            if (data != null) {
                writeBytes(data, 0, data.length);
            }
        }

        void writeBytes(byte data[], int off, int len) {
            for (int i = 0; i < len; i++) {
                writeByte(data[off + i]);
            }
        }

        void writeShort(int value) {
            writeByte(value >> 8);
            writeByte(value);
        }

        void writeInt(int value) {
            writeShort(value >> 16);
            writeShort(value);
        }

        void writeUTF(String str, int off, int len) {
            // compute utf length
            int utflen = 0;
            for (int i = 0; i < len; i++) {
                int ch = str.charAt(off + i);
                if ((ch >= 0x0001) && (ch <= 0x007F)) {
                    utflen += 1;
                } else {
                    if (ch > 0x07FF) {
                        utflen += 3;
                    } else {
                        utflen += 2;
                    }
                }
            }
            // write utf length
            writeByte(utflen);
            // write utf data
            for (int i = 0; i < len; i++) {
                int ch = str.charAt(off + i);
                if ((ch >= 0x0001) && (ch <= 0x007F)) {
                    writeByte(ch);
                } else {
                    if (ch > 0x07FF) {
                        writeByte(0xE0 | ((ch >> 12) & 0x0F));
                        writeByte(0x80 | ((ch >> 6) & 0x3F));
                        writeByte(0x80 | ((ch >> 0) & 0x3F));
                    } else {
                        writeByte(0xC0 | ((ch >> 6) & 0x1F));
                        writeByte(0x80 | ((ch >> 0) & 0x3F));
                    }
                }
            }
        }

        void writeName(String name) {
            writeName(name, true);
        }

        void writeName(String name, boolean useCompression) {
            String aName = name;
            while (true) {
                int n = aName.indexOf('.');
                if (n < 0) {
                    n = aName.length();
                }
                if (n <= 0) {
                    writeByte(0);
                    return;
                }
                String label = aName.substring(0, n);
                if (useCompression && USE_DOMAIN_NAME_COMPRESSION) {
                    Integer offset = _out._names.get(aName);
                    if (offset != null) {
                        int val = offset.intValue();
                        writeByte((val >> 8) | 0xC0);
                        writeByte(val & 0xFF);
                        return;
                    }
                    _out._names.put(aName, Integer.valueOf(this.size() + _offset));
                    writeUTF(label, 0, label.length());
                } else {
                    writeUTF(label, 0, label.length());
                }
                aName = aName.substring(n);
                if (aName.startsWith(".")) {
                    aName = aName.substring(1);
                }
            }
        }

        void writeQuestion(DNSQuestion question) {
            writeName(question.getName());
            writeShort(question.getRecordType().indexValue());
            writeShort(question.getRecordClass().indexValue());
        }

        void writeRecord(DNSRecord rec, long now) {
            writeName(rec.getName());
            writeShort(rec.getRecordType().indexValue());
            writeShort(rec.getRecordClass().indexValue() | ((rec.isUnique() && _out.isMulticast()) ? DNSRecordClass.CLASS_UNIQUE : 0));
            writeInt((now == 0) ? rec.getTTL() : rec.getRemainingTTL(now));

            // We need to take into account the 2 size bytes
            MessageOutputStream record = new MessageOutputStream(512, _out, _offset + this.size() + 2);
            rec.write(record);
            byte[] byteArray = record.toByteArray();

            writeShort(byteArray.length);
            write(byteArray, 0, byteArray.length);
        }

    }

    /**
     * This can be used to turn off domain name compression. This was helpful for tracking problems interacting with other mdns implementations.
     */
    public static boolean             USE_DOMAIN_NAME_COMPRESSION = true;

    Map<String, Integer>              _names;

    private int                       _maxUDPPayload;

    private final MessageOutputStream _questionsBytes;

    private final MessageOutputStream _answersBytes;

    private final MessageOutputStream _authoritativeAnswersBytes;

    private final MessageOutputStream _additionalsAnswersBytes;

    private final static int          HEADER_SIZE                 = 12;

    /**
     * Create an outgoing multicast query or response.
     *
     * @param flags
     */
    public DNSOutgoing(int flags) {
        this(flags, true, DNSConstants.MAX_MSG_TYPICAL);
    }

    /**
     * Create an outgoing query or response.
     *
     * @param flags
     * @param multicast
     */
    public DNSOutgoing(int flags, boolean multicast) {
        this(flags, multicast, DNSConstants.MAX_MSG_TYPICAL);
    }

    /**
     * Create an outgoing query or response.
     *
     * @param flags
     * @param multicast
     * @param senderUDPPayload
     *            The sender's UDP payload size is the number of bytes of the largest UDP payload that can be reassembled and delivered in the sender's network stack.
     */
    public DNSOutgoing(int flags, boolean multicast, int senderUDPPayload) {
        super(flags, 0, multicast);
        _names = new HashMap<String, Integer>();
        _maxUDPPayload = (senderUDPPayload > 0 ? senderUDPPayload : DNSConstants.MAX_MSG_TYPICAL);
        _questionsBytes = new MessageOutputStream(senderUDPPayload, this);
        _answersBytes = new MessageOutputStream(senderUDPPayload, this);
        _authoritativeAnswersBytes = new MessageOutputStream(senderUDPPayload, this);
        _additionalsAnswersBytes = new MessageOutputStream(senderUDPPayload, this);
    }

    /**
     * Return the number of byte available in the message.
     *
     * @return available space
     */
    public int availableSpace() {
        return _maxUDPPayload - HEADER_SIZE - _questionsBytes.size() - _answersBytes.size() - _authoritativeAnswersBytes.size() - _additionalsAnswersBytes.size();
    }

    /**
     * Add a question to the message.
     *
     * @param rec
     * @exception IOException
     */
    public void addQuestion(DNSQuestion rec) throws IOException {
        MessageOutputStream record = new MessageOutputStream(512, this);
        record.writeQuestion(rec);
        byte[] byteArray = record.toByteArray();
        if (byteArray.length < this.availableSpace()) {
            _questions.add(rec);
            _questionsBytes.write(byteArray, 0, byteArray.length);
        } else {
            throw new IOException("message full");
        }
    }

    /**
     * Add an answer if it is not suppressed.
     *
     * @param in
     * @param rec
     * @exception IOException
     */
    public void addAnswer(DNSIncoming in, DNSRecord rec) throws IOException {
        if ((in == null) || !rec.suppressedBy(in)) {
            this.addAnswer(rec, 0);
        }
    }

    /**
     * Add an answer to the message.
     *
     * @param rec
     * @param now
     * @exception IOException
     */
    public void addAnswer(DNSRecord rec, long now) throws IOException {
        if (rec != null) {
            if ((now == 0) || !rec.isExpired(now)) {
                MessageOutputStream record = new MessageOutputStream(512, this);
                record.writeRecord(rec, now);
                byte[] byteArray = record.toByteArray();
                if (byteArray.length < this.availableSpace()) {
                    _answers.add(rec);
                    _answersBytes.write(byteArray, 0, byteArray.length);
                } else {
                    throw new IOException("message full");
                }
            }
        }
    }

    /**
     * Add an authoritative answer to the message.
     *
     * @param rec
     * @exception IOException
     */
    public void addAuthorativeAnswer(DNSRecord rec) throws IOException {
        MessageOutputStream record = new MessageOutputStream(512, this);
        record.writeRecord(rec, 0);
        byte[] byteArray = record.toByteArray();
        if (byteArray.length < this.availableSpace()) {
            _authoritativeAnswers.add(rec);
            _authoritativeAnswersBytes.write(byteArray, 0, byteArray.length);
        } else {
            throw new IOException("message full");
        }
    }

    /**
     * Add an additional answer to the record. Omit if there is no room.
     *
     * @param in
     * @param rec
     * @exception IOException
     */
    public void addAdditionalAnswer(DNSIncoming in, DNSRecord rec) throws IOException {
        MessageOutputStream record = new MessageOutputStream(512, this);
        record.writeRecord(rec, 0);
        byte[] byteArray = record.toByteArray();
        if (byteArray.length < this.availableSpace()) {
            _additionals.add(rec);
            _additionalsAnswersBytes.write(byteArray, 0, byteArray.length);
        } else {
            throw new IOException("message full");
        }
    }

    /**
     * Builds the final message buffer to be send and returns it.
     *
     * @return bytes to send.
     */
    public byte[] data() {
        long now = System.currentTimeMillis(); // System.currentTimeMillis()
        _names.clear();

        MessageOutputStream message = new MessageOutputStream(_maxUDPPayload, this);
        message.writeShort(_multicast ? 0 : this.getId());
        message.writeShort(this.getFlags());
        message.writeShort(this.getNumberOfQuestions());
        message.writeShort(this.getNumberOfAnswers());
        message.writeShort(this.getNumberOfAuthorities());
        message.writeShort(this.getNumberOfAdditionals());
        for (DNSQuestion question : _questions) {
            message.writeQuestion(question);
        }
        for (DNSRecord record : _answers) {
            message.writeRecord(record, now);
        }
        for (DNSRecord record : _authoritativeAnswers) {
            message.writeRecord(record, now);
        }
        for (DNSRecord record : _additionals) {
            message.writeRecord(record, now);
        }
        return message.toByteArray();
    }

    /**
     * Debugging.
     */
    String print(boolean dump) {
        StringBuilder buf = new StringBuilder();
        buf.append(this.print());
        if (dump) {
            buf.append(this.print(this.data()));
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(isQuery() ? "dns[query:" : "dns[response:");
        buf.append(" id=0x");
        buf.append(Integer.toHexString(this.getId()));
        if (this.getFlags() != 0) {
            buf.append(", flags=0x");
            buf.append(Integer.toHexString(this.getFlags()));
            if (this.isResponse()) {
                buf.append(":r");
            }
            if (this.isAuthoritativeAnswer()) {
                buf.append(":aa");
            }
            if (this.isTruncated()) {
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
        buf.append("\nnames=");
        buf.append(_names);
        buf.append("]");
        return buf.toString();
    }

    /**
     * @return the maxUDPPayload
     */
    public int getMaxUDPPayload() {
        return this._maxUDPPayload;
    }

}
