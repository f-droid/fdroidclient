package org.spongycastle.eac.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Hashtable;

import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.eac.EACObjectIdentifiers;
import org.spongycastle.eac.operator.EACSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.OperatorStreamException;
import org.spongycastle.operator.RuntimeOperatorException;

public class JcaEACSignerBuilder
{
    private static final Hashtable sigNames = new Hashtable();

    static
    {
        sigNames.put("SHA1withRSA", EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_1);
        sigNames.put("SHA256withRSA", EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_256);
        sigNames.put("SHA1withRSAandMGF1", EACObjectIdentifiers.id_TA_RSA_PSS_SHA_1);
        sigNames.put("SHA256withRSAandMGF1", EACObjectIdentifiers.id_TA_RSA_PSS_SHA_256);
        sigNames.put("SHA512withRSA", EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_512);
        sigNames.put("SHA512withRSAandMGF1", EACObjectIdentifiers.id_TA_RSA_PSS_SHA_512);

        sigNames.put("SHA1withECDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_1);
        sigNames.put("SHA224withECDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_224);
        sigNames.put("SHA256withECDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_256);
        sigNames.put("SHA384withECDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_384);
        sigNames.put("SHA512withECDSA", EACObjectIdentifiers.id_TA_ECDSA_SHA_512);
    }

    private EACHelper helper = new DefaultEACHelper();

    public JcaEACSignerBuilder setProvider(String providerName)
    {
        this.helper = new NamedEACHelper(providerName);

        return this;
    }

    public JcaEACSignerBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderEACHelper(provider);

        return this;
    }

    public EACSigner build(String algorithm, PrivateKey privKey)
        throws OperatorCreationException
    {
        return build((ASN1ObjectIdentifier)sigNames.get(algorithm), privKey);
    }

    public EACSigner build(final ASN1ObjectIdentifier usageOid, PrivateKey privKey)
        throws OperatorCreationException
    {
        Signature sig;
        try
        {
            sig = helper.getSignature(usageOid);

            sig.initSign(privKey);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new OperatorCreationException("unable to find algorithm: " + e.getMessage(), e);
        }
        catch (NoSuchProviderException e)
        {
            throw new OperatorCreationException("unable to find provider: " + e.getMessage(), e);
        }
        catch (InvalidKeyException e)
        {
            throw new OperatorCreationException("invalid key: " + e.getMessage(), e);
        }

        final SignatureOutputStream sigStream = new SignatureOutputStream(sig);

        return new EACSigner()
        {
            public ASN1ObjectIdentifier getUsageIdentifier()
            {
                return usageOid;
            }

            public OutputStream getOutputStream()
            {
                return sigStream;
            }

            public byte[] getSignature()
            {
                try
                {
                    byte[] signature = sigStream.getSignature();

                    if (usageOid.on(EACObjectIdentifiers.id_TA_ECDSA))
                    {
                        return reencode(signature);
                    }

                    return signature;
                }
                catch (SignatureException e)
                {
                    throw new RuntimeOperatorException("exception obtaining signature: " + e.getMessage(), e);
                }
            }
        };
    }

    public static int max(int el1, int el2)
    {
        return el1 > el2 ? el1 : el2;
    }

    private static byte[] reencode(byte[] rawSign)
    {
        ASN1Sequence sData = ASN1Sequence.getInstance(rawSign);

        BigInteger r = ASN1Integer.getInstance(sData.getObjectAt(0)).getValue();
        BigInteger s = ASN1Integer.getInstance(sData.getObjectAt(1)).getValue();

        byte[] rB = r.toByteArray();
        byte[] sB = s.toByteArray();

        int rLen = unsignedIntLength(rB);
        int sLen = unsignedIntLength(sB);

        byte[] ret;
        int len = max(rLen, sLen);

        ret = new byte[len * 2];
        Arrays.fill(ret, (byte)0);

        copyUnsignedInt(rB, ret, len - rLen);
        copyUnsignedInt(sB, ret, 2 * len - sLen);

        return ret;
    }

    private static int unsignedIntLength(byte[] i)
    {
        int len = i.length;
        if (i[0] == 0)
        {
            len--;
        }

        return len;
    }

    private static void copyUnsignedInt(byte[] src, byte[] dst, int offset)
    {
        int len = src.length;
        int readoffset = 0;
        if (src[0] == 0)
        {
            len--;
            readoffset = 1;
        }

        System.arraycopy(src, readoffset, dst, offset, len);
    }

    private class SignatureOutputStream
        extends OutputStream
    {
        private Signature sig;

        SignatureOutputStream(Signature sig)
        {
            this.sig = sig;
        }

        public void write(byte[] bytes, int off, int len)
            throws IOException
        {
            try
            {
                sig.update(bytes, off, len);
            }
            catch (SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e);
            }
        }

        public void write(byte[] bytes)
            throws IOException
        {
            try
            {
                sig.update(bytes);
            }
            catch (SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e);
            }
        }

        public void write(int b)
            throws IOException
        {
            try
            {
                sig.update((byte)b);
            }
            catch (SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e);
            }
        }

        byte[] getSignature()
            throws SignatureException
        {
            return sig.sign();
        }
    }
}
