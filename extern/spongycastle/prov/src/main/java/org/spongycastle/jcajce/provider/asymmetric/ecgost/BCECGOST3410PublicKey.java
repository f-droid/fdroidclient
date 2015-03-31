package org.spongycastle.jcajce.provider.asymmetric.ecgost;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.cryptopro.ECGOST3410NamedCurves;
import org.spongycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X962Parameters;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.spongycastle.jce.ECGOST3410NamedCurveTable;
import org.spongycastle.jce.interfaces.ECPointEncoder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveSpec;
import org.spongycastle.math.ec.ECCurve;

public class BCECGOST3410PublicKey
    implements ECPublicKey, org.spongycastle.jce.interfaces.ECPublicKey, ECPointEncoder
{
    static final long serialVersionUID = 7026240464295649314L;

    private String algorithm = "ECGOST3410";
    private boolean withCompression;

    private transient org.spongycastle.math.ec.ECPoint q;
    private transient ECParameterSpec ecSpec;
    private transient GOST3410PublicKeyAlgParameters gostParams;

    public BCECGOST3410PublicKey(
        BCECGOST3410PublicKey key)
    {
        this.q = key.q;
        this.ecSpec = key.ecSpec;
        this.withCompression = key.withCompression;
        this.gostParams = key.gostParams;
    }

    public BCECGOST3410PublicKey(
        ECPublicKeySpec spec)
    {
        this.ecSpec = spec.getParams();
        this.q = EC5Util.convertPoint(ecSpec, spec.getW(), false);
    }

    public BCECGOST3410PublicKey(
        org.spongycastle.jce.spec.ECPublicKeySpec spec)
    {
        this.q = spec.getQ();

        if (spec.getParams() != null) // can be null if implictlyCa
        {
            ECCurve curve = spec.getParams().getCurve();
            EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, spec.getParams().getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec.getParams());
        }
        else
        {
            if (q.getCurve() == null)
            {
                org.spongycastle.jce.spec.ECParameterSpec s = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();

                q = s.getCurve().createPoint(q.getAffineXCoord().toBigInteger(), q.getAffineYCoord().toBigInteger());
            }
            this.ecSpec = null;
        }
    }

    public BCECGOST3410PublicKey(
        String algorithm,
        ECPublicKeyParameters params,
        ECParameterSpec spec)
    {
        ECDomainParameters dp = params.getParameters();

        this.algorithm = algorithm;
        this.q = params.getQ();

        if (spec == null)
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = createSpec(ellipticCurve, dp);
        }
        else
        {
            this.ecSpec = spec;
        }
    }

    public BCECGOST3410PublicKey(
        String algorithm,
        ECPublicKeyParameters params,
        org.spongycastle.jce.spec.ECParameterSpec spec)
    {
        ECDomainParameters dp = params.getParameters();

        this.algorithm = algorithm;
        this.q = params.getQ();

        if (spec == null)
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = createSpec(ellipticCurve, dp);
        }
        else
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(spec.getCurve(), spec.getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec);
        }
    }

    /*
     * called for implicitCA
     */
    public BCECGOST3410PublicKey(
        String algorithm,
        ECPublicKeyParameters params)
    {
        this.algorithm = algorithm;
        this.q = params.getQ();
        this.ecSpec = null;
    }

    private ECParameterSpec createSpec(EllipticCurve ellipticCurve, ECDomainParameters dp)
    {
        return new ECParameterSpec(
            ellipticCurve,
            new ECPoint(
                dp.getG().getAffineXCoord().toBigInteger(),
                dp.getG().getAffineYCoord().toBigInteger()),
            dp.getN(),
            dp.getH().intValue());
    }

    public BCECGOST3410PublicKey(
        ECPublicKey key)
    {
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParams();
        this.q = EC5Util.convertPoint(this.ecSpec, key.getW(), false);
    }

    BCECGOST3410PublicKey(
        SubjectPublicKeyInfo info)
    {
        populateFromPubKeyInfo(info);
    }

    private void populateFromPubKeyInfo(SubjectPublicKeyInfo info)
    {
        DERBitString bits = info.getPublicKeyData();
        ASN1OctetString key;
        this.algorithm = "ECGOST3410";

        try
        {
            key = (ASN1OctetString)ASN1Primitive.fromByteArray(bits.getBytes());
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("error recovering public key");
        }

        byte[] keyEnc = key.getOctets();
        byte[] x = new byte[32];
        byte[] y = new byte[32];

        for (int i = 0; i != x.length; i++)
        {
            x[i] = keyEnc[32 - 1 - i];
        }

        for (int i = 0; i != y.length; i++)
        {
            y[i] = keyEnc[64 - 1 - i];
        }

        gostParams = GOST3410PublicKeyAlgParameters.getInstance(info.getAlgorithm().getParameters());

        ECNamedCurveParameterSpec spec = ECGOST3410NamedCurveTable.getParameterSpec(ECGOST3410NamedCurves.getName(gostParams.getPublicKeyParamSet()));

        ECCurve curve = spec.getCurve();
        EllipticCurve ellipticCurve = EC5Util.convertCurve(curve, spec.getSeed());

        this.q = curve.createPoint(new BigInteger(1, x), new BigInteger(1, y));

        ecSpec = new ECNamedCurveSpec(
            ECGOST3410NamedCurves.getName(gostParams.getPublicKeyParamSet()),
            ellipticCurve,
            new ECPoint(
                spec.getG().getAffineXCoord().toBigInteger(),
                spec.getG().getAffineYCoord().toBigInteger()),
            spec.getN(), spec.getH());
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public String getFormat()
    {
        return "X.509";
    }

    public byte[] getEncoded()
    {
        ASN1Encodable params;
        SubjectPublicKeyInfo info;

        if (gostParams != null)
        {
            params = gostParams;
        }
        else
        {
            if (ecSpec instanceof ECNamedCurveSpec)
            {
                params = new GOST3410PublicKeyAlgParameters(
                    ECGOST3410NamedCurves.getOID(((ECNamedCurveSpec)ecSpec).getName()),
                    CryptoProObjectIdentifiers.gostR3411_94_CryptoProParamSet);
            }
            else
            {   // strictly speaking this may not be applicable...
                ECCurve curve = EC5Util.convertCurve(ecSpec.getCurve());

                X9ECParameters ecP = new X9ECParameters(
                    curve,
                    EC5Util.convertPoint(curve, ecSpec.getGenerator(), withCompression),
                    ecSpec.getOrder(),
                    BigInteger.valueOf(ecSpec.getCofactor()),
                    ecSpec.getCurve().getSeed());

                params = new X962Parameters(ecP);
            }
        }

        BigInteger bX = this.q.getAffineXCoord().toBigInteger();
        BigInteger bY = this.q.getAffineYCoord().toBigInteger();
        byte[] encKey = new byte[64];

        extractBytes(encKey, 0, bX);
        extractBytes(encKey, 32, bY);

        try
        {
            info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_2001, params), new DEROctetString(encKey));
        }
        catch (IOException e)
        {
            return null;
        }

        return KeyUtil.getEncodedSubjectPublicKeyInfo(info);
    }

    private void extractBytes(byte[] encKey, int offSet, BigInteger bI)
    {
        byte[] val = bI.toByteArray();
        if (val.length < 32)
        {
            byte[] tmp = new byte[32];
            System.arraycopy(val, 0, tmp, tmp.length - val.length, val.length);
            val = tmp;
        }

        for (int i = 0; i != 32; i++)
        {
            encKey[offSet + i] = val[val.length - 1 - i];
        }
    }

    public ECParameterSpec getParams()
    {
        return ecSpec;
    }

    public org.spongycastle.jce.spec.ECParameterSpec getParameters()
    {
        if (ecSpec == null)     // implictlyCA
        {
            return null;
        }

        return EC5Util.convertSpec(ecSpec, withCompression);
    }

    public ECPoint getW()
    {
        return new ECPoint(q.getAffineXCoord().toBigInteger(), q.getAffineYCoord().toBigInteger());
    }

    public org.spongycastle.math.ec.ECPoint getQ()
    {
        if (ecSpec == null)
        {
            if (q instanceof org.spongycastle.math.ec.ECPoint.Fp)
            {
                return new org.spongycastle.math.ec.ECPoint.Fp(null, q.getAffineXCoord(), q.getAffineYCoord());
            }
            else
            {
                return new org.spongycastle.math.ec.ECPoint.F2m(null, q.getAffineXCoord(), q.getAffineYCoord());
            }
        }

        return q;
    }

    public org.spongycastle.math.ec.ECPoint engineGetQ()
    {
        return q;
    }

    org.spongycastle.jce.spec.ECParameterSpec engineGetSpec()
    {
        if (ecSpec != null)
        {
            return EC5Util.convertSpec(ecSpec, withCompression);
        }

        return BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        String nl = System.getProperty("line.separator");

        buf.append("EC Public Key").append(nl);
        buf.append("            X: ").append(this.q.getAffineXCoord().toBigInteger().toString(16)).append(nl);
        buf.append("            Y: ").append(this.q.getAffineYCoord().toBigInteger().toString(16)).append(nl);

        return buf.toString();
    }

    public void setPointFormat(String style)
    {
        withCompression = !("UNCOMPRESSED".equalsIgnoreCase(style));
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof BCECGOST3410PublicKey))
        {
            return false;
        }

        BCECGOST3410PublicKey other = (BCECGOST3410PublicKey)o;

        return engineGetQ().equals(other.engineGetQ()) && (engineGetSpec().equals(other.engineGetSpec()));
    }

    public int hashCode()
    {
        return engineGetQ().hashCode() ^ engineGetSpec().hashCode();
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        byte[] enc = (byte[])in.readObject();

        populateFromPubKeyInfo(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(enc)));
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        out.writeObject(this.getEncoded());
    }

    public GOST3410PublicKeyAlgParameters getGostParams()
    {
        return gostParams;
    }
}
