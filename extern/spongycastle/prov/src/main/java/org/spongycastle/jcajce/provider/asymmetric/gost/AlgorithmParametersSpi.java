package org.spongycastle.jcajce.provider.asymmetric.gost;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import org.spongycastle.jce.spec.GOST3410ParameterSpec;
import org.spongycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;

public class AlgorithmParametersSpi
    extends java.security.AlgorithmParametersSpi
{
    GOST3410ParameterSpec currentSpec;

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
     * Return the X.509 ASN.1 structure GOST3410Parameter.
     * <p/>
     * <pre>
     *  GOST3410Parameter ::= SEQUENCE {
     *                   prime INTEGER, -- p
     *                   subprime INTEGER, -- q
     *                   base INTEGER, -- a}
     * </pre>
     */
    protected byte[] engineGetEncoded()
    {
        GOST3410PublicKeyAlgParameters gost3410P = new GOST3410PublicKeyAlgParameters(new ASN1ObjectIdentifier(currentSpec.getPublicKeyParamSetOID()), new ASN1ObjectIdentifier(currentSpec.getDigestParamSetOID()), new ASN1ObjectIdentifier(currentSpec.getEncryptionParamSetOID()));

        try
        {
            return gost3410P.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error encoding GOST3410Parameters");
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
        if (paramSpec == GOST3410PublicKeyParameterSetSpec.class)
        {
            return currentSpec;
        }

        throw new InvalidParameterSpecException("unknown parameter spec passed to GOST3410 parameters object.");
    }

    protected void engineInit(
        AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
    {
        if (!(paramSpec instanceof GOST3410ParameterSpec))
        {
            throw new InvalidParameterSpecException("GOST3410ParameterSpec required to initialise a GOST3410 algorithm parameters object");
        }

        this.currentSpec = (GOST3410ParameterSpec)paramSpec;
    }

    protected void engineInit(
        byte[] params)
        throws IOException
    {
        try
        {
            ASN1Sequence seq = (ASN1Sequence)ASN1Primitive.fromByteArray(params);

            this.currentSpec = GOST3410ParameterSpec.fromPublicKeyAlg(
                new GOST3410PublicKeyAlgParameters(seq));
        }
        catch (ClassCastException e)
        {
            throw new IOException("Not a valid GOST3410 Parameter encoding.");
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IOException("Not a valid GOST3410 Parameter encoding.");
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
        return "GOST3410 Parameters";
    }

}
