package org.spongycastle.eac;

import java.io.IOException;

/**
 * General IOException thrown in the cert package and its sub-packages.
 */
public class EACIOException
    extends IOException
{
    private Throwable cause;

    public EACIOException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public EACIOException(String msg)
    {
        super(msg);
    }

    public Throwable getCause()
    {
        return cause;
    }
}
