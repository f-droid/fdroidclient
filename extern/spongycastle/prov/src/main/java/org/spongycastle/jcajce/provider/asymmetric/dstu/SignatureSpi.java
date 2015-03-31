package org.spongycastle.jcajce.provider.asymmetric.dstu;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DSA;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.GOST3411Digest;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.signers.DSTU4145Signer;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jce.interfaces.ECKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class SignatureSpi
    extends java.security.SignatureSpi
    implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
    private Digest digest;
    private DSA signer;

    private static byte[] DEFAULT_SBOX = {
        0xa, 0x9, 0xd, 0x6, 0xe, 0xb, 0x4, 0x5, 0xf, 0x1, 0x3, 0xc, 0x7, 0x0, 0x8, 0x2,
        0x8, 0x0, 0xc, 0x4, 0x9, 0x6, 0x7, 0xb, 0x2, 0x3, 0x1, 0xf, 0x5, 0xe, 0xa, 0xd,
        0xf, 0x6, 0x5, 0x8, 0xe, 0xb, 0xa, 0x4, 0xc, 0x0, 0x3, 0x7, 0x2, 0x9, 0x1, 0xd,
        0x3, 0x8, 0xd, 0x9, 0x6, 0xb, 0xf, 0x0, 0x2, 0x5, 0xc, 0xa, 0x4, 0xe, 0x1, 0x7,
        0xf, 0x8, 0xe, 0x9, 0x7, 0x2, 0x0, 0xd, 0xc, 0x6, 0x1, 0x5, 0xb, 0x4, 0x3, 0xa,
        0x2, 0x8, 0x9, 0x7, 0x5, 0xf, 0x0, 0xb, 0xc, 0x1, 0xd, 0xe, 0xa, 0x3, 0x6, 0x4,
        0x3, 0x8, 0xb, 0x5, 0x6, 0x4, 0xe, 0xa, 0x2, 0xc, 0x1, 0x7, 0x9, 0xf, 0xd, 0x0,
        0x1, 0x2, 0x3, 0xe, 0x6, 0xd, 0xb, 0x8, 0xf, 0xa, 0xc, 0x5, 0x7, 0x9, 0x0, 0x4
    };

    public SignatureSpi()
    {
        //TODO: Add default ua s-box
        //this.digest = new GOST3411Digest(DEFAULT_SBOX);
        this.signer = new DSTU4145Signer();
    }

    protected void engineInitVerify(
        PublicKey publicKey)
        throws InvalidKeyException
    {
        CipherParameters param;

        if (publicKey instanceof ECPublicKey)
        {
            param = ECUtil.generatePublicKeyParameter(publicKey);
        }
        else
        {
            try
            {
                byte[] bytes = publicKey.getEncoded();

                publicKey = BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(bytes));

                if (publicKey instanceof ECPublicKey)
                {
                    param = ECUtil.generatePublicKeyParameter(publicKey);
                }
                else
                {
                    throw new InvalidKeyException("can't recognise key type in DSA based signer");
                }
            }
            catch (Exception e)
            {
                throw new InvalidKeyException("can't recognise key type in DSA based signer");
            }
        }

        digest = new GOST3411Digest(expandSbox(((BCDSTU4145PublicKey)publicKey).getSbox()));
        signer.init(false, param);
    }

    byte[] expandSbox(byte[] compressed)
    {
        byte[] expanded = new byte[128];

        for (int i = 0; i < compressed.length; i++)
        {
            expanded[i * 2] = (byte)((compressed[i] >> 4) & 0xf);
            expanded[i * 2 + 1] = (byte)(compressed[i] & 0xf);
        }
        return expanded;
    }

    protected void engineInitSign(
        PrivateKey privateKey)
        throws InvalidKeyException
    {
        CipherParameters param = null;

        if (privateKey instanceof ECKey)
        {
            param = ECUtil.generatePrivateKeyParameter(privateKey);
        }

        digest = new GOST3411Digest(DEFAULT_SBOX);

        if (appRandom != null)
        {
            signer.init(true, new ParametersWithRandom(param, appRandom));
        }
        else
        {
            signer.init(true, param);
        }
    }

    protected void engineUpdate(
        byte b)
        throws SignatureException
    {
        digest.update(b);
    }

    protected void engineUpdate(
        byte[] b,
        int off,
        int len)
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
            BigInteger[] sig = signer.generateSignature(hash);
            byte[] r = sig[0].toByteArray();
            byte[] s = sig[1].toByteArray();

            byte[] sigBytes = new byte[(r.length > s.length ? r.length * 2 : s.length * 2)];
            System.arraycopy(s, 0, sigBytes, (sigBytes.length / 2) - s.length, s.length);
            System.arraycopy(r, 0, sigBytes, sigBytes.length - r.length, r.length);

            return new DEROctetString(sigBytes).getEncoded();
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
        byte[] hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        BigInteger[] sig;

        try
        {
            byte[] bytes = ((ASN1OctetString)ASN1OctetString.fromByteArray(sigBytes)).getOctets();

            byte[] r = new byte[bytes.length / 2];
            byte[] s = new byte[bytes.length / 2];

            System.arraycopy(bytes, 0, s, 0, bytes.length / 2);

            System.arraycopy(bytes, bytes.length / 2, r, 0, bytes.length / 2);

            sig = new BigInteger[2];
            sig[0] = new BigInteger(1, r);
            sig[1] = new BigInteger(1, s);
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
        String param,
        Object value)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated
     */
    protected Object engineGetParameter(
        String param)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }
}
