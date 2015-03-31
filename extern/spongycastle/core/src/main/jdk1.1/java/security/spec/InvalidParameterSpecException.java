
package java.security.spec;

import java.security.GeneralSecurityException;

public class InvalidParameterSpecException extends GeneralSecurityException
{
    public InvalidParameterSpecException()
    {
    }

    public InvalidParameterSpecException(String msg)
    {
        super(msg);
    }
}
