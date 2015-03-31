package org.spongycastle.jcajce.provider.symmetric.util;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.IvParameterSpec;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.util.Arrays;

public class IvAlgorithmParameters
    extends BaseAlgorithmParameters
{
    private byte[] iv;

    protected byte[] engineGetEncoded()
        throws IOException
    {
        return engineGetEncoded("ASN.1");
    }

    protected byte[] engineGetEncoded(
        String format)
        throws IOException
    {
        if (isASN1FormatString(format))
        {
            return new DEROctetString(engineGetEncoded("RAW")).getEncoded();
        }

        if (format.equals("RAW"))
        {
            return Arrays.clone(iv);
        }

        return null;
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(
        Class paramSpec)
        throws InvalidParameterSpecException
    {
        if (paramSpec == IvParameterSpec.class)
        {
            return new IvParameterSpec(iv);
        }

        throw new InvalidParameterSpecException("unknown parameter spec passed to IV parameters object.");
    }

    protected void engineInit(
        AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException
    {
        if (!(paramSpec instanceof IvParameterSpec))
        {
            throw new InvalidParameterSpecException("IvParameterSpec required to initialise a IV parameters algorithm parameters object");
        }

        this.iv = ((IvParameterSpec)paramSpec).getIV();
    }

    protected void engineInit(
        byte[] params)
        throws IOException
    {
        //
        // check that we don't have a DER encoded octet string
        //
        if ((params.length % 8) != 0
            && params[0] == 0x04 && params[1] == params.length - 2)
        {
            ASN1OctetString oct = (ASN1OctetString)ASN1Primitive.fromByteArray(params);

            params = oct.getOctets();
        }

        this.iv = Arrays.clone(params);
    }

    protected void engineInit(
        byte[] params,
        String format)
        throws IOException
    {
        if (isASN1FormatString(format))
        {
            try
            {
                ASN1OctetString oct = (ASN1OctetString)ASN1Primitive.fromByteArray(params);

                engineInit(oct.getOctets());
            }
            catch (Exception e)
            {
                throw new IOException("Exception decoding: " + e);
            }

            return;
        }

        if (format.equals("RAW"))
        {
            engineInit(params);
            return;
        }

        throw new IOException("Unknown parameters format in IV parameters object");
    }

    protected String engineToString()
    {
        return "IV Parameters";
    }
}
