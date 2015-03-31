/**
 *
 */
package javax.jmdns.impl.constants;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DNS Record Class
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSRecordClass {
    /**
     *
     */
    CLASS_UNKNOWN("?", 0),
    /**
     * static final Internet
     */
    CLASS_IN("in", 1),
    /**
     * CSNET
     */
    CLASS_CS("cs", 2),
    /**
     * CHAOS
     */
    CLASS_CH("ch", 3),
    /**
     * Hesiod
     */
    CLASS_HS("hs", 4),
    /**
     * Used in DNS UPDATE [RFC 2136]
     */
    CLASS_NONE("none", 254),
    /**
     * Not a DNS class, but a DNS query class, meaning "all classes"
     */
    CLASS_ANY("any", 255);

    private static Logger       logger       = Logger.getLogger(DNSRecordClass.class.getName());

    /**
     * Multicast DNS uses the bottom 15 bits to identify the record class...<br/>
     * Except for pseudo records like OPT.
     */
    public static final int     CLASS_MASK   = 0x7FFF;

    /**
     * For answers the top bit indicates that all other cached records are now invalid.<br/>
     * For questions it indicates that we should send a unicast response.
     */
    public static final int     CLASS_UNIQUE = 0x8000;

    /**
     *
     */
    public static final boolean UNIQUE       = true;

    /**
     *
     */
    public static final boolean NOT_UNIQUE   = false;

    private final String        _externalName;

    private final int           _index;

    DNSRecordClass(String name, int index) {
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
     * Checks if the class is unique
     * 
     * @param index
     * @return <code>true</code> is the class is unique, <code>false</code> otherwise.
     */
    public boolean isUnique(int index) {
        return (this != CLASS_UNKNOWN) && ((index & CLASS_UNIQUE) != 0);
    }

    /**
     * @param name
     * @return class for name
     */
    public static DNSRecordClass classForName(String name) {
        if (name != null) {
            String aName = name.toLowerCase();
            for (DNSRecordClass aClass : DNSRecordClass.values()) {
                if (aClass._externalName.equals(aName)) return aClass;
            }
        }
        logger.log(Level.WARNING, "Could not find record class for name: " + name);
        return CLASS_UNKNOWN;
    }

    /**
     * @param index
     * @return class for name
     */
    public static DNSRecordClass classForIndex(int index) {
        int maskedIndex = index & CLASS_MASK;
        for (DNSRecordClass aClass : DNSRecordClass.values()) {
            if (aClass._index == maskedIndex) return aClass;
        }
        logger.log(Level.WARNING, "Could not find record class for index: " + index);
        return CLASS_UNKNOWN;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
