
package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public abstract class AlgorithmParametersSpi extends Object
{
    public AlgorithmParametersSpi()
    {
    }

    protected abstract byte[] engineGetEncoded()
        throws IOException;
    protected abstract byte[] engineGetEncoded(String format)
        throws IOException;
    protected abstract AlgorithmParameterSpec engineGetParameterSpec(Class paramSpec)
        throws InvalidParameterSpecException;
    protected abstract void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException;
    protected abstract void engineInit(byte[] params)
        throws IOException;
    protected abstract void engineInit(byte[] params, String format)
        throws IOException;
    protected abstract String engineToString();
}
