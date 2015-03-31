package org.spongycastle.jcajce.provider.asymmetric.ecgost;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.cryptopro.ECGOST3410NamedCurves;
import org.spongycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X962Parameters;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9ECPoint;
import org.spongycastle.asn1.x9.X9IntegerConverter;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.spongycastle.jce.ECGOST3410NamedCurveTable;
import org.spongycastle.jce.interfaces.ECPointEncoder;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

public class BCECGOST3410PublicKey
    implements ECPublicKey, ECPointEncoder
{
    private String                  algorithm = "ECGOST3410";
    private boolean                 withCompression;

    private transient org.spongycastle.math.ec.ECPoint q;
    private transient ECParameterSpec         ecSpec;
    private transient GOST3410PublicKeyAlgParameters       gostParams;

    public BCECGOST3410PublicKey(
        String              algorithm,
        BCECGOST3410PublicKey      key)
    {
        this.algorithm = algorithm;
        this.q = key.q;
        this.ecSpec = key.ecSpec;
        this.withCompression = key.withCompression;
        this.gostParams = key.gostParams;
    }

    public BCECGOST3410PublicKey(
        ECPublicKeySpec     spec)
    {
        this.q = spec.getQ();

        if (spec.getParams() != null)
        {
            this.ecSpec = spec.getParams();
        }
        else
        {
            if (q.getCurve() == null)
            {
                org.spongycastle.jce.spec.ECParameterSpec s = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();

                q = s.getCurve().createPoint(q.getX().toBigInteger(), q.getY().toBigInteger(), false);
            }
            this.ecSpec = null;
        }
    }

    public BCECGOST3410PublicKey(
        String                  algorithm,
        ECPublicKeyParameters   params,
        ECParameterSpec         spec)
    {
        ECDomainParameters      dp = params.getParameters();

        this.algorithm = algorithm;
        this.q = params.getQ();

        if (spec == null)
        {
            this.ecSpec = new ECParameterSpec(
                            dp.getCurve(),
                            dp.getG(),
                            dp.getN(),
                            dp.getH(),
                            dp.getSeed());
        }
        else
        {
            this.ecSpec = spec;
        }
    }

    public BCECGOST3410PublicKey(
        String                  algorithm,
        ECPublicKeyParameters   params)
    {
        this.algorithm = algorithm;
        this.q = params.getQ();
        this.ecSpec = null;
    }

    BCECGOST3410PublicKey(
        ECPublicKey     key)
    {
        this.q = key.getQ();
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParameters();
    }

    BCECGOST3410PublicKey(
        String            algorithm,
        ECPoint           q,
        ECParameterSpec   ecSpec)
    {
        this.algorithm = algorithm;
        this.q = q;
        this.ecSpec = ecSpec;
    }

    BCECGOST3410PublicKey(
        SubjectPublicKeyInfo    info)
    {
        populateFromPubKeyInfo(info);
    }

    private void populateFromPubKeyInfo(SubjectPublicKeyInfo info)
    {
        if (info.getAlgorithmId().getObjectId().equals(CryptoProObjectIdentifiers.gostR3410_2001))
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

            byte[]          keyEnc = key.getOctets();
            byte[]          x = new byte[32];
            byte[]          y = new byte[32];

            for (int i = 0; i != x.length; i++)
            {
                x[i] = keyEnc[32 - 1 - i];
            }

            for (int i = 0; i != y.length; i++)
            {
                y[i] = keyEnc[64 - 1 - i];
            }

            gostParams = new GOST3410PublicKeyAlgParameters((ASN1Sequence)info.getAlgorithmId().getParameters());

            ECNamedCurveParameterSpec spec = ECGOST3410NamedCurveTable.getParameterSpec(ECGOST3410NamedCurves.getName(gostParams.getPublicKeyParamSet()));

            ecSpec = spec;

            this.q = spec.getCurve().createPoint(new BigInteger(1, x), new BigInteger(1, y), false);
        }
        else
        {
            X962Parameters          params = X962Parameters.getInstance(info.getAlgorithmId().getParameters());
            ECCurve                 curve;

            if (params.isNamedCurve())
            {
                ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(params.getParameters());
                X9ECParameters      ecP = ECUtil.getNamedCurveByOid(oid);

                ecSpec = new ECNamedCurveParameterSpec(
                                            ECUtil.getCurveName(oid),
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
                curve = ((ECParameterSpec)ecSpec).getCurve();
            }
            else if (params.isImplicitlyCA())
            {
                ecSpec = null;
                curve = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa().getCurve();
            }
            else
            {
                X9ECParameters ecP = X9ECParameters.getInstance(params.getParameters());
                ecSpec = new ECParameterSpec(
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
                curve = ((ECParameterSpec)ecSpec).getCurve();
            }

            DERBitString    bits = info.getPublicKeyData();
            byte[]          data = bits.getBytes();
            ASN1OctetString key = new DEROctetString(data);

            //
            // extra octet string - one of our old certs...
            //
            if (data[0] == 0x04 && data[1] == data.length - 2
                && (data[2] == 0x02 || data[2] == 0x03))
            {
                int qLength = new X9IntegerConverter().getByteLength(curve);

                if (qLength >= data.length - 3)
                {
                    try
                    {
                        key = (ASN1OctetString)ASN1Primitive.fromByteArray(data);
                    }
                    catch (IOException ex)
                    {
                        throw new IllegalArgumentException("error recovering public key");
                    }
                }
            }

            X9ECPoint derQ = new X9ECPoint(curve, key);

            this.q = derQ.getPoint();
        }
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
        SubjectPublicKeyInfo info;
        
        if (algorithm.equals("ECGOST3410"))
        {
            ASN1Encodable          params = null;
            if (gostParams != null)
            {
                params = gostParams;
            }
            else if (ecSpec instanceof ECNamedCurveParameterSpec)
            {
                params = new GOST3410PublicKeyAlgParameters(
                                   ECGOST3410NamedCurves.getOID(((ECNamedCurveParameterSpec)ecSpec).getName()),
                                   CryptoProObjectIdentifiers.gostR3411_94_CryptoProParamSet);
            }
            else
            {
                ECParameterSpec         p = (ECParameterSpec)ecSpec;

                ECCurve curve = p.getG().getCurve();
                ECPoint generator = curve.createPoint(p.getG().getX().toBigInteger(), p.getG().getY().toBigInteger(), withCompression);

                X9ECParameters ecP = new X9ECParameters(
                    p.getCurve(), generator, p.getN(), p.getH(), p.getSeed());

                params = new X962Parameters(ecP);
            }

            ECPoint qq = this.getQ();
            ECPoint point = qq.getCurve().createPoint(qq.getX().toBigInteger(), qq.getY().toBigInteger(), false);
            ASN1OctetString p = ASN1OctetString.getInstance(new X9ECPoint(point));

            BigInteger      bX = this.q.getX().toBigInteger();
            BigInteger      bY = this.q.getY().toBigInteger();
            byte[]          encKey = new byte[64];
            
            byte[] val = bX.toByteArray();
            
            for (int i = 0; i != 32; i++)
            {
                encKey[i] = val[val.length - 1 - i];
            }
            
            val = bY.toByteArray();
            
            for (int i = 0; i != 32; i++)
            {
                encKey[32 + i] = val[val.length - 1 - i];
            }

            try
            {
                info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_2001, params), new DEROctetString(encKey));
            }
            catch (IOException e)
            {
                return null;
            }
        }
        else
        {
            X962Parameters          params = null;
            if (ecSpec instanceof ECNamedCurveParameterSpec)
            {
                DERObjectIdentifier curveOid = ECUtil.getNamedCurveOid(((ECNamedCurveParameterSpec)ecSpec).getName());

                if (curveOid == null)
                {
                    curveOid = new DERObjectIdentifier(((ECNamedCurveParameterSpec)ecSpec).getName());
                }
                params = new X962Parameters(curveOid);
            }
            else if (ecSpec == null)
            {
                params = new X962Parameters(DERNull.INSTANCE);
            }
            else
            {
                ECParameterSpec         p = (ECParameterSpec)ecSpec;

                ECCurve curve = p.getG().getCurve();
                ECPoint generator = curve.createPoint(p.getG().getX().toBigInteger(), p.getG().getY().toBigInteger(), withCompression);

                X9ECParameters ecP = new X9ECParameters(
                    p.getCurve(), generator, p.getN(), p.getH(), p.getSeed());

                params = new X962Parameters(ecP);
            }

            ECCurve curve = this.engineGetQ().getCurve();
            ECPoint point = curve.createPoint(this.getQ().getX().toBigInteger(), this.getQ().getY().toBigInteger(), withCompression);
            ASN1OctetString p = ASN1OctetString.getInstance(new X9ECPoint(point));

            info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), p.getOctets());
        }
        
        return KeyUtil.getEncodedSubjectPublicKeyInfo(info);
    }

    public ECParameterSpec getParams()
    {
        return (ECParameterSpec)ecSpec;
    }

    public ECParameterSpec getParameters()
    {
        return (ECParameterSpec)ecSpec;
    }
    
    public org.spongycastle.math.ec.ECPoint getQ()
    {
        if (ecSpec == null)
        {
            if (q instanceof org.spongycastle.math.ec.ECPoint.Fp)
            {
                return new org.spongycastle.math.ec.ECPoint.Fp(null, q.getX(), q.getY());
            }
            else
            {
                return new org.spongycastle.math.ec.ECPoint.F2m(null, q.getX(), q.getY());
            }
        }

        return q;
    }

    public org.spongycastle.math.ec.ECPoint engineGetQ()
    {
        return q;
    }

    public String toString()
    {
        StringBuffer    buf = new StringBuffer();
        String          nl = System.getProperty("line.separator");

        buf.append("EC Public Key").append(nl);
        buf.append("            X: ").append(this.getQ().getX().toBigInteger().toString(16)).append(nl);
        buf.append("            Y: ").append(this.getQ().getY().toBigInteger().toString(16)).append(nl);

        return buf.toString();

    }

    public void setPointFormat(String style)
    {
       withCompression = !("UNCOMPRESSED".equalsIgnoreCase(style));
    }

    ECParameterSpec engineGetSpec()
    {
        if (ecSpec != null)
        {
            return (ECParameterSpec)ecSpec;
        }

        return BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof BCECGOST3410PublicKey))
        {
            return false;
        }

        BCECGOST3410PublicKey other = (BCECGOST3410PublicKey)o;

        return getQ().equals(other.getQ()) && (engineGetSpec().equals(other.engineGetSpec()));
    }

    public int hashCode()
    {
        return getQ().hashCode() ^ engineGetSpec().hashCode();
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
}
