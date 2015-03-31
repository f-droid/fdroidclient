package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * A class for validating certification paths (also known as certificate 
 * chains).<br />
 * <br />
 * This class uses a provider-based architecture, as described in the Java 
 * Cryptography Architecture. To create a <code>CertPathValidator</code>, 
 * call one of the static <code>getInstance</code> methods, passing in the 
 * algorithm name of the <code>CertPathValidator</code> desired and 
 * optionally the name of the provider desired. <br />
 * <br />
 * Once a <code>CertPathValidator</code> object has been created, it can
 * be used to validate certification paths by calling the {@link #validate
 * validate} method and passing it the <code>CertPath</code> to be validated
 * and an algorithm-specific set of parameters. If successful, the result is
 * returned in an object that implements the 
 * <code>CertPathValidatorResult</code> interface.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * The static methods of this class are guaranteed to be thread-safe.
 * Multiple threads may concurrently invoke the static methods defined in
 * this class with no ill effects.<br />
 * <br />
 * However, this is not true for the non-static methods defined by this class.
 * Unless otherwise documented by a specific provider, threads that need to
 * access a single <code>CertPathValidator</code> instance concurrently should
 * synchronize amongst themselves and provide the necessary locking. Multiple
 * threads each manipulating a different <code>CertPathValidator</code>
 * instance need not synchronize.<br />
 * <br />
 * Uses {@link CertUtil CertUtil} to actualiy load the SPI classes.
 *
 * @see CertPath
 * @see CertUtil
 **/
public class CertPathValidator extends Object
{
    private CertPathValidatorSpi validatorSpi;
    private Provider         provider;
    private String         algorithm;

    /**
     * Creates a <code>CertPathValidator</code> object of the given algorithm, 
     * and encapsulates the given provider implementation (SPI object) in it.
     *
     * @param validatorSpi the provider implementation
     * @param provider the provider
     * @param algorithm the algorithm name
     */
    protected CertPathValidator( CertPathValidatorSpi validatorSpi,
                 Provider provider,
                 String algorithm)
    {
    this.validatorSpi = validatorSpi;
    this.provider = provider;
    this.algorithm = algorithm;
    }

