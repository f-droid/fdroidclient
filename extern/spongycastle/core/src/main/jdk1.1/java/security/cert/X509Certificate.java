
package java.security.cert;

import java.math.BigInteger;
import java.security.Principal;
import java.util.Date;

public abstract class X509Certificate extends Certificate
implements X509Extension
{
    protected X509Certificate()
    {
        super("X.509");
    }

    public abstract void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException;
    public abstract void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException;
    public abstract int getBasicConstraints();
    public abstract Principal getIssuerDN();
    public abstract boolean[] getIssuerUniqueID();
    public abstract boolean[] getKeyUsage();
    public abstract Date getNotAfter();
    public abstract Date getNotBefore();
    public abstract BigInteger getSerialNumber();
    public abstract String getSigAlgName();
    public abstract String getSigAlgOID();
    public abstract byte[] getSigAlgParams();
    public abstract byte[] getSignature();
    public abstract Principal getSubjectDN();
    public abstract boolean[] getSubjectUniqueID();
    public abstract byte[] getTBSCertificate() throws CertificateEncodingException;
    public abstract int getVersion();
}
