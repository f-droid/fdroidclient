package org.spongycastle.jce.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.pkcs.DHParameter;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.DHDomainParameters;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.crypto.params.DHPublicKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;

public class JCEDHPublicKey
    implements DHPublicKey
{
    static final long serialVersionUID = -216691575254424324L;
    
    private BigInteger              y;
    private DHParameterSpec         dhSpec;
    private SubjectPublicKeyInfo    info;
    
    JCEDHPublicKey(
        DHPublicKeySpec    spec)
    {
        this.y = spec.getY();
        this.dhSpec = new DHParameterSpec(spec.getP(), spec.getG());
    }

    JCEDHPublicKey(
        DHPublicKey    key)
    {
        this.y = key.getY();
        this.dhSpec = key.getParams();
    }

    JCEDHPublicKey(
        DHPublicKeyParameters  params)
    {
        this.y = params.getY();
        this.dhSpec = new DHParameterSpec(params.getParameters().getP(), params.getParameters().getG(), params.getParameters().getL());
    }

    JCEDHPublicKey(
        BigInteger        y,
        DHParameterSpec   dhSpec)
    {
        this.y = y;
        this.dhSpec = dhSpec;
    }

    JCEDHPublicKey(
        SubjectPublicKeyInfo    info)
    {
        this.info = info;

        DERInteger              derY;
        try
        {
            derY = (DERInteger)info.parsePublicKey();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("invalid info structure in DH public key");
        }

        this.y = derY.getValue();

        ASN1Sequence seq = ASN1Sequence.getInstance(info.getAlgorithmId().getParameters());
        DERObjectIdentifier id = info.getAlgorithmId().getAlgorithm();

        // we need the PKCS check to handle older keys marked with the X9 oid.
        if (id.equals(PKCSObjectIdentifiers.dhKeyAgreement) || isPKCSParam(seq))
        {
            DHParameter             params = DHParameter.getInstance(seq);

            if (params.getL() != null)
            {
                this.dhSpec = new DHParameterSpec(params.getP(), params.getG(), params.getL().intValue());
            }
            else
            {
                this.dhSpec = new DHParameterSpec(params.getP(), params.getG());
            }
        }
        else if (id.equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            DHDomainParameters params = DHDomainParameters.getInstance(seq);

            this.dhSpec = new DHParameterSpec(params.getP().getValue(), params.getG().getValue());
        }
        else
        {
            throw new IllegalArgumentException("unknown algorithm type: " + id);
        }
    }

    public String getAlgorithm()
    {
        return "DH";
    }

    public String getFormat()
    {
        return "X.509";
    }

    public byte[] getEncoded()
    {
        if (info != null)
        {
            return KeyUtil.getEncodedSubjectPublicKeyInfo(info);
        }

        return KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, new DHParameter(dhSpec.getP(), dhSpec.getG(), dhSpec.getL())), new DERInteger(y));
    }

    public DHParameterSpec getParams()
    {
        return dhSpec;
    }

    public BigInteger getY()
    {
        return y;
    }

    private boolean isPKCSParam(ASN1Sequence seq)
    {
        if (seq.size() == 2)
        {
            return true;
        }
        
        if (seq.size() > 3)
        {
            return false;
        }

        DERInteger l = DERInteger.getInstance(seq.getObjectAt(2));
        DERInteger p = DERInteger.getInstance(seq.getObjectAt(0));

        if (l.getValue().compareTo(BigInteger.valueOf(p.getValue().bitLength())) > 0)
        {
            return false;
        }

        return true;
    }

    private void readObject(
        ObjectInputStream   in)
        throws IOException, ClassNotFoundException
    {
        this.y = (BigInteger)in.readObject();
        this.dhSpec = new DHParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject(), in.readInt());
    }

    private void writeObject(
        ObjectOutputStream  out)
        throws IOException
    {
        out.writeObject(this.getY());
        out.writeObject(dhSpec.getP());
        out.writeObject(dhSpec.getG());
        out.writeInt(dhSpec.getL());
    }
}