    /**
     * Returns a <code>CertPathValidator</code> object that implements the 
     * specified algorithm.<br />
     * <br />
     * If the default provider package provides an implementation of the
     * specified <code>CertPathValidator</code> algorithm, an instance of 
     * <code>CertPathValidator</code> containing that implementation is 
     * returned. If the requested algorithm is not available in the default 
     * package, other packages are searched.
     * 
     * @param algorithm the name of the requested <code>CertPathValidator</code>
     * algorithm
     *
     * @return a <code>CertPathValidator</code> object that implements the
     * specified algorithm
     *
     * @exception NoSuchAlgorithmException if the requested algorithm
     * is not available in the default provider package or any of the other
     * provider packages that were searched
     */
    public static CertPathValidator getInstance(String algorithm)
    throws NoSuchAlgorithmException
    {
    try {
        CertUtil.Implementation  imp =
        CertUtil.getImplementation("CertPathValidator", algorithm, (String)null );
        if (imp != null)
        {
        return new CertPathValidator((CertPathValidatorSpi)imp.getEngine(), imp.getProvider(), algorithm);
        }
    } catch (NoSuchProviderException ex ) {}
    throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    /**
     * Returns a <code>CertPathValidator</code> object that implements the
     * specified algorithm, as supplied by the specified provider.
     *
     * @param algorithm the name of the requested <code>CertPathValidator</code>
     * algorithm
     * @param provider the name of the provider
     *
     * @return a <code>CertPathValidator</code> object that implements the
     * specified algorithm, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested algorithm
     * is not available from the specified provider
     * @exception NoSuchProviderException if the provider has not been
     * configured
     * @exception IllegalArgumentException if the <code>provider</code> is
     * null
     */
    public static CertPathValidator getInstance(String algorithm,
                        String provider)
    throws NoSuchAlgorithmException,
    NoSuchProviderException
    {
    if ( provider == null )
        throw new IllegalArgumentException("provider must be non-null");

    CertUtil.Implementation  imp = CertUtil.getImplementation("CertPathValidator", algorithm, provider );
    if (imp != null)
    {
        return new CertPathValidator((CertPathValidatorSpi)imp.getEngine(), imp.getProvider(), algorithm);
    }
    throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    /**
     * Returns a <code>CertPathValidator</code> object that implements the
     * specified algorithm, as supplied by the specified provider.
     * Note: the <code>provider</code> doesn't have to be registered.
     *
     * @param algorithm the name of the requested 
     * <code>CertPathValidator</code> algorithm
     * @param provider the provider
     *
     * @return a <code>CertPathValidator</code> object that implements the
     * specified algorithm, as supplied by the specified provider
     *
     * @exception NoSuchAlgorithmException if the requested algorithm
     * is not available from the specified provider
     * @exception IllegalArgumentException if the <code>provider</code> is
     * null
     */
    public static CertPathValidator getInstance(String algorithm,
                        Provider provider)
    throws NoSuchAlgorithmException
    {
    if ( provider == null )
        throw new IllegalArgumentException("provider must be non-null");

    CertUtil.Implementation  imp = CertUtil.getImplementation("CertPathValidator", algorithm, provider );
    if (imp != null)
    {
        return new CertPathValidator((CertPathValidatorSpi)imp.getEngine(), provider, algorithm);
    }
    throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    /**
     * Returns the <code>Provider</code> of this
     * <code>CertPathValidator</code>.
     *
     * @return the <code>Provider</code> of this <code>CertPathValidator</code>
     */
    public final Provider getProvider()
    {
    return provider;
    }

    /**
     * Returns the algorithm name of this <code>CertPathValidator</code>.
     *
     * @return the algorithm name of this <code>CertPathValidator</code>
     */
    public final String getAlgorithm()
    {
    return algorithm;
    }

    /**
     * Validates the specified certification path using the specified 
     * algorithm parameter set.<br /> 
     * <br />
     * The <code>CertPath</code> specified must be of a type that is 
     * supported by the validation algorithm, otherwise an
     * <code>InvalidAlgorithmParameterException</code> will be thrown. For 
     * example, a <code>CertPathValidator</code> that implements the PKIX
     * algorithm validates <code>CertPath</code> objects of type X.509.
     *
     * @param certPath the <code>CertPath</code> to be validated
     * @param params the algorithm parameters
     *
     * @return the result of the validation algorithm
     *
     * @exception CertPathValidatorException if the <code>CertPath</code>
     * does not validate
     * @exception InvalidAlgorithmParameterException if the specified 
     * parameters or the type of the specified <code>CertPath</code> are 
     * inappropriate for this <code>CertPathValidator</code>
     */
    public final CertPathValidatorResult validate( CertPath certPath,
                           CertPathParameters params)
    throws CertPathValidatorException,
    InvalidAlgorithmParameterException
    {
    return validatorSpi.engineValidate( certPath, params );
    }


    /**
     * Returns the default <code>CertPathValidator</code> type as specified in 
     * the Java security properties file, or the string &quot;PKIX&quot;
     * if no such property exists. The Java security properties file is 
     * located in the file named &lt;JAVA_HOME&gt;/lib/security/java.security, 
     * where &lt;JAVA_HOME&gt; refers to the directory where the SDK was 
     * installed.<br />
     * <br />
     * The default <code>CertPathValidator</code> type can be used by 
     * applications that do not want to use a hard-coded type when calling one 
     * of the <code>getInstance</code> methods, and want to provide a default 
     * type in case a user does not specify its own.<br />
     * <br />
     * The default <code>CertPathValidator</code> type can be changed by 
     * setting the value of the "certpathvalidator.type" security property 
     * (in the Java security properties file) to the desired type.
     *
     * @return the default <code>CertPathValidator</code> type as specified 
     * in the Java security properties file, or the string &quot;PKIX&quot;
     * if no such property exists.
     */
    public static final String getDefaultType()
    {
    String defaulttype = null;
    defaulttype = Security.getProperty("certpathvalidator.type");

    if ( defaulttype == null || defaulttype.length() <= 0 )
        return "PKIX";
     else
         return defaulttype;
    }
}

