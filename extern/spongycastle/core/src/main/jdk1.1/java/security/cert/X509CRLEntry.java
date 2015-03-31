
package java.security.cert;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Date;

public abstract class X509CRLEntry implements X509Extension
{
    public boolean equals(Object other)
    {
        if ( this == other )
            return true;

        if ( !(other instanceof X509CRLEntry) )
            return false;

        try
        {
            byte[] enc1 = getEncoded();
            byte[] enc2 = ((X509CRLEntry)other).getEncoded();

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
    public abstract Date getRevocationDate();
    public abstract BigInteger getSerialNumber();
    public abstract boolean hasExtensions();
    public abstract String toString();
}
