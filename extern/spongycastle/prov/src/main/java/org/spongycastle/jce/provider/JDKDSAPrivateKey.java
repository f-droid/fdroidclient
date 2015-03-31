package org.spongycastle.jce.provider;

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
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.DSAParameter;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.DSAPrivateKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.spongycastle.jce.interfaces.PKCS12BagAttributeCarrier;

public class JDKDSAPrivateKey
    implements DSAPrivateKey, PKCS12BagAttributeCarrier
{
    private static final long serialVersionUID = -4677259546958385734L;

    BigInteger          x;
    DSAParams           dsaSpec;

    private PKCS12BagAttributeCarrierImpl   attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected JDKDSAPrivateKey()
    {
    }

    JDKDSAPrivateKey(
        DSAPrivateKey    key)
    {
        this.x = key.getX();
        this.dsaSpec = key.getParams();
    }

    JDKDSAPrivateKey(
        DSAPrivateKeySpec    spec)
    {
        this.x = spec.getX();
        this.dsaSpec = new DSAParameterSpec(spec.getP(), spec.getQ(), spec.getG());
    }

    JDKDSAPrivateKey(
        PrivateKeyInfo  info)
        throws IOException
    {
        DSAParameter    params = DSAParameter.getInstance(info.getPrivateKeyAlgorithm().getParameters());
        DERInteger      derX = ASN1Integer.getInstance(info.parsePrivateKey());

        this.x = derX.getValue();
        this.dsaSpec = new DSAParameterSpec(params.getP(), params.getQ(), params.getG());
    }

    JDKDSAPrivateKey(
        DSAPrivateKeyParameters  params)
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
        try
        {
            PrivateKeyInfo          info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(dsaSpec.getP(), dsaSpec.getQ(), dsaSpec.getG())), new DERInteger(getX()));

            return info.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            return null;
        }
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
        this.x = (BigInteger)in.readObject();
        this.dsaSpec = new DSAParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), (BigInteger)in.readObject());
        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
        
        attrCarrier.readObject(in);
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(x);
        out.writeObject(dsaSpec.getP());
        out.writeObject(dsaSpec.getQ());
        out.writeObject(dsaSpec.getG());

        attrCarrier.writeObject(out);
    }
}
