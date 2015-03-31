package org.spongycastle.jcajce.provider.asymmetric.elgamal;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.DHParameterSpec;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.oiw.ElGamalParameter;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.spongycastle.jce.spec.ElGamalParameterSpec;

public class AlgorithmParametersSpi
    extends BaseAlgorithmParameters
{
    ElGamalParameterSpec currentSpec;

    /**
     * Return the X.509 ASN.1 structure ElGamalParameter.
     * <p/>
     * <pre>
     *  ElGamalParameter ::= SEQUENCE {
     *                   prime INTEGER, -- p
     *                   base INTEGER, -- g}
     * </pre>
     */
    protected byte[] engineGetEncoded()
    {
        ElGamalParameter elP = new ElGamalParameter(currentSpec.getP(), currentSpec.getG());

        try
        {
            return elP.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error encoding ElGamalParameters");
        }
    }

    protected byte[] engineGetEncoded(
        String format)
    {
        if (isASN1FormatString(format) || format.equalsIgnoreCase("X.509"))
        {
            return engineGetEncoded();
        }

        return null;
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(
        Class paramSpec)
        throws InvalidParameterSpecException
    {
        if (paramSpec == ElGamalParameterSpec.class)
        {
            return currentSpec;
        }
        else if (paramSpec == DHParameterSpec.class)
        {
            return new DHParameterSpec(currentSpec.getP(), currentSpec.getG());
        }

        throw new InvalidParameterSpecException("unknown parameter spec passed to ElGamal parameters object.");
    }

    protected void engineInit(
        AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
    {
        if (!(paramSpec instanceof ElGamalParameterSpec) && !(paramSpec instanceof DHParameterSpec))
        {
            throw new InvalidParameterSpecException("DHParameterSpec required to initialise a ElGamal algorithm parameters object");
        }

        if (paramSpec instanceof ElGamalParameterSpec)
        {
            this.currentSpec = (ElGamalParameterSpec)paramSpec;
        }
        else
        {
            DHParameterSpec s = (DHParameterSpec)paramSpec;

            this.currentSpec = new ElGamalParameterSpec(s.getP(), s.getG());
        }
    }

    protected void engineInit(
        byte[] params)
        throws IOException
    {
        try
        {
            ElGamalParameter elP = new ElGamalParameter((ASN1Sequence)ASN1Primitive.fromByteArray(params));

            currentSpec = new ElGamalParameterSpec(elP.getP(), elP.getG());
        }
        catch (ClassCastException e)
        {
            throw new IOException("Not a valid ElGamal Parameter encoding.");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IOException("Not a valid ElGamal Parameter encoding.");
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
        return "ElGamal Parameters";
    }
}
