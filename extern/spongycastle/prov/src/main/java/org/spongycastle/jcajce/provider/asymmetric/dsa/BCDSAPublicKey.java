package org.spongycastle.jcajce.provider.asymmetric.dsa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAPublicKeySpec;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.DSAParameter;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.DSAPublicKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;

public class BCDSAPublicKey
    implements DSAPublicKey
{
    private static final long serialVersionUID = 1752452449903495175L;

    private BigInteger      y;
    private transient DSAParams       dsaSpec;

    BCDSAPublicKey(
        DSAPublicKeySpec spec)
    {
        this.y = spec.getY();
        this.dsaSpec = new DSAParameterSpec(spec.getP(), spec.getQ(), spec.getG());
    }

    BCDSAPublicKey(
        DSAPublicKey key)
    {
        this.y = key.getY();
        this.dsaSpec = key.getParams();
    }

    BCDSAPublicKey(
        DSAPublicKeyParameters params)
    {
        this.y = params.getY();
        this.dsaSpec = new DSAParameterSpec(params.getParameters().getP(), params.getParameters().getQ(), params.getParameters().getG());
    }

    BCDSAPublicKey(
        BigInteger y,
        DSAParameterSpec dsaSpec)
    {
        this.y = y;
        this.dsaSpec = dsaSpec;
    }

    public BCDSAPublicKey(
        SubjectPublicKeyInfo info)
    {

        ASN1Integer              derY;

        try
        {
            derY = (ASN1Integer)info.parsePublicKey();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("invalid info structure in DSA public key");
        }

        this.y = derY.getValue();

        if (isNotNull(info.getAlgorithm().getParameters()))
        {
            DSAParameter params = DSAParameter.getInstance(info.getAlgorithm().getParameters());
            
            this.dsaSpec = new DSAParameterSpec(params.getP(), params.getQ(), params.getG());
        }
    }

    private boolean isNotNull(ASN1Encodable parameters)
    {
        return parameters != null && !DERNull.INSTANCE.equals(parameters.toASN1Primitive());
    }

    public String getAlgorithm()
    {
        return "DSA";
    }

    public String getFormat()
    {
        return "X.509";
    }

    public byte[] getEncoded()
    {
        if (dsaSpec == null)
        {
            return KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa), new ASN1Integer(y));
        }

        return KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(dsaSpec.getP(), dsaSpec.getQ(), dsaSpec.getG()).toASN1Primitive()), new ASN1Integer(y));
    }

    public DSAParams getParams()
    {
        return dsaSpec;
    }

    public BigInteger getY()
    {
        return y;
    }

    public String toString()
    {
        StringBuffer    buf = new StringBuffer();
        String          nl = System.getProperty("line.separator");

        buf.append("DSA Public Key").append(nl);
        buf.append("            y: ").append(this.getY().toString(16)).append(nl);

        return buf.toString();
    }

    public int hashCode()
    {
        return this.getY().hashCode() ^ this.getParams().getG().hashCode() 
                ^ this.getParams().getP().hashCode() ^ this.getParams().getQ().hashCode();
    }

    public boolean equals(
        Object o)
    {
        if (!(o instanceof DSAPublicKey))
        {
            return false;
        }
        
        DSAPublicKey other = (DSAPublicKey)o;
        
        return this.getY().equals(other.getY()) 
            && this.getParams().getG().equals(other.getParams().getG()) 
            && this.getParams().getP().equals(other.getParams().getP()) 
            && this.getParams().getQ().equals(other.getParams().getQ());
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        this.dsaSpec = new DSAParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), (BigInteger)in.readObject());
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
