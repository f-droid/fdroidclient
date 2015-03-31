/**
 *
 */
package javax.jmdns.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jmdns.impl.constants.DNSConstants;

/**
 * DNSMessage define a DNS message either incoming or outgoing.
 *
 * @author Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public abstract class DNSMessage {

    /**
     *
     */
    public static final boolean       MULTICAST = true;

    /**
     *
     */
    public static final boolean       UNICAST   = false;

    // protected DatagramPacket _packet;
    // protected int _off;
    // protected int _len;
    // protected byte[] _data;

    private int                       _id;

    boolean                           _multicast;

    private int                       _flags;

    protected final List<DNSQuestion> _questions;

    protected final List<DNSRecord>   _answers;

    protected final List<DNSRecord>   _authoritativeAnswers;

    protected final List<DNSRecord>   _additionals;

    /**
     * @param flags
     * @param id
     * @param multicast
     */
    protected DNSMessage(int flags, int id, boolean multicast) {
        super();
        _flags = flags;
        _id = id;
        _multicast = multicast;
        _questions = Collections.synchronizedList(new LinkedList<DNSQuestion>());
        _answers = Collections.synchronizedList(new LinkedList<DNSRecord>());
        _authoritativeAnswers = Collections.synchronizedList(new LinkedList<DNSRecord>());
        _additionals = Collections.synchronizedList(new LinkedList<DNSRecord>());
    }

    // public DatagramPacket getPacket() {
    // return _packet;
    // }
    //
    // public int getOffset() {
    // return _off;
    // }
    //
    // public int getLength() {
    // return _len;
    // }
    //
    // public byte[] getData() {
    // if ( _data == null ) _data = new byte[DNSConstants.MAX_MSG_TYPICAL];
    // return _data;
    // }

    /**
     * @return message id
     */
    public int getId() {
        return (_multicast ? 0 : _id);
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id) {
        this._id = id;
    }

    /**
     * @return message flags
     */
    public int getFlags() {
        return _flags;
    }

    /**
     * @param flags
     *            the flags to set
     */
    public void setFlags(int flags) {
        this._flags = flags;
    }

    /**
     * @return true if multicast
     */
    public boolean isMulticast() {
        return _multicast;
    }

    /**
     * @return list of questions
     */
    public Collection<? extends DNSQuestion> getQuestions() {
        return _questions;
    }

    /**
     * @return number of questions in the message
     */
    public int getNumberOfQuestions() {
        return this.getQuestions().size();
    }

    public Collection<? extends DNSRecord> getAllAnswers() {
        List<DNSRecord> aList = new ArrayList<DNSRecord>(_answers.size() + _authoritativeAnswers.size() + _additionals.size());
        aList.addAll(_answers);
        aList.addAll(_authoritativeAnswers);
        aList.addAll(_additionals);
        return aList;
    }

    /**
     * @return list of answers
     */
    public Collection<? extends DNSRecord> getAnswers() {
        return _answers;
    }

    /**
     * @return number of answers in the message
     */
    public int getNumberOfAnswers() {
        return this.getAnswers().size();
    }

    /**
     * @return list of authorities
     */
    public Collection<? extends DNSRecord> getAuthorities() {
        return _authoritativeAnswers;
    }

    /**
     * @return number of authorities in the message
     */
    public int getNumberOfAuthorities() {
        return this.getAuthorities().size();
    }

    /**
     * @return list of additional answers
     */
    public Collection<? extends DNSRecord> getAdditionals() {
        return _additionals;
    }

    /**
     * @return number of additional in the message
     */
    public int getNumberOfAdditionals() {
        return this.getAdditionals().size();
    }

    /**
     * Check is the response code is valid<br/>
     * The only valid value is zero all other values signify an error and the message must be ignored.
     *
     * @return true if the message has a valid response code.
     */
    public boolean isValidResponseCode() {
        return (_flags & DNSConstants.FLAGS_RCODE) == 0;
    }

    /**
     * Returns the operation code value. Currently only standard query 0 is valid.
     *
     * @return The operation code value.
     */
    public int getOperationCode() {
        return (_flags & DNSConstants.FLAGS_OPCODE) >> 11;
    }

    /**
     * Check if the message is truncated.
     *
     * @return true if the message was truncated
     */
    public boolean isTruncated() {
        return (_flags & DNSConstants.FLAGS_TC) != 0;
    }

    /**
     * Check if the message is an authoritative answer.
     *
     * @return true if the message is an authoritative answer
     */
    public boolean isAuthoritativeAnswer() {
        return (_flags & DNSConstants.FLAGS_AA) != 0;
    }

    /**
     * Check if the message is a query.
     *
     * @return true is the message is a query
     */
    public boolean isQuery() {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is a response.
     *
     * @return true is the message is a response
     */
    public boolean isResponse() {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_RESPONSE;
    }

    /**
     * Check if the message is empty
     *
     * @return true is the message is empty
     */
    public boolean isEmpty() {
        return (this.getNumberOfQuestions() + this.getNumberOfAnswers() + this.getNumberOfAuthorities() + this.getNumberOfAdditionals()) == 0;
    }

    /**
     * Debugging.
     */
    String print() {
        StringBuffer buf = new StringBuffer(200);
        buf.append(this.toString());
        buf.append("\n");
        for (DNSQuestion question : _questions) {
            buf.append("\tquestion:      ");
            buf.append(question);
            buf.append("\n");
        }
        for (DNSRecord answer : _answers) {
            buf.append("\tanswer:        ");
            buf.append(answer);
            buf.append("\n");
        }
        for (DNSRecord answer : _authoritativeAnswers) {
            buf.append("\tauthoritative: ");
            buf.append(answer);
            buf.append("\n");
        }
        for (DNSRecord answer : _additionals) {
            buf.append("\tadditional:    ");
            buf.append(answer);
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * Debugging.
     *
     * @param data
     * @return data dump
     */
    protected String print(byte[] data) {
        StringBuilder buf = new StringBuilder(4000);
        for (int off = 0, len = data.length; off < len; off += 32) {
            int n = Math.min(32, len - off);
            if (off < 0x10) {
                buf.append(' ');
            }
            if (off < 0x100) {
                buf.append(' ');
            }
            if (off < 0x1000) {
                buf.append(' ');
            }
            buf.append(Integer.toHexString(off));
            buf.append(':');
            int index = 0;
            for (index = 0; index < n; index++) {
                if ((index % 8) == 0) {
                    buf.append(' ');
                }
                buf.append(Integer.toHexString((data[off + index] & 0xF0) >> 4));
                buf.append(Integer.toHexString((data[off + index] & 0x0F) >> 0));
            }
            // for incomplete lines
            if (index < 32) {
                for (int i = index; i < 32; i++) {
                    if ((i % 8) == 0) {
                        buf.append(' ');
                    }
                    buf.append("  ");
                }
            }
            buf.append("    ");
            for (index = 0; index < n; index++) {
                if ((index % 8) == 0) {
                    buf.append(' ');
                }
                int ch = data[off + index] & 0xFF;
                buf.append(((ch > ' ') && (ch < 127)) ? (char) ch : '.');
            }
            buf.append("\n");

            // limit message size
            if (off + 32 >= 2048) {
                buf.append("....\n");
                break;
            }
        }
        return buf.toString();
    }

}
