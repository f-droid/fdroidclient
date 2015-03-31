
package java.security.spec;

public class X509EncodedKeySpec extends EncodedKeySpec
{
    public X509EncodedKeySpec(byte[] encodedKey)
    {
        super(encodedKey);
    }

    public byte[] getEncoded()
    {
        return super.getEncoded();
    }

    public final String getFormat()
    {
        return "X.509";
    }
}
