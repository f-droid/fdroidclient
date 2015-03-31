package org.spongycastle.jcajce.provider.asymmetric.dstu;

import java.io.IOException;
import java.security.SignatureException;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.DEROctetString;

public class SignatureSpiLe
    extends SignatureSpi
{
    void reverseBytes(byte[] bytes)
    {
        byte tmp;

        for (int i = 0; i < bytes.length / 2; i++)
        {
            tmp = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = tmp;
        }
    }

    protected byte[] engineSign()
        throws SignatureException
    {
        byte[] signature = ASN1OctetString.getInstance(super.engineSign()).getOctets();
        reverseBytes(signature);
        try
        {
            return (new DEROctetString(signature)).getEncoded();
        }
        catch (Exception e)
        {
            throw new SignatureException(e.toString());
        }
    }

    protected boolean engineVerify(
        byte[] sigBytes)
        throws SignatureException
    {
        byte[] bytes = null;

        try
        {
            bytes = ((ASN1OctetString)ASN1OctetString.fromByteArray(sigBytes)).getOctets();
        }
        catch (IOException e)
        {
            throw new SignatureException("error decoding signature bytes.");
        }

        reverseBytes(bytes);

        try
        {
            return super.engineVerify((new DEROctetString(bytes)).getEncoded());
        }
        catch (SignatureException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SignatureException(e.toString());
        }
    }
}
