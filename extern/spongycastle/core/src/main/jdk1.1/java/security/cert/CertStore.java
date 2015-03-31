package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;

/**
 * A class for retrieving <code>Certificate</code>s and <code>CRL</code>s
 * from a repository.<br />
 * <br />
 * This class uses a provider-based architecture, as described in the
 * Java Cryptography Architecture.
 * To create a <code>CertStore</code>, call one of the static
 * <code>getInstance</code> methods, passing in the type of
 * <code>CertStore</code> desired, any applicable initialization parameters 
 * and optionally the name of the provider desired. <br />
 * <br />
 * Once the <code>CertStore</code> has been created, it can be used to 
 * retrieve <code>Certificate</code>s and <code>CRL</code>s by calling its
 * {@link #getCertificates(CertSelector selector) getCertificates} and
 * {@link #getCRLs(CRLSelector selector) getCRLs} methods.<br />
 * <br />
 * Unlike a {@link java.security.KeyStore KeyStore}, which provides access
 * to a cache of private keys and trusted certificates, a 
 * <code>CertStore</code> is designed to provide access to a potentially
 * vast repository of untrusted certificates and CRLs. For example, an LDAP 
 * implementation of <code>CertStore</code> provides access to certificates
 * and CRLs stored in one or more directories using the LDAP protocol and the
 * schema as defined in the RFC service attribute. See Appendix A in the
 * Java Certification Path API Programmer's Guide for more information about
 * standard <code>CertStore</code> types.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * All public methods of <code>CertStore</code> objects must be thread-safe. 
 * That is, multiple threads may concurrently invoke these methods on a
 * single <code>CertStore</code> object (or more than one) with no
 * ill effects. This allows a <code>CertPathBuilder</code> to search for a
 * CRL while simultaneously searching for further certificates, for instance.<br />
 * <br />
 * The static methods of this class are also guaranteed to be thread-safe.
 * Multiple threads may concurrently invoke the static methods defined in
 * this class with no ill effects.<br />
 * <br />
 * Uses {@link CertUtil CertUtil} to actualiy load the SPI classes.
 *
 * @see CertUtil
 **/
public class CertStore extends Object
{
    private CertStoreSpi    storeSpi;
    private Provider            provider;
    private String        type;
    private CertStoreParameters params;

    /**
     * Creates a <code>CertStore</code> object of the given type, and
     * encapsulates the given provider implementation (SPI object) in it.
     *
     * @param storeSpi the provider implementation
     * @param provider the provider
     * @param type the type
     * @param params the initialization parameters (may be <code>null</code>)
     */
    protected CertStore( CertStoreSpi storeSpi,
             Provider provider,
             String type,
             CertStoreParameters params )
    {
    this.storeSpi = storeSpi;
    this.provider = provider;
    this.type = type;
    this.params = params;
    }

    /**
     * Returns a <code>Collection</code> of <code>Certificate</code>s that
     * match the specified selector. If no <code>Certificate</code>s
     * match the selector, an empty <code>Collection</code> will be returned.<br />
     * <br />
     * For some <code>CertStore</code> types, the resulting
     * <code>Collection</code> may not contain <b>all</b> of the
     * <code>Certificate</code>s that match the selector. For instance,
     * an LDAP <code>CertStore</code> may not search all entries in the
     * directory. Instead, it may just search entries that are likely to
     * contain the <code>Certificate</code>s it is looking for.<br />
     * <br />
     * Some <code>CertStore</code> implementations (especially LDAP 
     * <code>CertStore</code>s) may throw a <code>CertStoreException</code> 
     * unless a non-null <code>CertSelector</code> is provided that 
     * includes specific criteria that can be used to find the certificates.
     * Issuer and/or subject names are especially useful criteria.
     *
     * @param selector A <code>CertSelector</code> used to select which
     *  <code>Certificate</code>s should be returned. Specify <code>null</code>
     *  to return all <code>Certificate</code>s (if supported).
     *
     * @return A <code>Collection</code> of <code>Certificate</code>s that
     *         match the specified selector (never <code>null</code>)
     * @exception CertStoreException if an exception occurs 
     */
    public final Collection getCertificates( CertSelector selector )
    throws CertStoreException
    {
    return storeSpi.engineGetCertificates( selector );
    }

