
package java.security.cert;

import java.security.GeneralSecurityException;

public class CRLException extends GeneralSecurityException
{
    public CRLException()
    {
    }

    public CRLException(String msg)
    {
        super(msg);
    }
}
