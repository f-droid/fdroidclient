package org.spongycastle.jcajce.provider.asymmetric.util;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.crypto.DSA;
import org.spongycastle.crypto.Digest;

public abstract class DSABase
    extends SignatureSpi
    implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
    protected Digest digest;
    protected DSA                     signer;
    protected DSAEncoder              encoder;

    protected DSABase(
        Digest                  digest,
        DSA                     signer,
        DSAEncoder              encoder)
    {
        this.digest = digest;
        this.signer = signer;
        this.encoder = encoder;
    }

    protected void engineUpdate(
        byte    b)
        throws SignatureException
    {
        digest.update(b);
    }

    protected void engineUpdate(
        byte[]  b,
        int     off,
        int     len) 
        throws SignatureException
    {
        digest.update(b, off, len);
    }

    protected byte[] engineSign()
        throws SignatureException
    {
        byte[]  hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        try
        {
            BigInteger[]    sig = signer.generateSignature(hash);

            return encoder.encode(sig[0], sig[1]);
        }
        catch (Exception e)
        {
            throw new SignatureException(e.toString());
        }
    }

    protected boolean engineVerify(
        byte[]  sigBytes) 
        throws SignatureException
    {
        byte[]  hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        BigInteger[]    sig;

        try
        {
            sig = encoder.decode(sigBytes);
        }
        catch (Exception e)
        {
            throw new SignatureException("error decoding signature bytes.");
        }

        return signer.verifySignature(hash, sig[0], sig[1]);
    }

    protected void engineSetParameter(
        AlgorithmParameterSpec params)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated replaced with <a href = "#engineSetParameter(java.security.spec.AlgorithmParameterSpec)">
     */
    protected void engineSetParameter(
        String  param,
        Object  value)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated
     */
    protected Object engineGetParameter(
        String      param)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }
}
