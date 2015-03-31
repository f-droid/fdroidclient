package org.spongycastle.jcajce.provider.asymmetric.ec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.sec.ECPrivateKeyStructure;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X962Parameters;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.spongycastle.jcajce.provider.config.ProviderConfiguration;
import org.spongycastle.jce.interfaces.ECPointEncoder;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

public class BCECPrivateKey
    implements ECPrivateKey, PKCS12BagAttributeCarrier, ECPointEncoder
{
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

    BCECPrivateKey(
        ECPrivateKey    key,
        ProviderConfiguration configuration)
    {
        this.d = key.getD();
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParameters();
        this.configuration = configuration;
    }

    public BCECPrivateKey(
        String              algorithm,
        ECPrivateKeySpec    spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = spec.getD();
        this.ecSpec = spec.getParams();
        this.configuration = configuration;
    }

    public BCECPrivateKey(
        String                  algorithm,
        ECPrivateKeyParameters  params,
        BCECPublicKey          pubKey,
        ECParameterSpec         spec,
        ProviderConfiguration configuration)
    {
        ECDomainParameters      dp = params.getParameters();

        this.algorithm = algorithm;
        this.d = params.getD();
        this.configuration = configuration;

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

        publicKey = getPublicKeyDetails(pubKey);
    }

    public BCECPrivateKey(
        String                  algorithm,
        ECPrivateKeyParameters  params,
        ProviderConfiguration   configuration)
    {
        this.algorithm = algorithm;
        this.d = params.getD();
        this.ecSpec = null;
        this.configuration = configuration;
    }

    public BCECPrivateKey(
        String             algorithm,
        BCECPrivateKey    key)
    {
        this.algorithm = algorithm;
        this.d = key.d;
        this.ecSpec = key.ecSpec;
        this.withCompression = key.withCompression;
        this.publicKey = key.publicKey;
        this.attrCarrier = key.attrCarrier;
        this.configuration = key.configuration;
    }

    BCECPrivateKey(
        PrivateKeyInfo      info,
        ProviderConfiguration configuration)
    {
        this.configuration = configuration;

        populateFromPrivKeyInfo(info);
    }

    BCECPrivateKey(
        String              algorithm,
        PrivateKeyInfo      info,
        ProviderConfiguration configuration)
    {
        this.configuration = configuration;
        populateFromPrivKeyInfo(info);
        this.algorithm = algorithm;
    }

    private void populateFromPrivKeyInfo(PrivateKeyInfo info)
    {
        X962Parameters      params = X962Parameters.getInstance(info.getAlgorithmId().getParameters());

        if (params.isNamedCurve())
        {
            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)params.getParameters();
            X9ECParameters      ecP = ECUtil.getNamedCurveByOid(oid);

            ecSpec = new ECNamedCurveParameterSpec(
                                        ECUtil.getCurveName(oid),
                                        ecP.getCurve(),
                                        ecP.getG(),
                                        ecP.getN(),
                                        ecP.getH(),
                                        ecP.getSeed());
        }
        else if (params.isImplicitlyCA())
        {
            ecSpec = null;
        }
        else
        {
            X9ECParameters          ecP = X9ECParameters.getInstance(params.getParameters());
            ecSpec = new ECParameterSpec(ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
        }

        if (info.getPrivateKey() instanceof ASN1Integer)
        {
            ASN1Integer          derD = ASN1Integer.getInstance(info.getPrivateKey());

            this.d = derD.getValue();
        }
        else
        {
            ECPrivateKeyStructure   ec = new ECPrivateKeyStructure((ASN1Sequence)info.getPrivateKey());

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
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);
        X962Parameters          params = null;

        if (ecSpec instanceof ECNamedCurveParameterSpec)
        {
            ASN1ObjectIdentifier curveOid = ECUtil.getNamedCurveOid(((ECNamedCurveParameterSpec)ecSpec).getName());
            
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
            ECPoint generator;
            
            if (curve instanceof ECCurve.Fp) 
            {
                generator = new ECPoint.Fp(curve, p.getG().getX(), p.getG().getY(), withCompression);
            } 
            else if (curve instanceof ECCurve.F2m) 
            {
                generator = new ECPoint.F2m(curve, p.getG().getX(), p.getG().getY(), withCompression);
            }
            else 
            {
                throw new UnsupportedOperationException("Subclass of ECPoint " + curve.getClass().toString() + "not supported");
            }
            
            X9ECParameters ecP = new X9ECParameters(
                  p.getCurve(),
                  generator,
                  p.getN(),
                  p.getH(),
                  p.getSeed());

            params = new X962Parameters(ecP);
        }

        PrivateKeyInfo        info;
        ECPrivateKeyStructure keyStructure;

        if (publicKey != null)
        {
            keyStructure = new ECPrivateKeyStructure(this.getD(), publicKey, params);
        }
        else
        {
            keyStructure = new ECPrivateKeyStructure(this.getD(), params);
        }

        try
        {
            if (algorithm.equals("ECGOST3410"))
            {
                info = new PrivateKeyInfo(new AlgorithmIdentifier(CryptoProObjectIdentifiers.gostR3410_2001, params), keyStructure);
            }
            else
            {
                info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), keyStructure);
            }

            return KeyUtil.getEncodedPrivateKeyInfo(info);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public ECParameterSpec getParams()
    {
        return (ECParameterSpec)ecSpec;
    }

    public ECParameterSpec getParameters()
    {
        return (ECParameterSpec)ecSpec;
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

    ECParameterSpec engineGetSpec()
    {
        if (ecSpec != null)
        {
            return ecSpec;
        }

        return BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
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

    private DERBitString getPublicKeyDetails(BCECPublicKey   pub)
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
