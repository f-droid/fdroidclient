package java.security.cert;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Parameters used as input for the Collection <code>CertStore</code>
 * algorithm.<br />
 * <br />
 * This class is used to provide necessary configuration parameters
 * to implementations of the Collection <code>CertStore</code>
 * algorithm. The only parameter included in this class is the
 * <code>Collection</code> from which the <code>CertStore</code> will
 * retrieve certificates and CRLs.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see         java.util.Collection
 * @see         CertStore
 **/
public class CollectionCertStoreParameters implements CertStoreParameters
{
    private Collection collection;

    /**
     * Creates an instance of <code>CollectionCertStoreParameters</code>
     * which will allow certificates and CRLs to be retrieved from the
     * specified <code>Collection</code>. If the specified
     * <code>Collection</code> contains an object that is not a
     * <code>Certificate</code> or <code>CRL</code>, that object will be
     * ignored by the Collection <code>CertStore</code>.<br />
     * <br />
     * The <code>Collection</code> is <b>not</b> copied. Instead, a
     * reference is used. This allows the caller to subsequently add or
     * remove <code>Certificates</code> or <code>CRL</code>s from the
     * <code>Collection</code>, thus changing the set of
     * <code>Certificates</code> or <code>CRL</code>s available to the
     * Collection <code>CertStore</code>. The Collection <code>CertStore</code>
     * will not modify the contents of the <code>Collection</code>.<br />
     * <br />
     * If the <code>Collection</code> will be modified by one thread while
     * another thread is calling a method of a Collection <code>CertStore</code>
     * that has been initialized with this <code>Collection</code>, the
     * <code>Collection</code> must have fail-fast iterators.
     *
     * @param collection a <code>Collection</code> of
     *        <code>Certificate</code>s and <code>CRL</code>s
     *
     * @exception NullPointerException if <code>collection</code> is
     * <code>null</code>
     */
    public CollectionCertStoreParameters(Collection collection)
    {
    if ( collection == null )
        throw new NullPointerException("collection must be non-null");
    this.collection = collection;
    }

    /**
     * Creates an instance of <code>CollectionCertStoreParameters</code> with 
     * the an empty Collection.
     */
    public CollectionCertStoreParameters()
    {
    collection = new ArrayList();
    }

    /**
     * Returns the <code>Collection</code> from which <code>Certificate</code>s
     * and <code>CRL</code>s are retrieved. This is <b>not</b> a copy of the
     * <code>Collection</code>, it is a reference. This allows the caller to
     * subsequently add or remove <code>Certificates</code> or
     * <code>CRL</code>s from the <code>Collection</code>.
     *
     * @return the <code>Collection</code> (never null)
     */
    public Collection getCollection()
    {
    return collection;
    }

    /**
     * Returns a copy of this object. Note that only a reference to the
     * <code>Collection</code> is copied, and not the contents.
     *
     * @return the copy
     */
    public Object clone()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            /* Cannot happen */
            throw new InternalError(e.toString());
        }
    }

    /**
     * Returns a formatted string describing the parameters.
     *
     * @return a formatted string describing the parameters
     */
    public String toString()
    {
    StringBuffer s = new StringBuffer();
    s.append("CollectionCertStoreParameters: [\n  collections:\n");
    s.append( getCollection());
    s.append("\n]" );
    return s.toString();
    }
}
