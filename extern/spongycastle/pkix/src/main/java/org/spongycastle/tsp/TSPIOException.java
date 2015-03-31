package org.spongycastle.tsp;

import java.io.IOException;

public class TSPIOException
    extends IOException
{
    Throwable underlyingException;

    public TSPIOException(String message)
    {
        super(message);
    }

    public TSPIOException(String message, Throwable e)
    {
        super(message);
        underlyingException = e;
    }

    public Exception getUnderlyingException()
    {
        return (Exception)underlyingException;
    }

    public Throwable getCause()
    {
        return underlyingException;
    }
}
