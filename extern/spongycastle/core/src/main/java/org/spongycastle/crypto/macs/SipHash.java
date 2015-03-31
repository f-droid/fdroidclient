package org.spongycastle.crypto.macs;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.util.Pack;
import org.spongycastle.util.Arrays;

/**
 * Implementation of SipHash as specified in "SipHash: a fast short-input PRF", by Jean-Philippe
 * Aumasson and Daniel J. Bernstein (https://131002.net/siphash/siphash.pdf).
 * <p/>
 * "SipHash is a family of PRFs SipHash-c-d where the integer parameters c and d are the number of
 * compression rounds and the number of finalization rounds. A compression round is identical to a
 * finalization round and this round function is called SipRound. Given a 128-bit key k and a
 * (possibly empty) byte string m, SipHash-c-d returns a 64-bit value..."
 */
public class SipHash
    implements Mac
{

    protected final int c, d;

    protected long k0, k1;
    protected long v0, v1, v2, v3, v4;

    protected byte[] buf = new byte[8];
    protected int bufPos = 0;
    protected int wordCount = 0;

    /**
     * SipHash-2-4
     */
    public SipHash()
    {
        // use of this confuses flow analyser on earlier JDKs.
        this.c = 2;
        this.d = 4;
    }

    /**
     * SipHash-c-d
     *
     * @param c the number of compression rounds
     * @param d the number of finalization rounds
     */
    public SipHash(int c, int d)
    {
        this.c = c;
        this.d = d;
    }

    public String getAlgorithmName()
    {
        return "SipHash-" + c + "-" + d;
    }

    public int getMacSize()
    {
        return 8;
    }

    public void init(CipherParameters params)
        throws IllegalArgumentException
    {
        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException("'params' must be an instance of KeyParameter");
        }
        KeyParameter keyParameter = (KeyParameter)params;
        byte[] key = keyParameter.getKey();
        if (key.length != 16)
        {
            throw new IllegalArgumentException("'params' must be a 128-bit key");
        }

        this.k0 = Pack.littleEndianToLong(key, 0);
        this.k1 = Pack.littleEndianToLong(key, 8);

        reset();
    }

    public void update(byte input)
        throws IllegalStateException
    {

        buf[bufPos] = input;
        if (++bufPos == buf.length)
        {
            processMessageWord();
            bufPos = 0;
        }
    }

    public void update(byte[] input, int offset, int length)
        throws DataLengthException,
        IllegalStateException
    {

        for (int i = 0; i < length; ++i)
        {
            buf[bufPos] = input[offset + i];
            if (++bufPos == buf.length)
            {
                processMessageWord();
                bufPos = 0;
            }
        }
    }

    public long doFinal()
        throws DataLengthException, IllegalStateException
    {

        buf[7] = (byte)(((wordCount << 3) + bufPos) & 0xff);
        while (bufPos < 7)
        {
            buf[bufPos++] = 0;
        }

        processMessageWord();

        v2 ^= 0xffL;

        applySipRounds(d);

        long result = v0 ^ v1 ^ v2 ^ v3;

        reset();

        return result;
    }

    public int doFinal(byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {

        long result = doFinal();
        Pack.longToLittleEndian(result, out, outOff);
        return 8;
    }

    public void reset()
    {

        v0 = k0 ^ 0x736f6d6570736575L;
        v1 = k1 ^ 0x646f72616e646f6dL;
        v2 = k0 ^ 0x6c7967656e657261L;
        v3 = k1 ^ 0x7465646279746573L;

        Arrays.fill(buf, (byte)0);
        bufPos = 0;
        wordCount = 0;
    }

    protected void processMessageWord()
    {

        ++wordCount;
        long m = Pack.littleEndianToLong(buf, 0);
        v3 ^= m;
        applySipRounds(c);
        v0 ^= m;
    }

    protected void applySipRounds(int n)
    {
        for (int r = 0; r < n; ++r)
        {
            v0 += v1;
            v2 += v3;
            v1 = rotateLeft(v1, 13);
            v3 = rotateLeft(v3, 16);
            v1 ^= v0;
            v3 ^= v2;
            v0 = rotateLeft(v0, 32);
            v2 += v1;
            v0 += v3;
            v1 = rotateLeft(v1, 17);
            v3 = rotateLeft(v3, 21);
            v1 ^= v2;
            v3 ^= v0;
            v2 = rotateLeft(v2, 32);
        }
    }

    protected static long rotateLeft(long x, int n)
    {
        return (x << n) | (x >>> (64 - n));
    }
}
