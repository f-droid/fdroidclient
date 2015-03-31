package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

/**
 *
 * The <i>Service Provider Interface</i> (<b>SPI</b>) 
 * for the {@link CertPathValidator CertPathValidator} class. All 
 * <code>CertPathValidator</code> implementations must include a class (the
 * SPI class) that extends this class (<code>CertPathValidatorSpi</code>) 
 * and implements all of its methods. In general, instances of this class 
 * should only be accessed through the <code>CertPathValidator</code> class. 
 * For details, see the Java Cryptography Architecture.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Instances of this class need not be protected against concurrent
 * access from multiple threads. Threads that need to access a single
 * <code>CertPathValidatorSpi</code> instance concurrently should synchronize
 * amongst themselves and provide the necessary locking before calling the
 * wrapping <code>CertPathValidator</code> object.<br />
 * <br />
 * However, implementations of <code>CertPathValidatorSpi</code> may still
 * encounter concurrency issues, since multiple threads each
 * manipulating a different <code>CertPathValidatorSpi</code> instance need not
 * synchronize.
 **/
public abstract class CertPathValidatorSpi extends Object
{
    /**
     * The default constructor.
     */
    public CertPathValidatorSpi() {}

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
    public abstract CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params)
    throws CertPathValidatorException,
    InvalidAlgorithmParameterException;
}
