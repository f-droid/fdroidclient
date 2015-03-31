package org.spongycastle.pqc.jcajce.provider.rainbow;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA224Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.pqc.crypto.rainbow.RainbowSigner;

/**
 * Rainbow Signature class, extending the jce SignatureSpi.
 */
public class SignatureSpi
    extends java.security.SignatureSpi
{
    private Digest digest;
    private RainbowSigner signer;
    private SecureRandom random;

    protected SignatureSpi(Digest digest, RainbowSigner signer)
    {
        this.digest = digest;
        this.signer = signer;
    }

    protected void engineInitVerify(PublicKey publicKey)
        throws InvalidKeyException
    {
        CipherParameters param;
        param = RainbowKeysToParams.generatePublicKeyParameter(publicKey);

        digest.reset();
        signer.init(false, param);
    }

    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
        throws InvalidKeyException
    {
        this.random = random;
        engineInitSign(privateKey);
    }

    protected void engineInitSign(PrivateKey privateKey)
        throws InvalidKeyException
    {
        CipherParameters param;
        param = RainbowKeysToParams.generatePrivateKeyParameter(privateKey);

        if (random != null)
        {
            param = new ParametersWithRandom(param, random);
        }

        digest.reset();
        signer.init(true, param);

    }

    protected void engineUpdate(byte b)
        throws SignatureException
    {
        digest.update(b);
    }

    protected void engineUpdate(byte[] b, int off, int len)
        throws SignatureException
    {
        digest.update(b, off, len);
    }

    protected byte[] engineSign()
        throws SignatureException
    {
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        try
        {
            byte[] sig = signer.generateSignature(hash);

            return sig;
        }
        catch (Exception e)
        {
            throw new SignatureException(e.toString());
        }
    }

    protected boolean engineVerify(byte[] sigBytes)
        throws SignatureException
    {
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return signer.verifySignature(hash, sigBytes);
    }

    protected void engineSetParameter(AlgorithmParameterSpec params)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated replaced with <a href =
     *             "#engineSetParameter(java.security.spec.AlgorithmParameterSpec)"
     *             >
     */
    protected void engineSetParameter(String param, Object value)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated
     */
    protected Object engineGetParameter(String param)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }


    static public class withSha224
        extends SignatureSpi
    {
        public withSha224()
        {
            super(new SHA224Digest(), new RainbowSigner());
        }
    }

    static public class withSha256
        extends SignatureSpi
    {
        public withSha256()
        {
            super(new SHA256Digest(), new RainbowSigner());
        }
    }

    static public class withSha384
        extends SignatureSpi
    {
        public withSha384()
        {
            super(new SHA384Digest(), new RainbowSigner());
        }
    }

    static public class withSha512
        extends SignatureSpi
    {
        public withSha512()
        {
            super(new SHA512Digest(), new RainbowSigner());
        }
    }
}
