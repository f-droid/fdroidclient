package org.spongycastle.jcajce.provider.asymmetric.dsa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAPrivateKeySpec;
import java.util.Enumeration;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.DSAParameter;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.DSAPrivateKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.spongycastle.jce.interfaces.PKCS12BagAttributeCarrier;

public class BCDSAPrivateKey
    implements DSAPrivateKey, PKCS12BagAttributeCarrier
{
    private static final long serialVersionUID = -4677259546958385734L;

    private BigInteger          x;
    private transient DSAParams dsaSpec;

    private transient PKCS12BagAttributeCarrierImpl attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected BCDSAPrivateKey()
    {
    }

    BCDSAPrivateKey(
        DSAPrivateKey key)
    {
        this.x = key.getX();
        this.dsaSpec = key.getParams();
    }

    BCDSAPrivateKey(
        DSAPrivateKeySpec spec)
    {
        this.x = spec.getX();
        this.dsaSpec = new DSAParameterSpec(spec.getP(), spec.getQ(), spec.getG());
    }

    public BCDSAPrivateKey(
        PrivateKeyInfo info)
        throws IOException
    {
        DSAParameter    params = DSAParameter.getInstance(info.getPrivateKeyAlgorithm().getParameters());
        ASN1Integer      derX = (ASN1Integer)info.parsePrivateKey();

        this.x = derX.getValue();
        this.dsaSpec = new DSAParameterSpec(params.getP(), params.getQ(), params.getG());
    }

    BCDSAPrivateKey(
        DSAPrivateKeyParameters params)
    {
        this.x = params.getX();
        this.dsaSpec = new DSAParameterSpec(params.getParameters().getP(), params.getParameters().getQ(), params.getParameters().getG());
    }

    public String getAlgorithm()
    {
        return "DSA";
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
        return KeyUtil.getEncodedPrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(dsaSpec.getP(), dsaSpec.getQ(), dsaSpec.getG()).toASN1Primitive()), new ASN1Integer(getX()));
    }

    public DSAParams getParams()
    {
        return dsaSpec;
    }

    public BigInteger getX()
    {
        return x;
    }

    public boolean equals(
        Object o)
    {
        if (!(o instanceof DSAPrivateKey))
        {
            return false;
        }
        
        DSAPrivateKey other = (DSAPrivateKey)o;
        
        return this.getX().equals(other.getX()) 
            && this.getParams().getG().equals(other.getParams().getG()) 
            && this.getParams().getP().equals(other.getParams().getP()) 
            && this.getParams().getQ().equals(other.getParams().getQ());
    }

    public int hashCode()
    {
        return this.getX().hashCode() ^ this.getParams().getG().hashCode()
                ^ this.getParams().getP().hashCode() ^ this.getParams().getQ().hashCode();
    }

    public void setBagAttribute(
        ASN1ObjectIdentifier oid,
        ASN1Encodable attribute)
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

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        this.dsaSpec = new DSAParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), (BigInteger)in.readObject());
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        out.writeObject(dsaSpec.getP());
        out.writeObject(dsaSpec.getQ());
        out.writeObject(dsaSpec.getG());
    }
}
