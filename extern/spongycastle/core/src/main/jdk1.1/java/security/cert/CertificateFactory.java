
package java.security.cert;

import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Uses {@link CertUtil CertUtil} to actualiy load the SPI classes.
 *
 * @see CertUtil
 **/
public class CertificateFactory
{
    private CertificateFactorySpi certFacSpi;
    private Provider provider;
    private String type;

    protected CertificateFactory(
        CertificateFactorySpi certFacSpi,
        Provider provider,
        String type)
    {
        this.certFacSpi = certFacSpi;
        this.provider = provider;
        this.type = type;
    }

    public final CRL generateCRL(InputStream inStream)
    throws CRLException
    {
        return certFacSpi.engineGenerateCRL(inStream);
    }

    public final Collection generateCRLs(InputStream inStream)
    throws CRLException
    {
        return certFacSpi.engineGenerateCRLs(inStream);
    }

    public final Certificate generateCertificate(InputStream inStream)
    throws CertificateException
    {
        return certFacSpi.engineGenerateCertificate(inStream);
    }

    public final /*Sk13 Vector*/ Collection generateCertificates(InputStream inStream)
    throws CertificateException
    {
        return certFacSpi.engineGenerateCertificates(inStream);
    }

    /**
     * Returns an iteration of the <code>CertPath</code> encodings supported 
     * by this certificate factory, with the default encoding first. See 
     * Appendix A in the 
     * Java Certification Path API Programmer's Guide for information about 
     * standard encoding names and their formats.<br />
     * <br />
     * Attempts to modify the returned <code>Iterator</code> via its 
     * <code>remove</code> method result in an 
     * <code>UnsupportedOperationException</code>.
     *
     * @return an <code>Iterator</code> over the names of the supported
     *         <code>CertPath</code> encodings (as <code>String</code>s)
     */
    public final Iterator getCertPathEncodings()
    {
        return certFacSpi.engineGetCertPathEncodings();
    }

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * the data read from the <code>InputStream</code> inStream. The data
     * is assumed to be in the default encoding. The name of the default
     * encoding is the first element of the <code>Iterator</code> returned by
     * the {@link #getCertPathEncodings getCertPathEncodings} method.
     *
     * @param inStream an <code>InputStream</code> containing the data
     *
     * @return a <code>CertPath</code> initialized with the data from the
     *   <code>InputStream</code>
     *
     * @exception CertificateException if an exception occurs while decoding
     */
    public final CertPath generateCertPath(InputStream inStream)
    throws CertificateException
    {
        return certFacSpi.engineGenerateCertPath(inStream);
    }

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * the data read from the <code>InputStream</code> inStream. The data
     * is assumed to be in the specified encoding. See Appendix A in the 
     * <a href="../../../../guide/security/certpath/CertPathProgGuide.html#AppA">
     * Java Certification Path API Programmer's Guide</a>
     * for information about standard encoding names and their formats.
     *
     * @param inStream an <code>InputStream</code> containing the data
     * @param encoding the encoding used for the data
     *
     * @return a <code>CertPath</code> initialized with the data from the
     *   <code>InputStream</code>
     *
     * @exception CertificateException if an exception occurs while decoding or
     *   the encoding requested is not supported
     */
    public final CertPath generateCertPath(InputStream inStream, String encoding)
    throws CertificateException
    {
        return certFacSpi.engineGenerateCertPath(inStream, encoding);
    }

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * a <code>List</code> of <code>Certificate</code>s.<br />
     * <br />
     * The certificates supplied must be of a type supported by the
     * <code>CertificateFactory</code>. They will be copied out of the supplied
     * <code>List</code> object.
     *
     * @param certificates a <code>List</code> of <code>Certificate</code>s
     *
     * @return a <code>CertPath</code> initialized with the supplied list of
     *   certificates
     *
     * @exception CertificateException if an exception occurs
     */
    public final CertPath generateCertPath(List certificates)
    throws CertificateException
    {
        return certFacSpi.engineGenerateCertPath( certificates );
    }

    public static final CertificateFactory getInstance(String type)
    throws CertificateException
    {
        try
        {
            CertUtil.Implementation  imp = CertUtil.getImplementation("CertificateFactory", type, (String)null);

            if (imp != null)
            {
                return new CertificateFactory((CertificateFactorySpi)imp.getEngine(), imp.getProvider(), type);
            }

            throw new CertificateException("can't find type " + type);
        }
        catch (NoSuchProviderException e)
        {
            throw new CertificateException(type + " not found");
        }
    }

    public static final CertificateFactory getInstance(
        String type,
        String provider)
    throws CertificateException, NoSuchProviderException
    {
        CertUtil.Implementation  imp = CertUtil.getImplementation("CertificateFactory", type, provider);

        if (imp != null)
        {
            return new CertificateFactory((CertificateFactorySpi)imp.getEngine(), imp.getProvider(), type);
        }

        throw new CertificateException("can't find type " + type);
    }

    public final Provider getProvider()
    {
        return provider;
    }

    public final String getType()
    {
        return type;
    }
}
