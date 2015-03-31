
package java.security.cert;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class CertificateFactorySpi
{
    public CertificateFactorySpi()
    {
    }

    public abstract CRL engineGenerateCRL(InputStream inStream)
        throws CRLException;

    public abstract Collection engineGenerateCRLs(InputStream inStream)
        throws CRLException;

    public abstract Certificate engineGenerateCertificate(InputStream inStream)
        throws CertificateException;

    public abstract /*SK13 Vector*/ Collection engineGenerateCertificates(InputStream inStream)
        throws CertificateException;

    /**
     * Returns an iteration of the <code>CertPath</code> encodings supported 
     * by this certificate factory, with the default encoding first. See 
     * Appendix A in the 
     * Java Certification Path API Programmer's Guide
     * for information about standard encoding names.<br />
     * <br />
     * Attempts to modify the returned <code>Iterator</code> via its
     * <code>remove</code> method result in an
     * <code>UnsupportedOperationException</code>.<br />
     * <br />
     * This method was added to version 1.4 of the Java 2 Platform
     * Standard Edition. In order to maintain backwards compatibility with
     * existing service providers, this method cannot be <code>abstract</code>
     * and by default throws an <code>UnsupportedOperationException</code>.
     *
     * @return an <code>Iterator</code> over the names of the supported
     *         <code>CertPath</code> encodings (as <code>String</code>s)
     *
     * @exception UnsupportedOperationException if the method is not supported
     */
    public abstract Iterator engineGetCertPathEncodings();

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * the data read from the <code>InputStream</code> inStream. The data
     * is assumed to be in the default encoding.
     *
     * @param inStream an <code>InputStream</code> containing the data
     *
     * @return a <code>CertPath</code> initialized with the data from the
     *   <code>InputStream</code>
     *
     * @exception CertificateException if an exception occurs while decoding
     */
    public abstract CertPath engineGenerateCertPath(InputStream inStream)
        throws CertificateException;

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * the data read from the <code>InputStream</code> inStream. The data
     * is assumed to be in the specified encoding.<br />
     * <br />
     * This method was added to version 1.4 of the Java 2 Platform
     * Standard Edition. In order to maintain backwards compatibility with
     * existing service providers, this method cannot be <code>abstract</code>
     * and by default throws an <code>UnsupportedOperationException</code>.
     *
     * @param inStream an <code>InputStream</code> containing the data
     * @param encoding the encoding used for the data
     *
     * @return a <code>CertPath</code> initialized with the data from the
     *   <code>InputStream</code>
     *
     * @exception CertificateException if an exception occurs while decoding or
     *   the encoding requested is not supported
     * @exception UnsupportedOperationException if the method is not supported
     */
    public abstract CertPath engineGenerateCertPath(InputStream inStream, String encoding)
        throws CertificateException;

    /**
     * Generates a <code>CertPath</code> object and initializes it with
     * a <code>List</code> of <code>Certificate</code>s.<br />
     * <br />
     * The certificates supplied must be of a type supported by the
     * <code>CertificateFactory</code>. They will be copied out of the supplied
     * <code>List</code> object.<br />
     * <br />
     * This method was added to version 1.4 of the Java 2 Platform
     * Standard Edition. In order to maintain backwards compatibility with
     * existing service providers, this method cannot be <code>abstract</code>
     * and by default throws an <code>UnsupportedOperationException</code>.
     *
     * @param certificates a <code>List</code> of <code>Certificate</code>s
     *
     * @return a <code>CertPath</code> initialized with the supplied list of
     *   certificates
     *
     * @exception CertificateException if an exception occurs
     * @exception UnsupportedOperationException if the method is not supported
     */
    public abstract CertPath engineGenerateCertPath(List certificates)
        throws CertificateException;
}
