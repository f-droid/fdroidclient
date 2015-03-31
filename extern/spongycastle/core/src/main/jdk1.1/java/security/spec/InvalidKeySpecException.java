
package java.security.spec;

import java.security.GeneralSecurityException;

public class InvalidKeySpecException extends GeneralSecurityException
{
    public InvalidKeySpecException()
    {
    }

    public InvalidKeySpecException(String msg)
    {
        super(msg);
    }
}
