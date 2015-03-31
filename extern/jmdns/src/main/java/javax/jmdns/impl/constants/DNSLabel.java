/**
 *
 */
package javax.jmdns.impl.constants;

/**
 * DNS label.
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSLabel {
    /**
     * This is unallocated.
     */
    Unknown("", 0x80),
    /**
     * Standard label [RFC 1035]
     */
    Standard("standard label", 0x00),
    /**
     * Compressed label [RFC 1035]
     */
    Compressed("compressed label", 0xC0),
    /**
     * Extended label [RFC 2671]
     */
    Extended("extended label", 0x40);

    /**
     * DNS label types are encoded on the first 2 bits
     */
    static final int     LABEL_MASK     = 0xC0;
    static final int     LABEL_NOT_MASK = 0x3F;

    private final String _externalName;

    private final int    _index;

    DNSLabel(String name, int index) {
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
     * @param index
     * @return label
     */
    public static DNSLabel labelForByte(int index) {
        int maskedIndex = index & LABEL_MASK;
        for (DNSLabel aLabel : DNSLabel.values()) {
            if (aLabel._index == maskedIndex) return aLabel;
        }
        return Unknown;
    }

    /**
     * @param index
     * @return masked value
     */
    public static int labelValue(int index) {
        return index & LABEL_NOT_MASK;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
