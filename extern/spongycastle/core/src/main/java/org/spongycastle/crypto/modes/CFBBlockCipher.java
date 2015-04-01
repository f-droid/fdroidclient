package org.spongycastle.crypto.modes;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.Arrays;

/**
 * implements a Cipher-FeedBack (CFB) mode on top of a simple cipher.
 */
public class CFBBlockCipher
    extends StreamBlockCipher
{
    private byte[]          IV;
    private byte[]          cfbV;
    private byte[]          cfbOutV;
    private byte[]          inBuf;

    private int             blockSize;
    private BlockCipher     cipher = null;
    private boolean         encrypting;
    private int             byteCount;

    /**
     * Basic constructor.
     *
     * @param cipher the block cipher to be used as the basis of the
     * feedback mode.
     * @param bitBlockSize the block size in bits (note: a multiple of 8)
     */
    public CFBBlockCipher(
        BlockCipher cipher,
        int         bitBlockSize)
    {
        super(cipher);

        this.cipher = cipher;
        this.blockSize = bitBlockSize / 8;

        this.IV = new byte[cipher.getBlockSize()];
        this.cfbV = new byte[cipher.getBlockSize()];
        this.cfbOutV = new byte[cipher.getBlockSize()];
        this.inBuf = new byte[blockSize];
    }

    /**
     * Initialise the cipher and, possibly, the initialisation vector (IV).
     * If an IV isn't passed as part of the parameter, the IV will be all zeros.
     * An IV which is too short is handled in FIPS compliant fashion.
     *
     * @param encrypting if true the cipher is initialised for
     *  encryption, if false for decryption.
     * @param params the key and other data required by the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean             encrypting,
        CipherParameters    params)
        throws IllegalArgumentException
    {
        this.encrypting = encrypting;
        
        if (params instanceof ParametersWithIV)
        {
            ParametersWithIV ivParam = (ParametersWithIV)params;
            byte[]      iv = ivParam.getIV();

            if (iv.length < IV.length)
            {
                // prepend the supplied IV with zeros (per FIPS PUB 81)
                System.arraycopy(iv, 0, IV, IV.length - iv.length, iv.length);
                for (int i = 0; i < IV.length - iv.length; i++)
                {
                    IV[i] = 0;
                }
            }
            else
            {
                System.arraycopy(iv, 0, IV, 0, IV.length);
            }

            reset();

            // if null it's an IV changed only.
            if (ivParam.getParameters() != null)
            {
                cipher.init(true, ivParam.getParameters());
            }
        }
        else
        {
            reset();

            // if it's null, key is to be reused.
            if (params != null)
            {
                cipher.init(true, params);
            }
        }
    }

    /**
     * return the algorithm name and mode.
     *
     * @return the name of the underlying algorithm followed by "/CFB"
     * and the block size in bits.
     */
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName() + "/CFB" + (blockSize * 8);
    }

    protected byte calculateByte(byte in)
          throws DataLengthException, IllegalStateException
    {
        return (encrypting) ? encryptByte(in) : decryptByte(in);
    }

    private byte encryptByte(byte in)
    {
        if (byteCount == 0)
        {
            cipher.processBlock(cfbV, 0, cfbOutV, 0);
        }

        byte rv = (byte)(cfbOutV[byteCount] ^ in);
        inBuf[byteCount++] = rv;

        if (byteCount == blockSize)
        {
            byteCount = 0;

            System.arraycopy(cfbV, blockSize, cfbV, 0, cfbV.length - blockSize);
            System.arraycopy(inBuf, 0, cfbV, cfbV.length - blockSize, blockSize);
        }

        return rv;
    }

    private byte decryptByte(byte in)
    {
        if (byteCount == 0)
        {
            cipher.processBlock(cfbV, 0, cfbOutV, 0);
        }

        inBuf[byteCount] = in;
        byte rv = (byte)(cfbOutV[byteCount++] ^ in);

        if (byteCount == blockSize)
        {
            byteCount = 0;

            System.arraycopy(cfbV, blockSize, cfbV, 0, cfbV.length - blockSize);
            System.arraycopy(inBuf, 0, cfbV, cfbV.length - blockSize, blockSize);
        }

        return rv;
    }

    /**
     * return the block size we are operating at.
     *
     * @return the block size we are operating at (in bytes).
     */
    public int getBlockSize()
    {
        return blockSize;
    }

    /**
     * Process one block of input from the array in and write it to
     * the out array.
     *
     * @param in the array containing the input data.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the output data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    public int processBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        processBytes(in, inOff, blockSize, out, outOff);

        return blockSize;
    }

    /**
     * Do the appropriate processing for CFB mode encryption.
     *
     * @param in the array containing the data to be encrypted.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the encrypted data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    public int encryptBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        processBytes(in, inOff, blockSize, out, outOff);

        return blockSize;
    }

    /**
     * Do the appropriate processing for CFB mode decryption.
     *
     * @param in the array containing the data to be decrypted.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the encrypted data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    public int decryptBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        processBytes(in, inOff, blockSize, out, outOff);

        return blockSize;
    }

    /**
     * Return the current state of the initialisation vector.
     *
     * @return current IV
     */
    public byte[] getCurrentIV()
    {
        return Arrays.clone(cfbV);
    }

    /**
     * reset the chaining vector back to the IV and reset the underlying
     * cipher.
     */
    public void reset()
    {
        System.arraycopy(IV, 0, cfbV, 0, IV.length);
        Arrays.fill(inBuf, (byte)0);
        byteCount = 0;

        cipher.reset();
    }
}
