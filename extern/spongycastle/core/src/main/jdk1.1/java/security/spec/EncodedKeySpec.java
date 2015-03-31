
package java.security.spec;

public abstract class EncodedKeySpec implements KeySpec
{
    private byte[] encodedKey;

    public EncodedKeySpec(byte[] encodedKey)
    {
        this.encodedKey = (byte[])encodedKey.clone();
    }

    public byte[] getEncoded()
    {
        return (byte[])encodedKey.clone();
    }

    public abstract String getFormat();
}
