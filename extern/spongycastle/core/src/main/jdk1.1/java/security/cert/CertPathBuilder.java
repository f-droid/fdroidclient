package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * A class for building certification paths (also known as certificate
 * chains).<br />
 * <br />
 * This class uses a provider-based architecture, as described in the
 * Java Cryptography Architecture. To create a
 * <code>CertPathBuilder</code>, call one of the static
 * <code>getInstance</code> methods, passing in the algorithm name of
 * the CertPathBuilder desired and optionally the name of the provider
 * desired.<br />
 * <br />
 * Once a <code>CertPathBuilder</code> object has been created,
 * certification paths can be constructed by calling the
 * {@link #build build} method and passing it an algorithm-specific set
 * of parameters. If successful, the result (including the CertPath
 * that was built) is returned in an object that implements the
 * <code>CertPathBuilderResult</code> interface.<br />
 * <br />
 * <strong>Concurrent Access</strong><br />
 * <br />
 * The static methods of this class are guaranteed to be
 * thread-safe. Multiple threads may concurrently invoke the static
 * methods defined in this class with no ill effects.<br />
 * <br />
 * However, this is not true for the non-static methods defined by
 * this class. Unless otherwise documented by a specific provider,
 * threads that need to access a single <code>CertPathBuilder</code>
 * instance concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating a
 * different <code>CertPathBuilder</code> instance need not
 * synchronize.<br />
 * <br />
 * Uses {@link CertUtil CertUtil} to actualiy load the SPI classes.
 *
 * @see CertUtil
 **/
public class CertPathBuilder extends Object
{
    private CertPathBuilderSpi    builderSpi;
    private Provider        provider;
    private String        algorithm;

    /**
     * Creates a CertPathBuilder object of the given algorithm, and
     * encapsulates the given provider implementation (SPI object)
     * in it.
     *
     * @param builderSpi the provider implementation
     * @param provider the provider
     * @param algorithm the algorithm name
     **/
    protected CertPathBuilder(CertPathBuilderSpi builderSpi,
                  Provider provider,
                  String algorithm)
    {
    this.builderSpi = builderSpi;
    this.provider = provider;
    this.algorithm = algorithm;
    }

    /**
     * Returns a CertPathBuilder object that implements the
     * specified algorithm.<br />
     * <br />
     * If the default provider package provides an implementation
     * of the specified CertPathBuilder algorithm, an instance of
     * CertPathBuilder containing that implementation is
     * returned. If the requested algorithm is not available in
     * the default package, other packages are searched.<br />
     * <br />
     * @param algorithm the name of the requested CertPathBuilder algorithm
     *
     * @return a CertPathBuilder object that implements the
     * specified algorithm
     *
     * @exception NoSuchAlgorithmException  if the requested
     * algorithm is not available in the default provider package
     * or any of the other provider packages that were searched
     **/
    public static CertPathBuilder getInstance(String algorithm)
    throws NoSuchAlgorithmException
    {
    try {
        CertUtil.Implementation imp = 
        CertUtil.getImplementation("CertPathBuilder", algorithm, (String)null);
        if (imp != null)
        {
        return new CertPathBuilder((CertPathBuilderSpi)imp.getEngine(),
                       imp.getProvider(), algorithm);
        }
    } catch ( NoSuchProviderException ex ) {}
    throw new NoSuchAlgorithmException("can't find type " + algorithm);
    }

    /**
     * Returns a CertPathBuilder object that implements the
     * specified algorithm, as supplied by the specified provider.
     *
     * @param algorithm the name of the requested CertPathBuilder
     * algorithm
     * @param provider the name of the provider
     *
     * @return a CertPathBuilder object that implements the
     * specified algorithm, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested algorithm
     * is not available from the specified provider
     * @exception NoSuchProviderException if the provider has not
     * been configured
     * @exception IllegalArgumentException if the provider is null
     **/
    public static CertPathBuilder getInstance(String algorithm,
                          String provider)
    throws NoSuchAlgorithmException,
    NoSuchProviderException
    {
    if ( provider == null )
        throw new IllegalArgumentException("provider must be non-null");
    CertUtil.Implementation imp = 
        CertUtil.getImplementation("CertPathBuilder", algorithm, provider);

    if (imp != null)
    {
        return new CertPathBuilder((CertPathBuilderSpi)imp.getEngine(),
                       imp.getProvider(), algorithm);
    }
    throw new NoSuchAlgorithmException("can't find type " + algorithm);
    }

    /**
     * Returns a CertPathBuilder object that implements the
     * specified algorithm, as supplied by the specified
     * provider. Note: the provider doesn't have to be registered.
     *
     * @param algorithm the name of the requested CertPathBuilder
     * algorithm
     * @param provider the provider
     * @return a CertPathBuilder object that implements the
     * specified algorithm, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested algorithm
     * is not available from the specified provider
     * @exception IllegalArgumentException if the provider is null.
     **/
    public static CertPathBuilder getInstance(String algorithm,
                          Provider provider)
    throws NoSuchAlgorithmException
    {
    if ( provider == null )
        throw new IllegalArgumentException("provider must be non-null");
    CertUtil.Implementation imp =
        CertUtil.getImplementation("CertPathBuilder", algorithm, provider);

    if (imp != null)
    {
        return new CertPathBuilder((CertPathBuilderSpi)imp.getEngine(),
                       provider, algorithm);
    }
    throw new NoSuchAlgorithmException("can't find type " + algorithm);
    }

    /**
     * Returns the provider of this <code>CertPathBuilder</code>.
     *
     * @return the provider of this <code>CertPathBuilder</code>
     **/
    public final Provider getProvider()
    {
    return provider;
    }

    /**
     * Returns the name of the algorithm of this
     * <code>CertPathBuilder</code>.
     *
     * @return the name of the algorithm of this <code>CertPathBuilder</code>
     **/
    public final String getAlgorithm()
    {
    return algorithm;
    }

    /**
     * Attempts to build a certification path using the specified algorithm
     * parameter set.
     *
     * @param params the algorithm parameters
     *
     * @return the result of the build algorithm
     *
     * @exception CertPathBuilderException if the builder is unable to construct 
     *  a certification path that satisfies the specified parameters
     * @exception InvalidAlgorithmParameterException if the specified parameters * are inappropriate for this <code>CertPathBuilder</code>
     */
    public final CertPathBuilderResult build(CertPathParameters params)
    throws CertPathBuilderException,
    InvalidAlgorithmParameterException
    {
    return builderSpi.engineBuild(params);
    }


    /**
     * Returns the default <code>CertPathBuilder</code> type as specified in
     * the Java security properties file, or the string &quot;PKIX&quot;
     * if no such property exists. The Java security properties file is
     * located in the file named &lt;JAVA_HOME&gt;/lib/security/java.security,
     * where &lt;JAVA_HOME&gt; refers to the directory where the SDK was
     * installed.<br />
     * <br />
     * The default <code>CertPathBuilder</code> type can be used by
     * applications that do not want to use a hard-coded type when calling one
     * of the <code>getInstance</code> methods, and want to provide a default
     * type in case a user does not specify its own.<br />
     * <br />
     * The default <code>CertPathBuilder</code> type can be changed by
     * setting the value of the "certpathbuilder.type" security property
     * (in the Java security properties file) to the desired type.
     *
     * @return the default <code>CertPathBuilder</code> type as specified
     * in the Java security properties file, or the string &quot;PKIX&quot;
     * if no such property exists.
     */
    public static final String getDefaultType()
    {
    String defaulttype = null;
    defaulttype = Security.getProperty("certpathbuilder.type");

    if ( defaulttype == null || defaulttype.length() <= 0 )
        return "PKIX";
    else
        return defaulttype;
    }
}

