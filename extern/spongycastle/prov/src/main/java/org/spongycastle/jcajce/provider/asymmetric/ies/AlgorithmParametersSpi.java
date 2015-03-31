package org.spongycastle.jcajce.provider.asymmetric.ies;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.jce.spec.IESParameterSpec;

public class AlgorithmParametersSpi
    extends java.security.AlgorithmParametersSpi
{
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

    IESParameterSpec currentSpec;

    /**
     * in the absence of a standard way of doing it this will do for
     * now...
     */
    protected byte[] engineGetEncoded()
    {
        try
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(new DEROctetString(currentSpec.getDerivationV()));
            v.add(new DEROctetString(currentSpec.getEncodingV()));
            v.add(new DERInteger(currentSpec.getMacKeySize()));

            return new DERSequence(v).getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error encoding IESParameters");
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
        if (paramSpec == IESParameterSpec.class)
        {
            return currentSpec;
        }

        throw new InvalidParameterSpecException("unknown parameter spec passed to ElGamal parameters object.");
    }

    protected void engineInit(
        AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
    {
        if (!(paramSpec instanceof IESParameterSpec))
        {
            throw new InvalidParameterSpecException("IESParameterSpec required to initialise a IES algorithm parameters object");
        }

        this.currentSpec = (IESParameterSpec)paramSpec;
    }

    protected void engineInit(
        byte[] params)
        throws IOException
    {
        try
        {
            ASN1Sequence s = (ASN1Sequence)ASN1Primitive.fromByteArray(params);

            this.currentSpec = new IESParameterSpec(
                ((ASN1OctetString)s.getObjectAt(0)).getOctets(),
                ((ASN1OctetString)s.getObjectAt(0)).getOctets(),
                ((DERInteger)s.getObjectAt(0)).getValue().intValue());
        }
        catch (ClassCastException e)
        {
            throw new IOException("Not a valid IES Parameter encoding.");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IOException("Not a valid IES Parameter encoding.");
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
        return "IES Parameters";
    }
}