    /**
     * Returns a <code>Collection</code> of <code>CRL</code>s that
     * match the specified selector. If no <code>CRL</code>s
     * match the selector, an empty <code>Collection</code> will be returned.<br />
     * <br />
     * For some <code>CertStore</code> types, the resulting
     * <code>Collection</code> may not contain <b>all</b> of the
     * <code>CRL</code>s that match the selector. For instance,
     * an LDAP <code>CertStore</code> may not search all entries in the
     * directory. Instead, it may just search entries that are likely to
     * contain the <code>CRL</code>s it is looking for.<br />
     * <br />
     * Some <code>CertStore</code> implementations (especially LDAP 
     * <code>CertStore</code>s) may throw a <code>CertStoreException</code> 
     * unless a non-null <code>CRLSelector</code> is provided that 
     * includes specific criteria that can be used to find the CRLs.
     * Issuer names and/or the certificate to be checked are especially useful.
     *
     * @param selector A <code>CRLSelector</code> used to select which
     *  <code>CRL</code>s should be returned. Specify <code>null</code>
     *  to return all <code>CRL</code>s (if supported).
     *
     * @return A <code>Collection</code> of <code>CRL</code>s that
     *         match the specified selector (never <code>null</code>)
     *
     * @exception CertStoreException if an exception occurs 
     */
    public final Collection getCRLs( CRLSelector selector )
    throws CertStoreException
    {
    return storeSpi.engineGetCRLs( selector );
    }

    /**
     * Returns a <code>CertStore</code> object that implements the specified
     * <code>CertStore</code> type and is initialized with the specified
     * parameters.<br />
     * <br />
     * If the default provider package provides an implementation
     * of the specified <code>CertStore</code> type, an instance of
     * <code>CertStore</code> containing that implementation is returned.
     * If the requested type is not available in the default package, other
     * packages are searched.<br />
     * <br />
     * The <code>CertStore</code> that is returned is initialized with the 
     * specified <code>CertStoreParameters</code>. The type of parameters 
     * needed may vary between different types of <code>CertStore</code>s.
     * Note that the specified <code>CertStoreParameters</code> object is 
     * cloned.
     * 
     * @param type the name of the requested <code>CertStore</code> type
     * @param params the initialization parameters (may be <code>null</code>)
     *
     * @return a <code>CertStore</code> object that implements the specified
     *  <code>CertStore</code> type
     *
     * @exception NoSuchAlgorithmException if the requested type is not
     *  available in the default provider package or any of the other provider
     *  packages that were searched
     * @exception InvalidAlgorithmParameterException if the specified
     * initialization parameters are inappropriate for this 
     * <code>CertStore</code>
     */
    public static CertStore getInstance( String type,
                     CertStoreParameters params)
    throws InvalidAlgorithmParameterException,
    NoSuchAlgorithmException
    {
    try {
        CertUtil.Implementation  imp =
        CertUtil.getImplementation( "CertStore", type, (String)null,
                        new Class[] { CertStoreParameters.class },
                        new Object[] { params } );
        if (imp != null)
        {
        return new CertStore((CertStoreSpi)imp.getEngine(), imp.getProvider(), type, params );
        }
    } catch ( NoSuchProviderException ex ) {}
    throw new NoSuchAlgorithmException("can't find type " + type);
    }

    /**
     * Returns a <code>CertStore</code> object that implements the specified
     * <code>CertStore</code> type, as supplied by the specified provider
     * and initialized with the specified parameters.<br />
     * <br />
     * The <code>CertStore</code> that is returned is initialized with the 
     * specified <code>CertStoreParameters</code>. The type of parameters 
     * needed may vary between different types of <code>CertStore</code>s.
     * Note that the specified <code>CertStoreParameters</code> object is 
     * cloned.
     *
     * @param type the requested <code>CertStore</code> type
     * @param params the initialization parameters (may be <code>null</code>)
     * @param provider the name of the provider
     *
     * @return a <code>CertStore</code> object that implements the
     * specified type, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested type is not
     *  available from the specified provider
     * @exception InvalidAlgorithmParameterException if the specified
     * initialization parameters are inappropriate for this 
     * <code>CertStore</code>
     * @exception NoSuchProviderException if the provider has not been configured
     * @exception IllegalArgumentException if the <code>provider</code> is
     * null
     */
    public static CertStore getInstance( String type,
                     CertStoreParameters params,
                     String provider)
    throws InvalidAlgorithmParameterException,
    NoSuchAlgorithmException,
    NoSuchProviderException,
    IllegalArgumentException 
    {
    if ( provider == null )
        throw new IllegalArgumentException( "provider must be non-null" );

    CertUtil.Implementation  imp =
        CertUtil.getImplementation( "CertStore", type, provider,
                    new Class[] { CertStoreParameters.class },
                    new Object[] { params } );
    if (imp != null)
    {
        return new CertStore((CertStoreSpi)imp.getEngine(), imp.getProvider(), type, params );
    }
    throw new NoSuchAlgorithmException("can't find type " + type);
    }
    
