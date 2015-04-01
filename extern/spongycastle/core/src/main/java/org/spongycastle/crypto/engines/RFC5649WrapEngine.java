package org.spongycastle.crypto.engines;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.Wrapper;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Pack;

/**
 * An implementation of the AES Key Wrap with Padding specification
 * as described in RFC 5649.
 * <p>
 * For details on the specification see:
 * <a href="https://tools.ietf.org/html/rfc5649">https://tools.ietf.org/html/rfc5649</a>
 * </p>
 */
public class RFC5649WrapEngine
    implements Wrapper
{
    private BlockCipher engine;
    private KeyParameter param;
    private boolean forWrapping;

    // The AIV as defined in the RFC
    private byte[] highOrderIV = {(byte)0xa6, (byte)0x59, (byte)0x59, (byte)0xa6};
    private byte[] preIV = highOrderIV;

    private byte[] extractedAIV = null;

    public RFC5649WrapEngine(BlockCipher engine)
    {
        this.engine = engine;
    }

    public void init(boolean forWrapping, CipherParameters param)
    {
        this.forWrapping = forWrapping;

        if (param instanceof ParametersWithRandom)
        {
            param = ((ParametersWithRandom)param).getParameters();
        }

        if (param instanceof KeyParameter)
        {
            this.param = (KeyParameter)param;
        }
        else if (param instanceof ParametersWithIV)
        {
            this.preIV = ((ParametersWithIV)param).getIV();
            this.param = (KeyParameter)((ParametersWithIV)param).getParameters();
            if (this.preIV.length != 4)
            {
                throw new IllegalArgumentException("IV length not equal to 4");
            }
        }
    }

    public String getAlgorithmName()
    {
        return engine.getAlgorithmName();
    }

    /**
     * Pads the plaintext (i.e., the key to be wrapped)
     * as per section 4.1 of RFC 5649.
     *
     * @param plaintext The key being wrapped.
     * @return The padded key.
     */
    private byte[] padPlaintext(byte[] plaintext)
    {
        int plaintextLength = plaintext.length;
        int numOfZerosToAppend = (8 - (plaintextLength % 8)) % 8;
        byte[] paddedPlaintext = new byte[plaintextLength + numOfZerosToAppend];
        System.arraycopy(plaintext, 0, paddedPlaintext, 0, plaintextLength);
        if (numOfZerosToAppend != 0)
        {
            // plaintext (i.e., key to be wrapped) does not have
            // a multiple of 8 octet blocks so it must be padded
            byte[] zeros = new byte[numOfZerosToAppend];
            System.arraycopy(zeros, 0, paddedPlaintext, plaintextLength, numOfZerosToAppend);
        }
        return paddedPlaintext;
    }

    public byte[] wrap(byte[] in, int inOff, int inLen)
    {
        if (!forWrapping)
        {
            throw new IllegalStateException("not set for wrapping");
        }
        byte[] iv = new byte[8];

        // MLI = size of key to be wrapped
        byte[] mli = Pack.intToBigEndian(inLen);
        // copy in the fixed portion of the AIV
        System.arraycopy(preIV, 0, iv, 0, preIV.length);
        // copy in the MLI after the AIV
        System.arraycopy(mli, 0, iv, preIV.length, mli.length);

        // get the relevant plaintext to be wrapped
        byte[] relevantPlaintext = new byte[inLen];
        System.arraycopy(in, inOff, relevantPlaintext, 0, inLen);
        byte[] paddedPlaintext = padPlaintext(relevantPlaintext);

        if (paddedPlaintext.length == 8)
        {
            // if the padded plaintext contains exactly 8 octets,
            // then prepend iv and encrypt using AES in ECB mode.

            // prepend the IV to the plaintext
            byte[] paddedPlainTextWithIV = new byte[paddedPlaintext.length + iv.length];
            System.arraycopy(iv, 0, paddedPlainTextWithIV, 0, iv.length);
            System.arraycopy(paddedPlaintext, 0, paddedPlainTextWithIV, iv.length, paddedPlaintext.length);

            engine.init(true, param);
            for (int i = 0; i < paddedPlainTextWithIV.length; i += engine.getBlockSize())
            {
                engine.processBlock(paddedPlainTextWithIV, i, paddedPlainTextWithIV, i);
            }

            return paddedPlainTextWithIV;
        }
        else
        {
            // otherwise, apply the RFC 3394 wrap to
            // the padded plaintext with the new IV
            Wrapper wrapper = new RFC3394WrapEngine(engine);
            ParametersWithIV paramsWithIV = new ParametersWithIV(param, iv);
            wrapper.init(true, paramsWithIV);
            return wrapper.wrap(paddedPlaintext, inOff, paddedPlaintext.length);
        }

    }

    public byte[] unwrap(byte[] in, int inOff, int inLen)
        throws InvalidCipherTextException
    {
        if (forWrapping)
        {
            throw new IllegalStateException("not set for unwrapping");
        }

        int n = inLen / 8;

        if ((n * 8) != inLen)
        {
            throw new InvalidCipherTextException("unwrap data must be a multiple of 8 bytes");
        }

        if (n == 1)
        {
            throw new InvalidCipherTextException("unwrap data must be at least 16 bytes");
        }

        byte[] relevantCiphertext = new byte[inLen];
        System.arraycopy(in, inOff, relevantCiphertext, 0, inLen);
        byte[] decrypted = new byte[inLen];
        byte[] paddedPlaintext;

        if (n == 2)
        {
            // When there are exactly two 64-bit blocks of ciphertext,
            // they are decrypted as a single block using AES in ECB.
            engine.init(false, param);
            for (int i = 0; i < relevantCiphertext.length; i += engine.getBlockSize())
            {
                engine.processBlock(relevantCiphertext, i, decrypted, i);
            }

            // extract the AIV
            extractedAIV = new byte[8];
            System.arraycopy(decrypted, 0, extractedAIV, 0, extractedAIV.length);
            paddedPlaintext = new byte[decrypted.length - extractedAIV.length];
            System.arraycopy(decrypted, extractedAIV.length, paddedPlaintext, 0, paddedPlaintext.length);
        }
        else
        {
            // Otherwise, unwrap as per RFC 3394 but don't check IV the same way
            decrypted = rfc3394UnwrapNoIvCheck(in, inOff, inLen);
            paddedPlaintext = decrypted;
        }

        // Decompose the extracted AIV to the fixed portion and the MLI
        byte[] extractedHighOrderAIV = new byte[4];
        byte[] mliBytes = new byte[4];
        System.arraycopy(extractedAIV, 0, extractedHighOrderAIV, 0, extractedHighOrderAIV.length);
        System.arraycopy(extractedAIV, extractedHighOrderAIV.length, mliBytes, 0, mliBytes.length);
        int mli = Pack.bigEndianToInt(mliBytes, 0);

        // Even if a check fails we still continue and check everything 
        // else in order to avoid certain timing based side-channel attacks.
        boolean isValid = true;

        // Check the fixed portion of the AIV
        if (!Arrays.constantTimeAreEqual(extractedHighOrderAIV, preIV))
        {
            isValid = false;
        }

        // Check the MLI against the actual length
        int upperBound = paddedPlaintext.length;
        int lowerBound = upperBound - 8;
        if (mli <= lowerBound)
        {
            isValid = false;
        }
        if (mli > upperBound)
        {
            isValid = false;
        }

        // Check the number of padded zeros
        int expectedZeros = upperBound - mli;
        byte[] zeros = new byte[expectedZeros];
        byte[] pad = new byte[expectedZeros];
        System.arraycopy(paddedPlaintext, paddedPlaintext.length - expectedZeros, pad, 0, expectedZeros);
        if (!Arrays.constantTimeAreEqual(pad, zeros))
        {
            isValid = false;
        }

        // Extract the plaintext from the padded plaintext
        byte[] plaintext = new byte[mli];
        System.arraycopy(paddedPlaintext, 0, plaintext, 0, plaintext.length);

        if (!isValid)
        {
            throw new InvalidCipherTextException("checksum failed");
        }

        return plaintext;
    }

    /**
     * Performs steps 1 and 2 of the unwrap process defined in RFC 3394.
     * This code is duplicated from RFC3394WrapEngine because that class
     * will throw an error during unwrap because the IV won't match up.
     *
     * @param in
     * @param inOff
     * @param inLen
     * @return Unwrapped data.
     */
    private byte[] rfc3394UnwrapNoIvCheck(byte[] in, int inOff, int inLen)
    {
        byte[] iv = new byte[8];
        byte[] block = new byte[inLen - iv.length];
        byte[] a = new byte[iv.length];
        byte[] buf = new byte[8 + iv.length];

        System.arraycopy(in, inOff, a, 0, iv.length);
        System.arraycopy(in, inOff + iv.length, block, 0, inLen - iv.length);

        engine.init(false, param);

        int n = inLen / 8;
        n = n - 1;

        for (int j = 5; j >= 0; j--)
        {
            for (int i = n; i >= 1; i--)
            {
                System.arraycopy(a, 0, buf, 0, iv.length);
                System.arraycopy(block, 8 * (i - 1), buf, iv.length, 8);

                int t = n * j + i;
                for (int k = 1; t != 0; k++)
                {
                    byte v = (byte)t;

                    buf[iv.length - k] ^= v;

                    t >>>= 8;
                }

                engine.processBlock(buf, 0, buf, 0);
                System.arraycopy(buf, 0, a, 0, 8);
                System.arraycopy(buf, 8, block, 8 * (i - 1), 8);
            }
        }

        // set the extracted AIV
        extractedAIV = a;

        return block;
    }

}
