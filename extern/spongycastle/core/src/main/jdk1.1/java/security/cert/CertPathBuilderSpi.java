package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

/**
 * The Service Provider Interface (SPI) for the CertPathBuilder
 * class. All CertPathBuilder implementations must include a class
 * (the SPI class) that extends this class (CertPathBuilderSpi) and
 * implements all of its methods. In general, instances of this class
 * should only be accessed through the CertPathBuilder class. For
 * details, see the Java Cryptography Architecture.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Instances of this class need not be protected against concurrent
 * access from multiple threads. Threads that need to access a single
 * CertPathBuilderSpi instance concurrently should synchronize amongst
 * themselves and provide the necessary locking before calling the
 * wrapping CertPathBuilder object.<br />
 * <br />
 * However, implementations of CertPathBuilderSpi may still encounter
 * concurrency issues, since multiple threads each manipulating a
 * different CertPathBuilderSpi instance need not synchronize.
 **/
public abstract class CertPathBuilderSpi
    extends Object
{
    /**
     * The default constructor.
     */
    public CertPathBuilderSpi() {}

    /**
     * Attempts to build a certification path using the specified
     * algorithm parameter set.
     *
     * @param params the algorithm parameters
     *
     * @return the result of the build algorithm
     *
     * @exception CertPathBuilderException if the builder is unable
     * to construct a certification path that satisfies the
     * specified
     * @exception parametersInvalidAlgorithmParameterException if the
     * specified parameters are inappropriate for this CertPathBuilder
     */
    public abstract CertPathBuilderResult engineBuild( CertPathParameters params )
    throws CertPathBuilderException,
    InvalidAlgorithmParameterException;
}