    /**
     * Returns a <code>CertStore</code> object that implements the specified
     * <code>CertStore</code> type, as supplied by the specified provider and
     * initialized with the specified parameters.
     * Note: the <code>provider</code> doesn't have to be registered.<br />
     * <br />
     * The <code>CertStore</code> that is returned is initialized with the 
     * specified <code>CertStoreParameters</code>. The type of parameters 
     * needed may vary between different types of <code>CertStore</code>s.
     * Note that the specified <code>CertStoreParameters</code> object is 
     * cloned.
     *
     * @param type the requested <code>CertStore</code> type
     * @param params the initialization parameters (may be <code>null</code>)
     * @param provider the provider
     *
     * @return a <code>CertStore</code> object that implements the
     * specified type, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested type is not
     *  available from the specified provider
     * @exception InvalidAlgorithmParameterException if the specified
     * initialization parameters are inappropriate for this 
     * <code>CertStore</code>
     * @exception IllegalArgumentException if the <code>provider</code> is
     * null 
     */
    public static CertStore getInstance( String type,
                     CertStoreParameters params,
                     Provider provider )
    throws NoSuchAlgorithmException,
    InvalidAlgorithmParameterException,
    IllegalArgumentException
    {
    if ( provider == null )
        throw new IllegalArgumentException( "provider must be non-null" );
    CertUtil.Implementation imp =
        CertUtil.getImplementation( "CertStore", type, provider,
                    new Class[] { CertStoreParameters.class },
                    new Object[] { params } );
    if (imp != null)
    {
        return new CertStore((CertStoreSpi)imp.getEngine(), provider, type, params );
    }
    throw new NoSuchAlgorithmException("can't find type " + type);
    }

    /**
     * Returns the parameters used to initialize this <code>CertStore</code>.
     * Note that the <code>CertStoreParameters</code> object is cloned before 
     * it is returned.
     *
     * @return the parameters used to initialize this <code>CertStore</code>
     * (may be <code>null</code>)
     */
    public final CertStoreParameters getCertStoreParameters()
    {
    return params;
    }

    /**
     * Returns the type of this <code>CertStore</code>.
     *
     * @return the type of this <code>CertStore</code>
     */
    public final String getType()
    {
    return type;
    }

    /**
     * Returns the provider of this <code>CertStore</code>.
     *
     * @return the provider of this <code>CertStore</code>
     */
    public final Provider getProvider()
    {
    return provider;
    }

    /**
     * Returns the default <code>CertStore</code> type as specified in the 
     * Java security properties file, or the string &quot;LDAP&quot; if no 
     * such property exists. The Java security properties file is located in 
     * the file named &lt;JAVA_HOME&gt;/lib/security/java.security, where 
     * &lt;JAVA_HOME&gt; refers to the directory where the SDK was installed.<br />
     * <br />
     * The default <code>CertStore</code> type can be used by applications 
     * that do not want to use a hard-coded type when calling one of the
     * <code>getInstance</code> methods, and want to provide a default 
     * <code>CertStore</code> type in case a user does not specify its own.<br />
     * <br />
     * The default <code>CertStore</code> type can be changed by setting 
     * the value of the "certstore.type" security property (in the Java 
     * security properties file) to the desired type.
     *
     * @return the default <code>CertStore</code> type as specified in the
     * Java security properties file, or the string &quot;LDAP&quot;
     * if no such property exists.
     */
    public static final String getDefaultType()
    {
    String defaulttype = null;
    defaulttype = Security.getProperty("certstore.type");

    if ( defaulttype == null || defaulttype.length() <= 0 )
        return "LDAP";
    else
        return defaulttype;
    }
}

