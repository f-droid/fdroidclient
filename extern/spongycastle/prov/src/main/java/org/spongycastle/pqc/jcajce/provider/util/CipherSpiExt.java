package org.spongycastle.pqc.jcajce.provider.util;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

/**
 * The CipherSpiExt class extends CipherSpi.
 */
public abstract class CipherSpiExt
    extends CipherSpi
{

    /**
     * Constant specifying encrypt mode.
     */
    public static final int ENCRYPT_MODE = javax.crypto.Cipher.ENCRYPT_MODE;

    /**
     * Constant specifying decrypt mode.
     */
    public static final int DECRYPT_MODE = javax.crypto.Cipher.DECRYPT_MODE;

    /**
     * The operation mode for this cipher ({@link #ENCRYPT_MODE} or
     * {@link #DECRYPT_MODE}).
     */
    protected int opMode;

    // ****************************************************
    // JCA adapter methods
    // ****************************************************

    /**
     * Initialize this cipher object with a proper key and some random seed.
     * Before a cipher object is ready for data processing, it has to be
     * initialized according to the desired cryptographic operation, which is
     * specified by the <tt>opMode</tt> parameter.
     * <p/>
     * If this cipher (including its underlying mode or padding scheme) requires
     * any random bytes, it will obtain them from <tt>random</tt>.
     * <p/>
     * Note: If the mode needs an initialization vector, a blank array is used
     * in this case.
     *
     * @param opMode the operation mode ({@link #ENCRYPT_MODE} or
     *               {@link #DECRYPT_MODE})
     * @param key    the key
     * @param random the random seed
     * @throws java.security.InvalidKeyException if the key is inappropriate for initializing this cipher.
     */
    protected final void engineInit(int opMode, java.security.Key key,
                                    java.security.SecureRandom random)
        throws java.security.InvalidKeyException
    {

        try
        {
            engineInit(opMode, key,
                (java.security.spec.AlgorithmParameterSpec)null, random);
        }
        catch (java.security.InvalidAlgorithmParameterException e)
        {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness. The cipher is initialized for encryption or
     * decryption, depending on the value of <tt>opMode</tt>.
     * <p/>
     * If this cipher (including its underlying mode or padding scheme) requires
     * any random bytes, it will obtain them from <tt>random</tt>. Note that
     * when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     * <p/>
     * Note: If the mode needs an initialization vector, a try to retrieve it
     * from the AlgorithmParametersSpec is made.
     *
     * @param opMode    the operation mode ({@link #ENCRYPT_MODE} or
     *                  {@link #DECRYPT_MODE})
     * @param key       the key
     * @param algParams the algorithm parameters
     * @param random    the random seed
     * @throws java.security.InvalidKeyException if the key is inappropriate for initializing this block
     * cipher.
     * @throws java.security.InvalidAlgorithmParameterException if the parameters are inappropriate for initializing this
     * block cipher.
     */
    protected final void engineInit(int opMode, java.security.Key key,
                                    java.security.AlgorithmParameters algParams,
                                    java.security.SecureRandom random)
        throws java.security.InvalidKeyException,
        java.security.InvalidAlgorithmParameterException
    {

        // if algParams are not specified, initialize without them
        if (algParams == null)
        {
            engineInit(opMode, key, random);
            return;
        }

        AlgorithmParameterSpec paramSpec = null;
        // XXX getting AlgorithmParameterSpec from AlgorithmParameters

        engineInit(opMode, key, paramSpec, random);
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness. The cipher is initialized for one of the following
     * four operations: encryption, decryption, key wrapping or key unwrapping,
     * depending on the value of opMode. If this cipher (including its
     * underlying feedback or padding scheme) requires any random bytes (e.g.,
     * for parameter generation), it will get them from random. Note that when a
     * Cipher object is initialized, it loses all previously-acquired state. In
     * other words, initializing a Cipher is equivalent to creating a new
     * instance of that Cipher and initializing it.
     *
     * @param opMode   the operation mode ({@link #ENCRYPT_MODE} or
     *                 {@link #DECRYPT_MODE})
     * @param key      the encryption key
     * @param params   the algorithm parameters
     * @param javaRand the source of randomness
     * @throws java.security.InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws java.security.InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is being initialized for
     * decryption and requires algorithm parameters and the
     * parameters are null.
     */
    protected void engineInit(int opMode, java.security.Key key,
                              java.security.spec.AlgorithmParameterSpec params,
                              java.security.SecureRandom javaRand)
        throws java.security.InvalidKeyException,
        java.security.InvalidAlgorithmParameterException
    {

        if ((params != null) && !(params instanceof AlgorithmParameterSpec))
        {
            throw new java.security.InvalidAlgorithmParameterException();
        }

        if ((key == null) || !(key instanceof Key))
        {
            throw new java.security.InvalidKeyException();
        }

        this.opMode = opMode;

        if (opMode == ENCRYPT_MODE)
        {
            SecureRandom flexiRand = javaRand;
            initEncrypt((Key)key, (AlgorithmParameterSpec)params, flexiRand);

        }
        else if (opMode == DECRYPT_MODE)
        {
            initDecrypt((Key)key, (AlgorithmParameterSpec)params);

        }
    }

    /**
     * Return the result of the last step of a multi-step en-/decryption
     * operation or the result of a single-step en-/decryption operation by
     * processing the given input data and any remaining buffered data. The data
     * to be processed is given in an input byte array. Beginning at
     * inputOffset, only the first inputLen bytes are en-/decrypted, including
     * any buffered bytes of a previous update operation. If necessary, padding
     * is performed. The result is returned as a output byte array.
     *
     * @param input the byte array holding the data to be processed
     * @param inOff the offset indicating the start position within the input
     *              byte array
     * @param inLen the number of bytes to be processed
     * @return the byte array containing the en-/decrypted data
     * @throws javax.crypto.IllegalBlockSizeException if the ciphertext length is not a multiple of the
     * blocklength.
     * @throws javax.crypto.BadPaddingException if unpadding is not possible.
     */
    protected final byte[] engineDoFinal(byte[] input, int inOff, int inLen)
        throws javax.crypto.IllegalBlockSizeException,
        javax.crypto.BadPaddingException
    {
        return doFinal(input, inOff, inLen);
    }

    /**
     * Perform the last step of a multi-step en-/decryption operation or a
     * single-step en-/decryption operation by processing the given input data
     * and any remaining buffered data. The data to be processed is given in an
     * input byte array. Beginning at inputOffset, only the first inputLen bytes
     * are en-/decrypted, including any buffered bytes of a previous update
     * operation. If necessary, padding is performed. The result is stored in
     * the given output byte array, beginning at outputOffset. The number of
     * bytes stored in this byte array are returned.
     *
     * @param input  the byte array holding the data to be processed
     * @param inOff  the offset indicating the start position within the input
     *               byte array
     * @param inLen  the number of bytes to be processed
     * @param output the byte array for holding the result
     * @param outOff the offset indicating the start position within the output
     *               byte array to which the en/decrypted data is written
     * @return the number of bytes stored in the output byte array
     * @throws javax.crypto.ShortBufferException if the output buffer is too short to hold the output.
     * @throws javax.crypto.IllegalBlockSizeException if the ciphertext length is not a multiple of the
     * blocklength.
     * @throws javax.crypto.BadPaddingException if unpadding is not possible.
     */
    protected final int engineDoFinal(byte[] input, int inOff, int inLen,
                                      byte[] output, int outOff)
        throws javax.crypto.ShortBufferException,
        javax.crypto.IllegalBlockSizeException,
        javax.crypto.BadPaddingException
    {
        return doFinal(input, inOff, inLen, output, outOff);
    }

    /**
     * @return the block size (in bytes), or 0 if the underlying algorithm is
     *         not a block cipher
     */
    protected final int engineGetBlockSize()
    {
        return getBlockSize();
    }

    /**
     * Return the key size of the given key object in bits.
     *
     * @param key the key object
     * @return the key size in bits of the given key object
     * @throws java.security.InvalidKeyException if key is invalid.
     */
    protected final int engineGetKeySize(java.security.Key key)
        throws java.security.InvalidKeyException
    {
        if (!(key instanceof Key))
        {
            throw new java.security.InvalidKeyException("Unsupported key.");
        }
        return getKeySize((Key)key);
    }

    /**
     * Return the initialization vector. This is useful in the context of
     * password-based encryption or decryption, where the IV is derived from a
     * user-provided passphrase.
     *
     * @return the initialization vector in a new buffer, or <tt>null</tt> if
     *         the underlying algorithm does not use an IV, or if the IV has not
     *         yet been set.
     */
    protected final byte[] engineGetIV()
    {
        return getIV();
    }

    /**
     * Return the length in bytes that an output buffer would need to be in
     * order to hold the result of the next update or doFinal operation, given
     * the input length inputLen (in bytes).
     * <p/>
     * This call takes into account any unprocessed (buffered) data from a
     * previous update call, and padding.
     * <p/>
     * The actual output length of the next update or doFinal call may be
     * smaller than the length returned by this method.
     *
     * @param inLen the input length (in bytes)
     * @return the required output buffer size (in bytes)
     */
    protected final int engineGetOutputSize(int inLen)
    {
        return getOutputSize(inLen);
    }

    /**
     * Returns the parameters used with this cipher.
     * <p/>
     * The returned parameters may be the same that were used to initialize this
     * cipher, or may contain the default set of parameters or a set of randomly
     * generated parameters used by the underlying cipher implementation
     * (provided that the underlying cipher implementation uses a default set of
     * parameters or creates new parameters if it needs parameters but was not
     * initialized with any).
     *
     * @return the parameters used with this cipher, or null if this cipher does
     *         not use any parameters.
     */
    protected final java.security.AlgorithmParameters engineGetParameters()
    {
        // TODO
        return null;
    }

    /**
     * Set the mode of this cipher.
     *
     * @param modeName the cipher mode
     * @throws java.security.NoSuchAlgorithmException if neither the mode with the given name nor the default
     * mode can be found
     */
    protected final void engineSetMode(String modeName)
        throws java.security.NoSuchAlgorithmException
    {
        setMode(modeName);
    }

    /**
     * Set the padding scheme of this cipher.
     *
     * @param paddingName the padding scheme
     * @throws javax.crypto.NoSuchPaddingException if the requested padding scheme cannot be found.
     */
    protected final void engineSetPadding(String paddingName)
        throws javax.crypto.NoSuchPaddingException
    {
        setPadding(paddingName);
    }

    /**
     * Return the result of the next step of a multi-step en-/decryption
     * operation. The data to be processed is given in an input byte array.
     * Beginning at inputOffset, only the first inputLen bytes are
     * en-/decrypted. The result is returned as a byte array.
     *
     * @param input the byte array holding the data to be processed
     * @param inOff the offset indicating the start position within the input
     *              byte array
     * @param inLen the number of bytes to be processed
     * @return the byte array containing the en-/decrypted data
     */
    protected final byte[] engineUpdate(byte[] input, int inOff, int inLen)
    {
        return update(input, inOff, inLen);
    }

    /**
     * Perform the next step of a multi-step en-/decryption operation. The data
     * to be processed is given in an input byte array. Beginning at
     * inputOffset, only the first inputLen bytes are en-/decrypted. The result
     * is stored in the given output byte array, beginning at outputOffset. The
     * number of bytes stored in this output byte array are returned.
     *
     * @param input  the byte array holding the data to be processed
     * @param inOff  the offset indicating the start position within the input
     *               byte array
     * @param inLen  the number of bytes to be processed
     * @param output the byte array for holding the result
     * @param outOff the offset indicating the start position within the output
     *               byte array to which the en-/decrypted data is written
     * @return the number of bytes that are stored in the output byte array
     * @throws javax.crypto.ShortBufferException if the output buffer is too short to hold the output.
     */
    protected final int engineUpdate(final byte[] input, final int inOff,
                                     final int inLen, byte[] output, final int outOff)
        throws javax.crypto.ShortBufferException
    {
        return update(input, inOff, inLen, output, outOff);
    }

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for encryption.
     * <p/>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and raise an
     * InvalidAlgorithmParameterException if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p/>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p/>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     *
     * @param key          the encryption key
     * @param cipherParams the cipher parameters
     * @param random       the source of randomness
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * block cipher.
     * @throws InvalidAlgorithmParameterException if the parameters are inappropriate for initializing this
     * block cipher.
     */
    public abstract void initEncrypt(Key key,
                                     AlgorithmParameterSpec cipherParams, SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Initialize this cipher with a key, a set of algorithm parameters, and a
     * source of randomness for decryption.
     * <p/>
     * If this cipher requires any algorithm parameters and paramSpec is null,
     * the underlying cipher implementation is supposed to generate the required
     * parameters itself (using provider-specific default or random values) if
     * it is being initialized for encryption, and throw an
     * {@link InvalidAlgorithmParameterException} if it is being initialized for
     * decryption. The generated parameters can be retrieved using
     * engineGetParameters or engineGetIV (if the parameter is an IV).
     * <p/>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p/>
     * Note that when a {@link BlockCipher} object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     *
     * @param key          the encryption key
     * @param cipherParams the cipher parameters
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * block cipher.
     * @throws InvalidAlgorithmParameterException if the parameters are inappropriate for initializing this
     * block cipher.
     */
    public abstract void initDecrypt(Key key,
                                     AlgorithmParameterSpec cipherParams)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException;

    /**
     * @return the name of this cipher
     */
    public abstract String getName();

    /**
     * @return the block size (in bytes), or 0 if the underlying algorithm is
     *         not a block cipher
     */
    public abstract int getBlockSize();

    /**
     * Returns the length in bytes that an output buffer would need to be in
     * order to hold the result of the next update or doFinal operation, given
     * the input length inputLen (in bytes).
     * <p/>
     * This call takes into account any unprocessed (buffered) data from a
     * previous update call, and padding.
     * <p/>
     * The actual output length of the next update or doFinal call may be
     * smaller than the length returned by this method.
     *
     * @param inputLen the input length (in bytes)
     * @return the required output buffer size (in bytes)
     */
    public abstract int getOutputSize(int inputLen);

    /**
     * Return the key size of the given key object in bits.
     *
     * @param key the key object
     * @return the key size in bits of the given key object
     * @throws InvalidKeyException if key is invalid.
     */
    public abstract int getKeySize(Key key)
        throws InvalidKeyException;

    /**
     * Returns the parameters used with this cipher.
     * <p/>
     * The returned parameters may be the same that were used to initialize this
     * cipher, or may contain the default set of parameters or a set of randomly
     * generated parameters used by the underlying cipher implementation
     * (provided that the underlying cipher implementation uses a default set of
     * parameters or creates new parameters if it needs parameters but was not
     * initialized with any).
     *
     * @return the parameters used with this cipher, or null if this cipher does
     *         not use any parameters.
     */
    public abstract AlgorithmParameterSpec getParameters();

    /**
     * Return the initialization vector. This is useful in the context of
     * password-based encryption or decryption, where the IV is derived from a
     * user-provided passphrase.
     *
     * @return the initialization vector in a new buffer, or <tt>null</tt> if
     *         the underlying algorithm does not use an IV, or if the IV has not
     *         yet been set.
     */
    public abstract byte[] getIV();

    /**
     * Set the mode of this cipher.
     *
     * @param mode the cipher mode
     * @throws NoSuchModeException if the requested mode cannot be found.
     */
    protected abstract void setMode(String mode)
        throws NoSuchAlgorithmException;

    /**
     * Set the padding mechanism of this cipher.
     *
     * @param padding the padding mechanism
     * @throws NoSuchPaddingException if the requested padding scheme cannot be found.
     */
    protected abstract void setPadding(String padding)
        throws NoSuchPaddingException;

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     *
     * @param input the input buffer
     * @return a new buffer with the result (maybe an empty byte array)
     */
    public final byte[] update(byte[] input)
    {
        return update(input, 0, input.length);
    }

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     *
     * @param input the input buffer
     * @param inOff the offset where the input starts
     * @param inLen the input length
     * @return a new buffer with the result (maybe an empty byte array)
     */
    public abstract byte[] update(byte[] input, int inOff, int inLen);

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     *
     * @param input  the input buffer
     * @param inOff  the offset where the input starts
     * @param inLen  the input length
     * @param output the output buffer
     * @param outOff the offset where the result is stored
     * @return the length of the output
     * @throws ShortBufferException if the output buffer is too small to hold the result.
     */
    public abstract int update(byte[] input, int inOff, int inLen,
                               byte[] output, int outOff)
        throws ShortBufferException;

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @return a new buffer with the result
     * @throws IllegalBlockSizeException if this cipher is a block cipher and the total input
     * length is not a multiple of the block size (for
     * encryption when no padding is used or for decryption).
     * @throws BadPaddingException if this cipher is a block cipher and unpadding fails.
     */
    public final byte[] doFinal()
        throws IllegalBlockSizeException,
        BadPaddingException
    {
        return doFinal(null, 0, 0);
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @param input the input buffer
     * @return a new buffer with the result
     * @throws IllegalBlockSizeException if this cipher is a block cipher and the total input
     * length is not a multiple of the block size (for
     * encryption when no padding is used or for decryption).
     * @throws BadPaddingException if this cipher is a block cipher and unpadding fails.
     */
    public final byte[] doFinal(byte[] input)
        throws IllegalBlockSizeException,
        BadPaddingException
    {
        return doFinal(input, 0, input.length);
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @param input the input buffer
     * @param inOff the offset where the input starts
     * @param inLen the input length
     * @return a new buffer with the result
     * @throws IllegalBlockSizeException if this cipher is a block cipher and the total input
     * length is not a multiple of the block size (for
     * encryption when no padding is used or for decryption).
     * @throws BadPaddingException if this cipher is a block cipher and unpadding fails.
     */
    public abstract byte[] doFinal(byte[] input, int inOff, int inLen)
        throws IllegalBlockSizeException, BadPaddingException;

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @param input  the input buffer
     * @param inOff  the offset where the input starts
     * @param inLen  the input length
     * @param output the buffer for the result
     * @param outOff the offset where the result is stored
     * @return the output length
     * @throws ShortBufferException if the output buffer is too small to hold the result.
     * @throws IllegalBlockSizeException if this cipher is a block cipher and the total input
     * length is not a multiple of the block size (for
     * encryption when no padding is used or for decryption).
     * @throws BadPaddingException if this cipher is a block cipher and unpadding fails.
     */
    public abstract int doFinal(byte[] input, int inOff, int inLen,
                                byte[] output, int outOff)
        throws ShortBufferException,
        IllegalBlockSizeException, BadPaddingException;

}
