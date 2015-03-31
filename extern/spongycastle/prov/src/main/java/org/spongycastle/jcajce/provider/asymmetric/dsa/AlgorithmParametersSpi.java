package org.spongycastle.jcajce.provider.asymmetric.dsa;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.x509.DSAParameter;

public class AlgorithmParametersSpi
    extends java.security.AlgorithmParametersSpi
{
    DSAParameterSpec currentSpec;

    protected boolean isASN1FormatString(String format)
    {
        return format == null || format.equals("ASN.1");
    }

    protected AlgorithmParameterSpec engineGetParameterSpec(
        Class paramSpec)
        throws InvalidParameterSpecException
    {
        if (paramSpec == null)
        {
            throw new NullPointerException("argument to getParameterSpec must not be null");
        }

        return localEngineGetParameterSpec(paramSpec);
    }

    /**
     * Return the X.509 ASN.1 structure DSAParameter.
     * <p/>
     * <pre>
     *  DSAParameter ::= SEQUENCE {
     *                   prime INTEGER, -- p
     *                   subprime INTEGER, -- q
     *                   base INTEGER, -- g}
     * </pre>
     */
    protected byte[] engineGetEncoded()
    {
        DSAParameter dsaP = new DSAParameter(currentSpec.getP(), currentSpec.getQ(), currentSpec.getG());

        try
        {
            return dsaP.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error encoding DSAParameters");
        }
    }

    protected byte[] engineGetEncoded(
        String format)
    {
        if (isASN1FormatString(format))
        {
            return engineGetEncoded();
        }

        return null;
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(
        Class paramSpec)
        throws InvalidParameterSpecException
    {
        if (paramSpec == DSAParameterSpec.class)
        {
            return currentSpec;
        }

        throw new InvalidParameterSpecException("unknown parameter spec passed to DSA parameters object.");
    }

    protected void engineInit(
        AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
    {
        if (!(paramSpec instanceof DSAParameterSpec))
        {
            throw new InvalidParameterSpecException("DSAParameterSpec required to initialise a DSA algorithm parameters object");
        }

        this.currentSpec = (DSAParameterSpec)paramSpec;
    }

    protected void engineInit(
        byte[] params)
        throws IOException
    {
        try
        {
            DSAParameter dsaP = DSAParameter.getInstance(ASN1Primitive.fromByteArray(params));

            currentSpec = new DSAParameterSpec(dsaP.getP(), dsaP.getQ(), dsaP.getG());
        }
        catch (ClassCastException e)
        {
            throw new IOException("Not a valid DSA Parameter encoding.");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IOException("Not a valid DSA Parameter encoding.");
        }
    }

    protected void engineInit(
        byte[] params,
        String format)
        throws IOException
    {
        if (isASN1FormatString(format) || format.equalsIgnoreCase("X.509"))
        {
            engineInit(params);
        }
        else
        {
            throw new IOException("Unknown parameter format " + format);
        }
    }

    protected String engineToString()
    {
        return "DSA Parameters";
    }
}
