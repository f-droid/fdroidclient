
package java.security.spec;

public class PKCS8EncodedKeySpec extends EncodedKeySpec
{
    public PKCS8EncodedKeySpec(byte[] encodedKey)
    {
        super(encodedKey);
    }

    public byte[] getEncoded()
    {
        return super.getEncoded();
    }

    public final String getFormat()
    {
        return "PKCS#8";
    }
}
