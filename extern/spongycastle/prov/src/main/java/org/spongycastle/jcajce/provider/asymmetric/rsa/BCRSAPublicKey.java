package org.spongycastle.jcajce.provider.asymmetric.rsa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;

public class BCRSAPublicKey
    implements RSAPublicKey
{
    private static final AlgorithmIdentifier DEFAULT_ALGORITHM_IDENTIFIER = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);

    static final long serialVersionUID = 2675817738516720772L;

    private BigInteger modulus;
    private BigInteger publicExponent;
    private transient AlgorithmIdentifier algorithmIdentifier;

    BCRSAPublicKey(
        RSAKeyParameters key)
    {
        this.algorithmIdentifier = DEFAULT_ALGORITHM_IDENTIFIER;
        this.modulus = key.getModulus();
        this.publicExponent = key.getExponent();
    }

    BCRSAPublicKey(
        RSAPublicKeySpec spec)
    {
        this.algorithmIdentifier = DEFAULT_ALGORITHM_IDENTIFIER;
        this.modulus = spec.getModulus();
        this.publicExponent = spec.getPublicExponent();
    }

    BCRSAPublicKey(
        RSAPublicKey key)
    {
        this.algorithmIdentifier = DEFAULT_ALGORITHM_IDENTIFIER;
        this.modulus = key.getModulus();
        this.publicExponent = key.getPublicExponent();
    }

    BCRSAPublicKey(
        SubjectPublicKeyInfo info)
    {
        populateFromPublicKeyInfo(info);
    }

    private void populateFromPublicKeyInfo(SubjectPublicKeyInfo info)
    {
        try
        {
            org.spongycastle.asn1.pkcs.RSAPublicKey  pubKey = org.spongycastle.asn1.pkcs.RSAPublicKey.getInstance(info.parsePublicKey());

            this.algorithmIdentifier = info.getAlgorithm();
            this.modulus = pubKey.getModulus();
            this.publicExponent = pubKey.getPublicExponent();
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("invalid info structure in RSA public key");
        }
    }

    /**
     * return the modulus.
     *
     * @return the modulus.
     */
    public BigInteger getModulus()
    {
        return modulus;
    }

    /**
     * return the public exponent.
     *
     * @return the public exponent.
     */
    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }

    public String getAlgorithm()
    {
        return "RSA";
    }

    public String getFormat()
    {
        return "X.509";
    }

    public byte[] getEncoded()
    {
        return KeyUtil.getEncodedSubjectPublicKeyInfo(algorithmIdentifier, new org.spongycastle.asn1.pkcs.RSAPublicKey(getModulus(), getPublicExponent()));
    }

    public int hashCode()
    {
        return this.getModulus().hashCode() ^ this.getPublicExponent().hashCode();
    }

    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof RSAPublicKey))
        {
            return false;
        }

        RSAPublicKey key = (RSAPublicKey)o;

        return getModulus().equals(key.getModulus())
            && getPublicExponent().equals(key.getPublicExponent());
    }

    public String toString()
    {
        StringBuffer    buf = new StringBuffer();
        String          nl = System.getProperty("line.separator");

        buf.append("RSA Public Key").append(nl);
        buf.append("            modulus: ").append(this.getModulus().toString(16)).append(nl);
        buf.append("    public exponent: ").append(this.getPublicExponent().toString(16)).append(nl);

        return buf.toString();
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        try
        {
            algorithmIdentifier = AlgorithmIdentifier.getInstance(in.readObject());
        }
        catch (OptionalDataException e)
        {
            algorithmIdentifier = DEFAULT_ALGORITHM_IDENTIFIER;
        }
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        if (!algorithmIdentifier.equals(DEFAULT_ALGORITHM_IDENTIFIER))
        {
            out.writeObject(algorithmIdentifier.getEncoded());
        }
    }
}
