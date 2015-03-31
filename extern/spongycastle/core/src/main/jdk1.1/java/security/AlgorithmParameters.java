
package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

public class AlgorithmParameters extends Object
{
    private AlgorithmParametersSpi spi;
    private Provider provider;
    private String algorithm;

    protected AlgorithmParameters(
        AlgorithmParametersSpi paramSpi,
        Provider provider,
        String algorithm)
    {
        this.spi = paramSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm()
    {
        return algorithm;
    }

    public final byte[] getEncoded() throws IOException
    {
        return spi.engineGetEncoded();
    }

    public final byte[] getEncoded(String format) throws IOException
    {
        return spi.engineGetEncoded(format);
    }

    public static AlgorithmParameters getInstance(String algorithm)
        throws NoSuchAlgorithmException
    {
        try
        {
            SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("AlgorithmParameters", algorithm, null);

            if (imp != null)
            {
                return new AlgorithmParameters((AlgorithmParametersSpi)imp.getEngine(), imp.getProvider(), algorithm);
            }

            throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
        }
        catch (NoSuchProviderException e)
        {
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
    }

    public static AlgorithmParameters getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("AlgorithmParameters", algorithm, provider);

        if (imp != null)
        {
            return new AlgorithmParameters((AlgorithmParametersSpi)imp.getEngine(), imp.getProvider(), algorithm);
        }

        throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    public final AlgorithmParameterSpec getParameterSpec(Class paramSpec)
    throws InvalidParameterSpecException
    {
        return spi.engineGetParameterSpec(paramSpec);
    }

    public final Provider getProvider()
    {
        return provider;
    }

    public final void init(AlgorithmParameterSpec paramSpec)
    throws InvalidParameterSpecException
    {
        spi.engineInit(paramSpec);
    }

    public final void init(byte[] params) throws IOException
    {
        spi.engineInit(params);
    }

    public final void init(byte[] params, String format) throws IOException
    {
        spi.engineInit(params, format);
    }

    public final String toString()
    {
        return spi.engineToString();
    }
}
