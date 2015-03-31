package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An immutable sequence of certificates (a certification path).<br />
 * <br />
 * This is an abstract class that defines the methods common to all
 * CertPaths. Subclasses can handle different kinds of certificates
 * (X.509, PGP, etc.).<br />
 * <br />
 * All CertPath objects have a type, a list of Certificates, and one
 * or more supported encodings. Because the CertPath class is
 * immutable, a CertPath cannot change in any externally visible way
 * after being constructed. This stipulation applies to all public
 * fields and methods of this class and any added or overridden by
 * subclasses.<br />
 * <br />
 * The type is a String that identifies the type of Certificates in
 * the certification path. For each certificate cert in a
 * certification path certPath,
 * cert.getType().equals(certPath.getType()) must be true.<br />
 * <br />
 * The list of Certificates is an ordered List of zero or more
 * Certificates. This List and all of the Certificates contained in it
 * must be immutable.<br />
 * <br />
 * Each CertPath object must support one or more encodings so that the
 * object can be translated into a byte array for storage or
 * transmission to other parties. Preferably, these encodings should
 * be well-documented standards (such as PKCS#7). One of the encodings
 * supported by a CertPath is considered the default encoding. This
 * encoding is used if no encoding is explicitly requested (for the
 * {@link #getEncoded()} method, for instance).<br />
 * <br />
 * All CertPath objects are also Serializable. CertPath objects are
 * resolved into an alternate {@link CertPathRep} object during
 * serialization. This allows a CertPath object to be serialized into
 * an equivalent representation regardless of its underlying
 * implementation.<br />
 * <br />
 * CertPath objects can be created with a CertificateFactory or they
 * can be returned by other classes, such as a CertPathBuilder.<br />
 * <br />
 * By convention, X.509 CertPaths (consisting of X509Certificates),
 * are ordered starting with the target certificate and ending with a
 * certificate issued by the trust anchor. That is, the issuer of one
 * certificate is the subject of the following one. The certificate
 * representing the {@link TrustAnchor TrustAnchor} should not be included in the
 * certification path. Unvalidated X.509 CertPaths may not follow
 * these conventions. PKIX CertPathValidators will detect any
 * departure from these conventions that cause the certification path
 * to be invalid and throw a CertPathValidatorException.<br />
 * <br />
 * <strong>Concurrent Access</strong><br />
 * <br />
 * All CertPath objects must be thread-safe. That is, multiple threads
 * may concurrently invoke the methods defined in this class on a
 * single CertPath object (or more than one) with no ill effects. This
 * is also true for the List returned by CertPath.getCertificates.<br />
 * <br />
 * Requiring CertPath objects to be immutable and thread-safe allows
 * them to be passed around to various pieces of code without worrying
 * about coordinating access. Providing this thread-safety is
 * generally not difficult, since the CertPath and List objects in
 * question are immutable.
 *
 * @see CertificateFactory
 * @see CertPathBuilder
 */
public abstract class CertPath extends Object implements Serializable
{
    private String type;

    /**
     * Alternate <code>CertPath</code> class for serialization.
     **/
    protected static class CertPathRep
    implements Serializable
    {
    private String type;
    private byte[] data;

        /**
         * Creates a <code>CertPathRep</code> with the specified
         * type and encoded form of a certification path.
         *
         * @param type the standard name of a CertPath
         * @param typedata the encoded form of the certification
         * path
         **/
    protected CertPathRep(String type, byte[] data)
    {
        this.type = type;
        this.data = data;
    }

        /**
         * Returns a CertPath constructed from the type and data.
         *
         * @return the resolved CertPath object
         * @exception ObjectStreamException if a CertPath could not be constructed
         **/
    protected Object readResolve()
        throws ObjectStreamException
    {
        try {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        CertificateFactory cf = CertificateFactory.getInstance(type);
        return cf.generateCertPath(inStream);
        } catch ( CertificateException ce ) {
        throw new NotSerializableException(" java.security.cert.CertPath: " + type);
        }
    }
    }

    /**
     * Creates a CertPath of the specified type.
     * This constructor is protected because most users should use
     * a CertificateFactory to create CertPaths.
     * @param type the standard name of the type of Certificatesin this path
     **/
    protected CertPath(String type)
    {
    this.type = type;
    }
    
    /**
     * Returns the type of Certificates in this certification
     * path. This is the same string that would be returned by
     * {@link Certificate#getType() cert.getType()}  for all
     * Certificates in the certification path.
     *
     * @return the type of Certificates in this certification path (never null)
     **/
    public String getType()
    {
    return type;
    }

    /**
     * Returns an iteration of the encodings supported by this
     * certification path, with the default encoding
     * first. Attempts to modify the returned Iterator via its
     * remove method result in an UnsupportedOperationException.
     *
     * @return an Iterator over the names of the supported encodings (as Strings)
     **/
    public abstract Iterator getEncodings();

    /**
     * Compares this certification path for equality with the
     * specified object. Two CertPaths are equal if and only if
     * their types are equal and their certificate Lists (and by
     * implication the Certificates in those Lists) are equal. A
     * CertPath is never equal to an object that is not a
     * CertPath.<br />
     * <br />
     * This algorithm is implemented by this method. If it is
     * overridden, the behavior specified here must be maintained.
     *
     * @param other the object to test for equality with this
     * certification path
     *
     * @return true if the specified object is equal to this
     * certification path, false otherwise
     *
     * @see Object#hashCode() Object.hashCode()
     **/
    public boolean equals(Object other)
    {
    if (!( other instanceof CertPath ) )
        return false;

    CertPath otherCertPath = (CertPath)other;
    if ( ! getType().equals(otherCertPath.getType()) )
        return false;
    return getCertificates().equals(otherCertPath.getCertificates());
    }

    /**
     * Returns the hashcode for this certification path. The hash
     * code of a certification path is defined to be the result of
     * the following calculation:
     * <pre>
     *   hashCode = path.getType().hashCode();
     *   hashCode = 31 * hashCode + path.getCertificates().hashCode();
     * </pre>
     * This ensures that path1.equals(path2) implies that
     * path1.hashCode()==path2.hashCode() for any two
     * certification paths, path1 and path2, as required by the
     * general contract of Object.hashCode.
     *
     * @return The hashcode value for this certification path
     *
     * @see #equals(Object)
     **/
    public int hashCode()
    {
    return getType().hashCode() * 31 + getCertificates().hashCode();
    }
    
    /** 
     * Returns a string representation of this certification
     * path. This calls the toString method on each of the
     * Certificates in the path.
     *
     * @return a string representation of this certification path
     **/
    public String toString()
    {
    StringBuffer s = new StringBuffer();
    List certs = getCertificates();
    ListIterator iter = certs.listIterator();
    s.append('\n').append(getType()).append(" Cert Path: length = ").append(certs.size()).append("\n[\n");
    while ( iter.hasNext() ) {
        s.append("=========================================================Certificate ").append(iter.nextIndex()).append('\n');
        s.append(iter.next()).append('\n');
        s.append("========================================================Certificate end\n\n\n");
    }
    s.append("\n]");
    return s.toString();
    }

    /**
     * Returns the encoded form of this certification path, using
     * the default encoding.
     *
     * @return the encoded bytes
     *
     * @exception CertificateEncodingException if an encoding error occurs
     **/
    public abstract byte[] getEncoded()
    throws CertificateEncodingException;

    /**
     * Returns the encoded form of this certification path, using
     * the specified encoding.
     *
     * @param encoding the name of the encoding to use
     *
     * @return the encoded bytes
     *
     * @exception CertificateEncodingException if an encoding error
     * occurs or the encoding requested is not supported
     **/
    public abstract byte[] getEncoded(String encoding)
    throws CertificateEncodingException;

    /**
     * Returns the list of certificates in this certification
     * path. The List returned must be immutable and thread-safe. 
     *
     * @return an immutable List of Certificates (may be empty, but not null)
     **/
    public abstract List getCertificates();

    /**
     * Replaces the CertPath to be serialized with a CertPathRep
     * object.
     *
     * @return the CertPathRep to be serialized
     *
     * @exception ObjectStreamException if a CertPathRep object
     * representing this certification path could not be created
     **/
    protected Object writeReplace()
    throws ObjectStreamException
    {
    try {
        return new CertPathRep( getType(), getEncoded() );
    } catch ( CertificateException ce ) {
        throw new NotSerializableException( " java.security.cert.CertPath: " + getType() );
    }
    }
}

