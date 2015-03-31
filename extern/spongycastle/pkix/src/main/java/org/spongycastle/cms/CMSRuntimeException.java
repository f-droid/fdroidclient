package org.spongycastle.cms;

public class CMSRuntimeException
    extends RuntimeException
{
    Exception   e;

    public CMSRuntimeException(
        String name)
    {
        super(name);
    }

    public CMSRuntimeException(
        String name,
        Exception e)
    {
        super(name);

        this.e = e;
    }

    public Exception getUnderlyingException()
    {
        return e;
    }
    
    public Throwable getCause()
    {
        return e;
    }
}
