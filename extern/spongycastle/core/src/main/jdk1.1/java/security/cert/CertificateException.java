
package java.security.cert;

import java.security.GeneralSecurityException;

public class CertificateException extends GeneralSecurityException
{
    public CertificateException()
    {
    }

    public CertificateException(String msg)
    {
        super(msg);
    }
}
