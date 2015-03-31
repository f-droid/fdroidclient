package java.security.cert;

/**
 * A selector that defines a set of criteria for selecting <code>CRL</code>s.
 * Classes that implement this interface are often used to specify
 * which <code>CRL</code>s should be retrieved from a <code>CertStore</code>.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this interface are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CRL
 * @see CertStore
 * @see CertStore#getCRLs
 **/
public interface CRLSelector extends Cloneable
{
    /**
     * Decides whether a <code>CRL</code> should be selected.
     *
     * @param crl the <code>CRL</code> to be checked
     *
     * @return <code>true</code> if the <code>CRL</code> should be selected, 
     * <code>false</code> otherwise
     */
    public boolean match(CRL crl);

    /**
     * Makes a copy of this <code>CRLSelector</code>. Changes to the 
     * copy will not affect the original and vice versa.
     *
     * @return a copy of this <code>CRLSelector</code>
     */
    public Object clone();
}
