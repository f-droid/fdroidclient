package org.spongycastle.jcajce.provider.asymmetric.util;

import java.security.spec.InvalidKeySpecException;

public class ExtendedInvalidKeySpecException
    extends InvalidKeySpecException
{
    private Throwable cause;

    public ExtendedInvalidKeySpecException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
