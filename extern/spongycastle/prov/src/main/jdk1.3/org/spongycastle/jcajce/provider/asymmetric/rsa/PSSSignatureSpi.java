package org.spongycastle.jcajce.provider.asymmetric.rsa;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.crypto.AsymmetricBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA224Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.engines.RSABlindedEngine;
import org.spongycastle.crypto.params.ParametersWithRandom;

public class PSSSignatureSpi
    extends Signature
{
    private AlgorithmParameters engineParams;
    private AsymmetricBlockCipher signer;
    private Digest contentDigest;
    private Digest mgfDigest;
    private int saltLength;
    private byte trailer;
    private boolean isRaw;
    private ByteArrayOutputStream bOut;
    private org.spongycastle.crypto.signers.PSSSigner pss;
    private CipherParameters sigParams;

    private byte getTrailer(
        int trailerField)
    {
        if (trailerField == 1)
        {
            return org.spongycastle.crypto.signers.PSSSigner.TRAILER_IMPLICIT;
        }
        
        throw new IllegalArgumentException("unknown trailer field");
    }

    private void setupContentDigest()
    {
        if (isRaw)
        {
            this.contentDigest = new NullPssDigest(mgfDigest);
        }
        else
        {
            this.contentDigest = mgfDigest;
        }
    }

    protected PSSSignatureSpi(
        String name,
        AsymmetricBlockCipher signer,
        Digest digest)
    {
        super(name);

        this.signer = signer;
        this.mgfDigest = digest;

        if (digest != null)
        {
            this.saltLength = digest.getDigestSize();
        }
        else
        {
            this.saltLength = 20;
        }

        this.isRaw = false;

        setupContentDigest();
    }

    // care - this constructor is actually used by outside organisations
    protected PSSSignatureSpi(
        String name,
        AsymmetricBlockCipher signer,
        Digest digest,
        boolean isRaw)
    {
        super(name);

        this.signer = signer;
        this.mgfDigest = digest;
        
        if (digest != null)
        {
            this.saltLength = digest.getDigestSize();
        }
        else
        {
            this.saltLength = 20;
        }

        this.isRaw = isRaw;

        setupContentDigest();
    }
    
    protected void engineInitVerify(
        PublicKey publicKey)
        throws InvalidKeyException
    {
        if (!(publicKey instanceof RSAPublicKey))
        {
            throw new InvalidKeyException("Supplied key is not a RSAPublicKey instance");
        }

        sigParams = RSAUtil.generatePublicKeyParameter((RSAPublicKey)publicKey);

        if (isRaw)
        {
            bOut = new ByteArrayOutputStream();
        }
        else
        {
            pss = new org.spongycastle.crypto.signers.PSSSigner(signer, contentDigest, mgfDigest, saltLength);
            pss.init(false,
                sigParams);
        }
    }

    protected void engineInitSign(
        PrivateKey privateKey,
        SecureRandom random)
        throws InvalidKeyException
    {
        if (!(privateKey instanceof RSAPrivateKey))
        {
            throw new InvalidKeyException("Supplied key is not a RSAPrivateKey instance");
        }

        sigParams = new ParametersWithRandom(RSAUtil.generatePrivateKeyParameter((RSAPrivateKey)privateKey), random);

        if (isRaw)
        {
            bOut = new ByteArrayOutputStream();
        }
        else
        {
            pss = new org.spongycastle.crypto.signers.PSSSigner(signer, contentDigest, mgfDigest, saltLength);
            pss.init(true, sigParams);
        }
    }

    protected void engineInitSign(
        PrivateKey privateKey)
        throws InvalidKeyException
    {
        if (!(privateKey instanceof RSAPrivateKey))
        {
            throw new InvalidKeyException("Supplied key is not a RSAPrivateKey instance");
        }

        sigParams = RSAUtil.generatePrivateKeyParameter((RSAPrivateKey)privateKey);

        if (isRaw)
        {
            bOut = new ByteArrayOutputStream();
        }
        else
        {
            pss = new org.spongycastle.crypto.signers.PSSSigner(signer, contentDigest, mgfDigest, saltLength);
            pss.init(true, sigParams);
        }
    }

    protected void engineUpdate(
        byte    b)
        throws SignatureException
    {
        if (isRaw)
        {
            bOut.write(b);
        }
        else
        {
            pss.update(b);
        }
    }

    protected void engineUpdate(
        byte[]  b,
        int     off,
        int     len) 
        throws SignatureException
    {
        if (isRaw)
        {
            bOut.write(b, off, len);
        }
        else
        {
            pss.update(b, off, len);
        }
    }

    protected byte[] engineSign()
        throws SignatureException
    {
        try
        {
            if (isRaw)
            {
                byte[] hash = bOut.toByteArray();
                contentDigest = mgfDigest = guessDigest(hash.length);
                saltLength = contentDigest.getDigestSize();
                pss = new org.spongycastle.crypto.signers.PSSSigner(signer, new NullPssDigest(contentDigest), mgfDigest, saltLength);

                pss.init(true, sigParams);
            }
            return pss.generateSignature();
        }
        catch (CryptoException e)
        {
            throw new SignatureException(e.getMessage());
        }
    }

    protected boolean engineVerify(
        byte[]  sigBytes) 
        throws SignatureException
    {
        if (isRaw)
        {
            byte[] hash = bOut.toByteArray();
            contentDigest = mgfDigest = guessDigest(hash.length);
            saltLength = contentDigest.getDigestSize();
            pss = new org.spongycastle.crypto.signers.PSSSigner(signer, new NullPssDigest(contentDigest), mgfDigest, saltLength);

            pss.init(false, sigParams);

            pss.update(hash, 0, hash.length);
        }
        return pss.verifySignature(sigBytes);
    }

    protected void engineSetParameter(
        AlgorithmParameterSpec params)
        throws InvalidParameterException
    {
            throw new InvalidParameterException("Only PSSParameterSpec supported");
    }

    protected AlgorithmParameters engineGetParameters()
    {
        return engineParams;
    }
    
    /**
     * @deprecated replaced with <a href = "#engineSetParameter(java.security.spec.AlgorithmParameterSpec)">
     */
    protected void engineSetParameter(
        String param,
        Object value)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }
    
    protected Object engineGetParameter(
        String param)
    {
        throw new UnsupportedOperationException("engineGetParameter unsupported");
    }

    private Digest guessDigest(int size)
    {
        switch (size)
        {
        case 20:
            return new SHA1Digest();
        case 28:
            return new SHA224Digest();
        case 32:
            return new SHA256Digest();
        case 48:
            return new SHA384Digest();
        case 64:
            return new SHA512Digest();
        }

        return null;
    }

    static public class nonePSS
        extends PSSSignatureSpi
    {
        public nonePSS()
        {
            super("NONEwithRSAandMGF1", new RSABlindedEngine(), null, true);
        }
    }

    static public class PSSwithRSA
        extends PSSSignatureSpi
    {
        public PSSwithRSA()
        {
            super("SHA1withRSAandMGF1", new RSABlindedEngine(), null);
        }
    }

    static public class SHA1withRSA
        extends PSSSignatureSpi
    {
        public SHA1withRSA()
        {
            super("SHA1withRSAandMGF1", new RSABlindedEngine(), new SHA1Digest());
        }
    }

    static public class SHA224withRSA
        extends PSSSignatureSpi
    {
        public SHA224withRSA()
        {
            super("SHA224withRSAandMGF1", new RSABlindedEngine(), new SHA224Digest());
        }
    }

    static public class SHA256withRSA
        extends PSSSignatureSpi
    {
        public SHA256withRSA()
        {
            super("SHA256withRSAandMGF1", new RSABlindedEngine(), new SHA256Digest());
        }
    }

    static public class SHA384withRSA
        extends PSSSignatureSpi
    {
        public SHA384withRSA()
        {
            super("SHA384withRSAandMGF1", new RSABlindedEngine(), new SHA384Digest());
        }
    }

    static public class SHA512withRSA
        extends PSSSignatureSpi
    {
        public SHA512withRSA()
        {
            super("SHA512withRSAandMGF1", new RSABlindedEngine(), new SHA512Digest());
        }
    }

    private class NullPssDigest
        implements Digest
    {
        private ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        private Digest baseDigest;
        private boolean oddTime = true;

        public NullPssDigest(Digest mgfDigest)
        {
            this.baseDigest = mgfDigest;
        }

        public String getAlgorithmName()
        {
            return "NULL";
        }

        public int getDigestSize()
        {
            return baseDigest.getDigestSize();
        }

        public void update(byte in)
        {
            bOut.write(in);
        }

        public void update(byte[] in, int inOff, int len)
        {
            bOut.write(in, inOff, len);
        }

        public int doFinal(byte[] out, int outOff)
        {
            byte[] res = bOut.toByteArray();

            if (oddTime)
            {
                System.arraycopy(res, 0, out, outOff, res.length);
            }
            else
            {
                baseDigest.update(res, 0, res.length);

                baseDigest.doFinal(out, outOff);
            }

            reset();

            oddTime = !oddTime;

            return res.length;
        }

        public void reset()
        {
            bOut.reset();
            baseDigest.reset();
        }

        public int getByteLength()
        {
            return 0;
        }
    }
}
