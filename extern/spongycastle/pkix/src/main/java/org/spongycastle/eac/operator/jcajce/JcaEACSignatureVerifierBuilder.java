package org.spongycastle.eac.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.eac.EACObjectIdentifiers;
import org.spongycastle.eac.operator.EACSignatureVerifier;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.OperatorStreamException;
import org.spongycastle.operator.RuntimeOperatorException;

public class JcaEACSignatureVerifierBuilder
{
    private EACHelper helper = new DefaultEACHelper();

    public JcaEACSignatureVerifierBuilder setProvider(String providerName)
    {
        this.helper = new NamedEACHelper(providerName);

        return this;
    }

    public JcaEACSignatureVerifierBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderEACHelper(provider);

        return this;
    }

    public EACSignatureVerifier build(final ASN1ObjectIdentifier usageOid, PublicKey pubKey)
        throws OperatorCreationException
    {
        Signature sig;
        try
        {
            sig = helper.getSignature(usageOid);

            sig.initVerify(pubKey);
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

        return new EACSignatureVerifier()
        {
            public ASN1ObjectIdentifier getUsageIdentifier()
            {
                return usageOid;
            }

            public OutputStream getOutputStream()
            {
                return sigStream;
            }

            public boolean verify(byte[] expected)
            {
                try
                {
                    if (usageOid.on(EACObjectIdentifiers.id_TA_ECDSA))
                    {
                        try
                        {
                            byte[] reencoded = derEncode(expected);

                            return sigStream.verify(reencoded);
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                    }
                    else
                    {
                        return sigStream.verify(expected);
                    }
                }
                catch (SignatureException e)
                {
                    throw new RuntimeOperatorException("exception obtaining signature: " + e.getMessage(), e);
                }
            }
        };
    }

    private static byte[] derEncode(byte[] rawSign) throws IOException
    {
        int len = rawSign.length / 2;

        byte[] r = new byte[len];
        byte[] s = new byte[len];
        System.arraycopy(rawSign, 0, r, 0, len);
        System.arraycopy(rawSign, len, s, 0, len);

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERInteger(new BigInteger(1, r)));
        v.add(new DERInteger(new BigInteger(1, s)));

        DERSequence seq = new DERSequence(v);
        return seq.getEncoded();
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

        boolean verify(byte[] expected)
            throws SignatureException
        {
            return sig.verify(expected);
        }
    }
}
