package org.spongycastle.crypto.engines;

import java.math.BigInteger;

/**
 * Class, holding Cramer Shoup ciphertexts (u1, u2, e, v)
 */
public class CramerShoupCiphertext
{

    BigInteger u1, u2, e, v;

    public CramerShoupCiphertext()
    {

    }

    public CramerShoupCiphertext(BigInteger u1, BigInteger u2, BigInteger e, BigInteger v)
    {
        this.u1 = u1;
        this.u2 = u2;
        this.e = e;
        this.v = v;
    }

    public CramerShoupCiphertext(byte[] c)
    {
        int off = 0, s;
        byte[] size = new byte[4];
        byte[] tmp;

        System.arraycopy(c, off, size, 0, 4);
        s = byteArrayToInt(size);
        tmp = new byte[s];
        off += 4;
        System.arraycopy(c, off, tmp, 0, s);
        off += s;
        u1 = new BigInteger(tmp);

        System.arraycopy(c, off, size, 0, 4);
        s = byteArrayToInt(size);
        tmp = new byte[s];
        off += 4;
        System.arraycopy(c, off, tmp, 0, s);
        off += s;
        u2 = new BigInteger(tmp);

        System.arraycopy(c, off, size, 0, 4);
        s = byteArrayToInt(size);
        tmp = new byte[s];
        off += 4;
        System.arraycopy(c, off, tmp, 0, s);
        off += s;
        e = new BigInteger(tmp);

        System.arraycopy(c, off, size, 0, 4);
        s = byteArrayToInt(size);
        tmp = new byte[s];
        off += 4;
        System.arraycopy(c, off, tmp, 0, s);
        off += s;
        v = new BigInteger(tmp);
    }

    public BigInteger getU1()
    {
        return u1;
    }

    public void setU1(BigInteger u1)
    {
        this.u1 = u1;
    }

    public BigInteger getU2()
    {
        return u2;
    }

    public void setU2(BigInteger u2)
    {
        this.u2 = u2;
    }

    public BigInteger getE()
    {
        return e;
    }

    public void setE(BigInteger e)
    {
        this.e = e;
    }

    public BigInteger getV()
    {
        return v;
    }

    public void setV(BigInteger v)
    {
        this.v = v;
    }

    public String toString()
    {
        StringBuffer result = new StringBuffer();

        result.append("u1: " + u1.toString());
        result.append("\nu2: " + u2.toString());
        result.append("\ne: " + e.toString());
        result.append("\nv: " + v.toString());

        return result.toString();
    }

    /**
     * convert the cipher-text in a byte array,
     * prepending them with 4 Bytes for their length
     *
     * @return
     */
    public byte[] toByteArray()
    {

        byte[] u1Bytes = u1.toByteArray();
        int u1Length = u1Bytes.length;
        byte[] u2Bytes = u2.toByteArray();
        int u2Length = u2Bytes.length;
        byte[] eBytes = e.toByteArray();
        int eLength = eBytes.length;
        byte[] vBytes = v.toByteArray();
        int vLength = vBytes.length;

        int off = 0;
        byte[] result = new byte[u1Length + u2Length + eLength + vLength + 4 * 4];
        System.arraycopy(intToByteArray(u1Length), 0, result, 0, 4);
        off += 4;
        System.arraycopy(u1Bytes, 0, result, off, u1Length);
        off += u1Length;
        System.arraycopy(intToByteArray(u2Length), 0, result, off, 4);
        off += 4;
        System.arraycopy(u2Bytes, 0, result, off, u2Length);
        off += u2Length;
        System.arraycopy(intToByteArray(eLength), 0, result, off, 4);
        off += 4;
        System.arraycopy(eBytes, 0, result, off, eLength);
        off += eLength;
        System.arraycopy(intToByteArray(vLength), 0, result, off, 4);
        off += 4;
        System.arraycopy(vBytes, 0, result, off, vLength);

        return result;
    }

    private byte[] intToByteArray(int in)
    {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            bytes[3 - i] = (byte)(in >>> (i * 8));
        }
        return bytes;
    }

    private int byteArrayToInt(byte[] in)
    {
        if (in.length != 4)
        {
            return -1;
        }
        int r = 0;
        for (int i = 3; i >= 0; i--)
        {
            r += (int)in[i] << ((3 - i) * 8);
        }
        return r;
    }
}