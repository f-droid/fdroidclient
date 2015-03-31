package org.spongycastle.crypto.tls;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.DSA;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.Signer;
import org.spongycastle.crypto.digests.NullDigest;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.signers.DSADigestSigner;

public abstract class TlsDSASigner
    extends AbstractTlsSigner
{
    public byte[] generateRawSignature(SignatureAndHashAlgorithm algorithm,
        AsymmetricKeyParameter privateKey, byte[] hash)
        throws CryptoException
    {
        Signer signer = makeSigner(algorithm, true, true,
            new ParametersWithRandom(privateKey, this.context.getSecureRandom()));
        if (algorithm == null)
        {
            // Note: Only use the SHA1 part of the (MD5/SHA1) hash
            signer.update(hash, 16, 20);
        }
        else
        {
            signer.update(hash, 0, hash.length);
        }
        return signer.generateSignature();
    }

    public boolean verifyRawSignature(SignatureAndHashAlgorithm algorithm, byte[] sigBytes,
        AsymmetricKeyParameter publicKey, byte[] hash)
        throws CryptoException
    {
        Signer signer = makeSigner(algorithm, true, false, publicKey);
        if (algorithm == null)
        {
            // Note: Only use the SHA1 part of the (MD5/SHA1) hash
            signer.update(hash, 16, 20);
        }
        else
        {
            signer.update(hash, 0, hash.length);
        }
        return signer.verifySignature(sigBytes);
    }

    public Signer createSigner(SignatureAndHashAlgorithm algorithm, AsymmetricKeyParameter privateKey)
    {
        return makeSigner(algorithm, false, true, new ParametersWithRandom(privateKey, this.context.getSecureRandom()));
    }

    public Signer createVerifyer(SignatureAndHashAlgorithm algorithm, AsymmetricKeyParameter publicKey)
    {
        return makeSigner(algorithm, false, false, publicKey);
    }

    protected Signer makeSigner(SignatureAndHashAlgorithm algorithm, boolean raw, boolean forSigning,
        CipherParameters cp)
    {
        if ((algorithm != null) != TlsUtils.isTLSv12(context))
        {
            throw new IllegalStateException();
        }

        if (algorithm != null
            && (algorithm.getHash() != HashAlgorithm.sha1 || algorithm.getSignature() != getSignatureAlgorithm()))
        {
            throw new IllegalStateException();
        }

        Digest d = raw ? new NullDigest() : TlsUtils.createHash(HashAlgorithm.sha1);

        Signer s = new DSADigestSigner(createDSAImpl(), d);
        s.init(forSigning, cp);
        return s;
    }

    protected abstract short getSignatureAlgorithm();

    protected abstract DSA createDSAImpl();
}
