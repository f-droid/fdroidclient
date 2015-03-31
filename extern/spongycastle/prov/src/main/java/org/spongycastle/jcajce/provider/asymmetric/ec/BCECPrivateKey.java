package org.spongycastle.jcajce.provider.asymmetric.ec;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Enumeration;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X962Parameters;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.spongycastle.jcajce.provider.config.ProviderConfiguration;
import org.spongycastle.jce.interfaces.ECPointEncoder;
import org.spongycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveSpec;
import org.spongycastle.math.ec.ECCurve;

public class BCECPrivateKey
    implements ECPrivateKey, org.spongycastle.jce.interfaces.ECPrivateKey, PKCS12BagAttributeCarrier, ECPointEncoder
{
    static final long serialVersionUID = 994553197664784084L;

    private String          algorithm = "EC";
    private boolean         withCompression;

    private transient BigInteger              d;
    private transient ECParameterSpec         ecSpec;
    private transient ProviderConfiguration   configuration;
    private transient DERBitString            publicKey;

    private transient PKCS12BagAttributeCarrierImpl attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected BCECPrivateKey()
    {
    }

    public BCECPrivateKey(
        ECPrivateKey key,
        ProviderConfiguration configuration)
    {
        this.d = key.getS();
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParams();
        this.configuration = configuration;
    }

    public BCECPrivateKey(
        String algorithm,
        org.spongycastle.jce.spec.ECPrivateKeySpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = spec.getD();

        if (spec.getParams() != null) // can be null if implicitlyCA
        {
            ECCurve curve = spec.getParams().getCurve();
            EllipticCurve ellipticCurve;

            ellipticCurve = EC5Util.convertCurve(curve, spec.getParams().getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec.getParams());
        }
        else
        {
            this.ecSpec = null;
        }

        this.configuration = configuration;
    }


    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeySpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = spec.getS();
        this.ecSpec = spec.getParams();
        this.configuration = configuration;
    }

    public BCECPrivateKey(
        String algorithm,
        BCECPrivateKey key)
    {
        this.algorithm = algorithm;
        this.d = key.d;
        this.ecSpec = key.ecSpec;
        this.withCompression = key.withCompression;
        this.attrCarrier = key.attrCarrier;
        this.publicKey = key.publicKey;
        this.configuration = key.configuration;
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        BCECPublicKey pubKey,
        ECParameterSpec spec,
        ProviderConfiguration configuration)
    {
        ECDomainParameters      dp = params.getParameters();

        this.algorithm = algorithm;
        this.d = params.getD();
        this.configuration = configuration;

        if (spec == null)
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = new ECParameterSpec(
                            ellipticCurve,
                            new ECPoint(
                                    dp.getG().getAffineXCoord().toBigInteger(),
                                    dp.getG().getAffineYCoord().toBigInteger()),
                            dp.getN(),
                            dp.getH().intValue());
        }
        else
        {
            this.ecSpec = spec;
        }

        publicKey = getPublicKeyDetails(pubKey);
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        BCECPublicKey pubKey,
        org.spongycastle.jce.spec.ECParameterSpec spec,
        ProviderConfiguration configuration)
    {
        ECDomainParameters      dp = params.getParameters();

        this.algorithm = algorithm;
        this.d = params.getD();
        this.configuration = configuration;

        if (spec == null)
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = new ECParameterSpec(
                            ellipticCurve,
                            new ECPoint(
                                    dp.getG().getAffineXCoord().toBigInteger(),
                                    dp.getG().getAffineYCoord().toBigInteger()),
                            dp.getN(),
                            dp.getH().intValue());
        }
        else
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(spec.getCurve(), spec.getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec);
        }

        publicKey = getPublicKeyDetails(pubKey);
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = params.getD();
        this.ecSpec = null;
        this.configuration = configuration;
    }

    BCECPrivateKey(
        String         algorithm,
        PrivateKeyInfo info,
        ProviderConfiguration configuration)
        throws IOException
    {
        this.algorithm = algorithm;
        this.configuration = configuration;
        populateFromPrivKeyInfo(info);
    }

    private void populateFromPrivKeyInfo(PrivateKeyInfo info)
        throws IOException
    {
        X962Parameters params = X962Parameters.getInstance(info.getPrivateKeyAlgorithm().getParameters());

        if (params.isNamedCurve())
        {
            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(params.getParameters());
            X9ECParameters ecP = ECUtil.getNamedCurveByOid(oid);
            EllipticCurve ellipticCurve = EC5Util.convertCurve(ecP.getCurve(), ecP.getSeed());

            ecSpec = new ECNamedCurveSpec(
                    ECUtil.getCurveName(oid),
                    ellipticCurve,
                    new ECPoint(
                            ecP.getG().getAffineXCoord().toBigInteger(),
                            ecP.getG().getAffineYCoord().toBigInteger()),
                    ecP.getN(),
                    ecP.getH());
        }
        else if (params.isImplicitlyCA())
        {
            ecSpec = null;
        }
        else
        {
            X9ECParameters      ecP = X9ECParameters.getInstance(params.getParameters());
            EllipticCurve       ellipticCurve = EC5Util.convertCurve(ecP.getCurve(), ecP.getSeed());

            this.ecSpec = new ECParameterSpec(
                ellipticCurve,
                new ECPoint(
                        ecP.getG().getAffineXCoord().toBigInteger(),
                        ecP.getG().getAffineYCoord().toBigInteger()),
                ecP.getN(),
                ecP.getH().intValue());
        }

        ASN1Encodable privKey = info.parsePrivateKey();
        if (privKey instanceof ASN1Integer)
        {
            ASN1Integer          derD = ASN1Integer.getInstance(privKey);

            this.d = derD.getValue();
        }
        else
        {
            org.spongycastle.asn1.sec.ECPrivateKey ec = org.spongycastle.asn1.sec.ECPrivateKey.getInstance(privKey);

            this.d = ec.getKey();
            this.publicKey = ec.getPublicKey();
        }
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        X962Parameters          params;

        if (ecSpec instanceof ECNamedCurveSpec)
        {
            ASN1ObjectIdentifier curveOid = ECUtil.getNamedCurveOid(((ECNamedCurveSpec)ecSpec).getName());
            if (curveOid == null)  // guess it's the OID
            {
                curveOid = new ASN1ObjectIdentifier(((ECNamedCurveSpec)ecSpec).getName());
            }

            params = new X962Parameters(curveOid);
        }
        else if (ecSpec == null)
        {
            params = new X962Parameters(DERNull.INSTANCE);
        }
        else
        {
            ECCurve curve = EC5Util.convertCurve(ecSpec.getCurve());

            X9ECParameters ecP = new X9ECParameters(
                curve,
                EC5Util.convertPoint(curve, ecSpec.getGenerator(), withCompression),
                ecSpec.getOrder(),
                BigInteger.valueOf(ecSpec.getCofactor()),
                ecSpec.getCurve().getSeed());

            params = new X962Parameters(ecP);
        }
        
        PrivateKeyInfo          info;
        org.spongycastle.asn1.sec.ECPrivateKey            keyStructure;

        if (publicKey != null)
        {
            keyStructure = new org.spongycastle.asn1.sec.ECPrivateKey(this.getS(), publicKey, params);
        }
        else
        {
            keyStructure = new org.spongycastle.asn1.sec.ECPrivateKey(this.getS(), params);
        }

        try
        {
            info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), keyStructure);

            return info.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public ECParameterSpec getParams()
    {
        return ecSpec;
    }

    public org.spongycastle.jce.spec.ECParameterSpec getParameters()
    {
        if (ecSpec == null)
        {
            return null;
        }
        
        return EC5Util.convertSpec(ecSpec, withCompression);
    }

    org.spongycastle.jce.spec.ECParameterSpec engineGetSpec()
    {
        if (ecSpec != null)
        {
            return EC5Util.convertSpec(ecSpec, withCompression);
        }

        return configuration.getEcImplicitlyCa();
    }

    public BigInteger getS()
    {
        return d;
    }

    public BigInteger getD()
    {
        return d;
    }
    
    public void setBagAttribute(
        ASN1ObjectIdentifier oid,
        ASN1Encodable        attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public ASN1Encodable getBagAttribute(
        ASN1ObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }

    public void setPointFormat(String style)
    {
       withCompression = !("UNCOMPRESSED".equalsIgnoreCase(style));
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof BCECPrivateKey))
        {
            return false;
        }

        BCECPrivateKey other = (BCECPrivateKey)o;

        return getD().equals(other.getD()) && (engineGetSpec().equals(other.engineGetSpec()));
    }

    public int hashCode()
    {
        return getD().hashCode() ^ engineGetSpec().hashCode();
    }

    public String toString()
    {
        StringBuffer    buf = new StringBuffer();
        String          nl = System.getProperty("line.separator");

        buf.append("EC Private Key").append(nl);
        buf.append("             S: ").append(this.d.toString(16)).append(nl);

        return buf.toString();

    }

    private DERBitString getPublicKeyDetails(BCECPublicKey pub)
    {
        try
        {
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(pub.getEncoded()));

            return info.getPublicKeyData();
        }
        catch (IOException e)
        {   // should never happen
            return null;
        }
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        byte[] enc = (byte[])in.readObject();

        populateFromPrivKeyInfo(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(enc)));

        this.configuration = BouncyCastleProvider.CONFIGURATION;
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        out.writeObject(this.getEncoded());
    }
}
