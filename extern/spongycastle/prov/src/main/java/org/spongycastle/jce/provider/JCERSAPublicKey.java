package org.spongycastle.jce.provider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.RSAPublicKeyStructure;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.KeyUtil;

public class JCERSAPublicKey
    implements RSAPublicKey
{
    static final long serialVersionUID = 2675817738516720772L;
    
    private BigInteger modulus;
    private BigInteger publicExponent;

    JCERSAPublicKey(
        RSAKeyParameters key)
    {
        this.modulus = key.getModulus();
        this.publicExponent = key.getExponent();
    }

    JCERSAPublicKey(
        RSAPublicKeySpec spec)
    {
        this.modulus = spec.getModulus();
        this.publicExponent = spec.getPublicExponent();
    }

    JCERSAPublicKey(
        RSAPublicKey key)
    {
        this.modulus = key.getModulus();
        this.publicExponent = key.getPublicExponent();
    }

    JCERSAPublicKey(
        SubjectPublicKeyInfo    info)
    {
        try
        {
            RSAPublicKeyStructure   pubKey = new RSAPublicKeyStructure((ASN1Sequence)info.parsePublicKey());

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
        return KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), new RSAPublicKeyStructure(getModulus(), getPublicExponent()));
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
}
