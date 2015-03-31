package java.security.cert;

/**
 * A specification of <code>CertStore</code> parameters.<br />
 * <br />
 * The purpose of this interface is to group (and provide type safety for) 
 * all <code>CertStore</code> parameter specifications. All 
 * <code>CertStore</code> parameter specifications must implement this 
 * interface.  <br />
 * <br />
 * Typically, a <code>CertStoreParameters</code> object is passed as a parameter
 * to one of the {@link CertStore#getInstance CertStore.getInstance} methods. 
 * The <code>getInstance</code> method returns a <code>CertStore</code> that
 * is used for retrieving <code>Certificate</code>s and <code>CRL</code>s. The 
 * <code>CertStore</code> that is returned is initialized with the specified 
 * parameters. The type of parameters needed may vary between different types 
 * of <code>CertStore</code>s.
 *
 * @see CertStore#getInstance
 **/
public interface CertStoreParameters extends Cloneable
{
    /**
     * Makes a copy of this <code>CertStoreParameters</code>.<br />
     * <br />
     * The precise meaning of "copy" may depend on the class of 
     * the <code>CertStoreParameters</code> object. A typical implementation 
     * performs a "deep copy" of this object, but this is not an absolute 
     * requirement. Some implementations may perform a "shallow copy" of some 
     * or all of the fields of this object.<br />
     * <br />
     * Note that the <code>CertStore.getInstance</code> methods make a copy
     * of the specified <code>CertStoreParameters</code>. A deep copy
     * implementation of <code>clone</code> is safer and more robust, as it 
     * prevents the caller from corrupting a shared <code>CertStore</code> by
     * subsequently modifying the contents of its initialization parameters. 
     * However, a shallow copy implementation of <code>clone</code> is more 
     * appropriate for applications that need to hold a reference to a
     * parameter contained in the <code>CertStoreParameters</code>. For example,
     * a shallow copy clone allows an application to release the resources of 
     * a particular <code>CertStore</code> initialization parameter immediately,
     * rather than waiting for the garbage collection mechanism. This should 
     * be done with the utmost care, since the <code>CertStore</code> may still 
     * be in use by other threads.<br />
     * <br />
     * Each subclass should state the precise behavior of this method so
     * that users and developers know what to expect.
     *
     * @return a copy of this <code>CertStoreParameters</code>
     */
    public Object clone();
}
