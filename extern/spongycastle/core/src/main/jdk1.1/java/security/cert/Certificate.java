
package java.security.cert;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;

public abstract class Certificate extends Object
{
    private String type;

    protected Certificate(String type)
    {
        this.type = type;
    }

    public boolean equals(Object other)
    {
        if ( !(other instanceof Certificate) )
            return false;

        if ( other == this )
            return true;

        try
        {
            byte[] enc1 = getEncoded();
            byte[] enc2 = ((Certificate)other).getEncoded();

            return MessageDigest.isEqual(enc1, enc2);
        }
        catch (CertificateEncodingException e)
        {
            return false;
        }
    }

    public final String getType()
    {
        return type;
    }

    // XXX
    public int hashCode()
    {
        try
        {
            byte[] enc1 = getEncoded();
            int hc = 0;
            for (int i = 0; i < enc1.length; i++)
            {
                hc += enc1[i];
            }

            return hc;
        }
        catch (CertificateEncodingException e)
        {
            return 0;
        }
    }

    public abstract byte[] getEncoded()
        throws CertificateEncodingException;

    public abstract PublicKey getPublicKey();

    public abstract String toString();

    public abstract void verify(PublicKey key)
        throws CertificateException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException, SignatureException;

    public abstract void verify(PublicKey key, String sigProvider)
        throws CertificateException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException, SignatureException;
}
