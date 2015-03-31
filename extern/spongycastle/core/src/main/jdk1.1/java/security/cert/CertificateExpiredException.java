
package java.security.cert;

public class CertificateExpiredException extends CertificateException
{
    public CertificateExpiredException()
    {
    }

    public CertificateExpiredException(String msg)
    {
        super(msg);
    }
}
