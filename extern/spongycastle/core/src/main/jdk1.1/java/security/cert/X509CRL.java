
package java.security.cert;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;
import java.util.Set;

public abstract class X509CRL extends CRL implements X509Extension
{
    protected X509CRL()
    {
        super("X.509");
    }

    public boolean equals(Object other)
    {
        if ( this == other )
            return true;

        if ( !(other instanceof X509CRL) )
            return false;

        try
        {
            byte[] enc1 = getEncoded();
            byte[] enc2 = ((X509CRL)other).getEncoded();

            return MessageDigest.isEqual(enc1, enc2);
        }
        catch (CRLException e)
        {
            return false;
        }
    }

    public int hashCode()
    {
        int hashcode = 0;

        try
        {
            byte[] encoded = getEncoded();
            for (int i = 1; i < encoded.length; i++)
            {
                hashcode += encoded[i] * i;
            }
        }
        catch (CRLException ce)
        {
            return(hashcode);
        }

        return(hashcode);
    }

    public abstract byte[] getEncoded() throws CRLException;
    public abstract Principal getIssuerDN();
    public abstract Date getNextUpdate();
    public abstract X509CRLEntry getRevokedCertificate(BigInteger serialNumber);
    public abstract Set getRevokedCertificates();
    public abstract String getSigAlgName();
    public abstract String getSigAlgOID();
    public abstract byte[] getSigAlgParams();
    public abstract byte[] getSignature();
    public abstract byte[] getTBSCertList() throws CRLException;
    public abstract Date getThisUpdate();
    public abstract int getVersion();
    public abstract void verify(PublicKey key) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException;
    public abstract void verify(PublicKey key, String sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException;
}
