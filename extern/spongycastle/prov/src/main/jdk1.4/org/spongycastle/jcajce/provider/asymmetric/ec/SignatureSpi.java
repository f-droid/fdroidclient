package org.spongycastle.jcajce.provider.asymmetric.ec;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DSA;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.NullDigest;
import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA224Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.crypto.signers.ECNRSigner;
import org.spongycastle.jcajce.provider.asymmetric.util.DSABase;
import org.spongycastle.jcajce.provider.asymmetric.util.DSAEncoder;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jce.interfaces.ECKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class SignatureSpi
    extends DSABase
{
    SignatureSpi(Digest digest, DSA signer, DSAEncoder encoder)
    {
        super("ECDSA", digest, signer, encoder);
    }

    protected void engineInitVerify(PublicKey publicKey)
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
                    throw new InvalidKeyException("can't recognise key type in ECDSA based signer");
                }
            }
            catch (Exception e)
            {
                throw new InvalidKeyException("can't recognise key type in ECDSA based signer");
            }
        }

        digest.reset();

        signer.init(false, param);
    }

    protected void doEngineInitSign(
        PrivateKey privateKey,
        SecureRandom random)
        throws InvalidKeyException
    {
        CipherParameters param;

        if (privateKey instanceof ECKey)
        {
            param = ECUtil.generatePrivateKeyParameter(privateKey);
        }
        else
        {
            throw new InvalidKeyException("can't recognise key type in ECDSA based signer");
        }

        digest.reset();

        if (random != null)
        {
            signer.init(true, new ParametersWithRandom(param, random));
        }
        else
        {
            signer.init(true, param);
        }
    }

    static public class ecDSA
        extends SignatureSpi
    {
        public ecDSA()
        {
            super(new SHA1Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSAnone
        extends SignatureSpi
    {
        public ecDSAnone()
        {
            super(new NullDigest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSA224
        extends SignatureSpi
    {
        public ecDSA224()
        {
            super(new SHA224Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSA256
        extends SignatureSpi
    {
        public ecDSA256()
        {
            super(new SHA256Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSA384
        extends SignatureSpi
    {
        public ecDSA384()
        {
            super(new SHA384Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSA512
        extends SignatureSpi
    {
        public ecDSA512()
        {
            super(new SHA512Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecDSARipeMD160
        extends SignatureSpi
    {
        public ecDSARipeMD160()
        {
            super(new RIPEMD160Digest(), new ECDSASigner(), new StdDSAEncoder());
        }
    }

    static public class ecNR
        extends SignatureSpi
    {
        public ecNR()
        {
            super(new SHA1Digest(), new ECNRSigner(), new StdDSAEncoder());
        }
    }

    static public class ecNR224
        extends SignatureSpi
    {
        public ecNR224()
        {
            super(new SHA224Digest(), new ECNRSigner(), new StdDSAEncoder());
        }
    }

    static public class ecNR256
        extends SignatureSpi
    {
        public ecNR256()
        {
            super(new SHA256Digest(), new ECNRSigner(), new StdDSAEncoder());
        }
    }

    static public class ecNR384
        extends SignatureSpi
    {
        public ecNR384()
        {
            super(new SHA384Digest(), new ECNRSigner(), new StdDSAEncoder());
        }
    }

    static public class ecNR512
        extends SignatureSpi
    {
        public ecNR512()
        {
            super(new SHA512Digest(), new ECNRSigner(), new StdDSAEncoder());
        }
    }

    static public class ecCVCDSA
        extends SignatureSpi
    {
        public ecCVCDSA()
        {
            super(new SHA1Digest(), new ECDSASigner(), new CVCDSAEncoder());
        }
    }

    static public class ecCVCDSA224
        extends SignatureSpi
    {
        public ecCVCDSA224()
        {
            super(new SHA224Digest(), new ECDSASigner(), new CVCDSAEncoder());
        }
    }

    static public class ecCVCDSA256
        extends SignatureSpi
    {
        public ecCVCDSA256()
        {
            super(new SHA256Digest(), new ECDSASigner(), new CVCDSAEncoder());
        }
    }

    static public class ecCVCDSA384
        extends SignatureSpi
    {
        public ecCVCDSA384()
        {
            super(new SHA384Digest(), new ECDSASigner(), new CVCDSAEncoder());
        }
    }

    static public class ecCVCDSA512
        extends SignatureSpi
    {
        public ecCVCDSA512()
        {
            super(new SHA512Digest(), new ECDSASigner(), new CVCDSAEncoder());
        }
    }

    private static class StdDSAEncoder
        implements DSAEncoder
    {
        public byte[] encode(
            BigInteger r,
            BigInteger s)
            throws IOException
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(new DERInteger(r));
            v.add(new DERInteger(s));

            return new DERSequence(v).getEncoded(ASN1Encoding.DER);
        }

        public BigInteger[] decode(
            byte[] encoding)
            throws IOException
        {
            ASN1Sequence s = (ASN1Sequence)ASN1Primitive.fromByteArray(encoding);
            BigInteger[] sig = new BigInteger[2];

            sig[0] = ((DERInteger)s.getObjectAt(0)).getValue();
            sig[1] = ((DERInteger)s.getObjectAt(1)).getValue();

            return sig;
        }
    }

    private static class CVCDSAEncoder
        implements DSAEncoder
    {
        public byte[] encode(
            BigInteger r,
            BigInteger s)
            throws IOException
        {
            byte[] first = makeUnsigned(r);
            byte[] second = makeUnsigned(s);
            byte[] res;

            if (first.length > second.length)
            {
                res = new byte[first.length * 2];
            }
            else
            {
                res = new byte[second.length * 2];
            }

            System.arraycopy(first, 0, res, res.length / 2 - first.length, first.length);
            System.arraycopy(second, 0, res, res.length - second.length, second.length);

            return res;
        }


        private byte[] makeUnsigned(BigInteger val)
        {
            byte[] res = val.toByteArray();

            if (res[0] == 0)
            {
                byte[] tmp = new byte[res.length - 1];

                System.arraycopy(res, 1, tmp, 0, tmp.length);

                return tmp;
            }

            return res;
        }

        public BigInteger[] decode(
            byte[] encoding)
            throws IOException
        {
            BigInteger[] sig = new BigInteger[2];

            byte[] first = new byte[encoding.length / 2];
            byte[] second = new byte[encoding.length / 2];

            System.arraycopy(encoding, 0, first, 0, first.length);
            System.arraycopy(encoding, first.length, second, 0, second.length);

            sig[0] = new BigInteger(1, first);
            sig[1] = new BigInteger(1, second);

            return sig;
        }
    }
}
