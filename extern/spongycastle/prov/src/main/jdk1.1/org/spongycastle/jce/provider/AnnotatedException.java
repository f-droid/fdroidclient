package org.spongycastle.jce.provider;

public class AnnotatedException
    extends Exception
{
    private Throwable _underlyingException;

    AnnotatedException(String string, Throwable e)
    {
        super(string);

        _underlyingException = e;
    }

    AnnotatedException(String string)
    {
        this(string, null);
    }

    Throwable getUnderlyingException()
    {
        return _underlyingException;
    }

    public Throwable getCause()
    {
        return _underlyingException;
    }
}
