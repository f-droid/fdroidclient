package org.spongycastle.pqc.jcajce.provider.util;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;


/**
 * The AsymmetricBlockCipher class extends CipherSpiExt.
 * NOTE: Some Ciphers are using Padding. OneAndZeroesPadding is used as default
 * padding. However padding can still be specified, but mode is not supported;
 * if you try to instantiate the cipher with something else than "NONE" as mode
 * NoSuchAlgorithmException is thrown.
 */
public abstract class AsymmetricBlockCipher
    extends CipherSpiExt
{

    /**
     * ParameterSpec used with this cipher
     */
    protected AlgorithmParameterSpec paramSpec;

    /**
     * Internal buffer
     */
    protected ByteArrayOutputStream buf;

    /**
     * The maximum number of bytes the cipher can decrypt.
     */
    protected int maxPlainTextSize;

    /**
     * The maximum number of bytes the cipher can encrypt.
     */
    protected int cipherTextSize;

    /**
     * The AsymmetricBlockCipher() constructor
     */
    public AsymmetricBlockCipher()
    {
        buf = new ByteArrayOutputStream();
    }

    /**
     * Return the block size (in bytes). Note: although the ciphers extending
     * this class are not block ciphers, the method was adopted to return the
     * maximal plaintext and ciphertext sizes for non hybrid ciphers. If the
     * cipher is hybrid, it returns 0.
     *
     * @return if the cipher is not a hybrid one the max plain/cipher text size
     *         is returned, otherwise 0 is returned
     */
    public final int getBlockSize()
    {
        return opMode == ENCRYPT_MODE ? maxPlainTextSize : cipherTextSize;
    }

    /**
     * @return <tt>null</tt> since no initialization vector is used.
     */
    public final byte[] getIV()
    {
        return null;
    }

    /**
     * Return the length in bytes that an output buffer would need to be in
     * order to hold the result of the next update or doFinal operation, given
     * the input length <tt>inLen</tt> (in bytes). This call takes into
     * account any unprocessed (buffered) data from a previous update call, and
     * padding. The actual output length of the next update() or doFinal() call
     * may be smaller than the length returned by this method.
     * <p/>
     * If the input length plus the length of the buffered data exceeds the
     * maximum length, <tt>0</tt> is returned.
     *
     * @param inLen the length of the input
     * @return the length of the ciphertext or <tt>0</tt> if the input is too
     *         long.
     */
    public final int getOutputSize(int inLen)
    {

        int totalLen = inLen + buf.size();

        int maxLen = getBlockSize();

        if (totalLen > maxLen)
        {
            // the length of the input exceeds the maximal supported length
            return 0;
        }

        return maxLen;
    }

    /**
     * <p/>
     * Returns the parameters used with this cipher.
     * <p/>
     * The returned parameters may be the same that were used to initialize this
     * cipher, or may contain the default set of parameters or a set of randomly
     * generated parameters used by the underlying cipher implementation
     * (provided that the underlying cipher implementation uses a default set of
     * parameters or creates new parameters if it needs parameters but was not
     * initialized with any).
     * <p/>
     *
     * @return the parameters used with this cipher, or null if this cipher does
     *         not use any parameters.
     */
    public final AlgorithmParameterSpec getParameters()
    {
        return paramSpec;
    }

    /**
     * Initializes the cipher for encryption by forwarding it to
     * initEncrypt(Key, FlexiSecureRandom).
     * <p/>
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     *
     * @param key the encryption or decryption key.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     */
    public final void initEncrypt(Key key)
        throws InvalidKeyException
    {
        try
        {
            initEncrypt(key, null, new SecureRandom());
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new InvalidParameterException(
                "This cipher needs algorithm parameters for initialization (cannot be null).");
        }
    }

    /**
     * Initialize this cipher for encryption by forwarding it to
     * initEncrypt(Key, FlexiSecureRandom, AlgorithmParameterSpec).
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     *
     * @param key    the encryption or decryption key.
     * @param random the source of randomness.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     */
    public final void initEncrypt(Key key, SecureRandom random)
        throws InvalidKeyException
    {

        try
        {
            initEncrypt(key, null, random);
        }
        catch (InvalidAlgorithmParameterException iape)
        {
            throw new InvalidParameterException(
                "This cipher needs algorithm parameters for initialization (cannot be null).");
        }
    }

    /**
     * Initializes the cipher for encryption by forwarding it to
     * initEncrypt(Key, FlexiSecureRandom, AlgorithmParameterSpec).
     *
     * @param key    the encryption or decryption key.
     * @param params the algorithm parameters.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     * @throws InvalidAlgorithmParameterException if the given algortihm parameters are inappropriate for
     * this cipher, or if this cipher is being initialized for
     * decryption and requires algorithm parameters and params
     * is null.
     */
    public final void initEncrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        initEncrypt(key, params, new SecureRandom());
    }

    /**
     * This method initializes the AsymmetricBlockCipher with a certain key for
     * data encryption.
     * <p/>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p/>
     * Note that when a Cipher object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     * <p/>
     *
     * @param key          the key which has to be used to encrypt data.
     * @param secureRandom the source of randomness.
     * @param params       the algorithm parameters.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is being initialized for
     * decryption and requires algorithm parameters and params
     * is null.
     */
    public final void initEncrypt(Key key, AlgorithmParameterSpec params,
                                  SecureRandom secureRandom)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException
    {
        opMode = ENCRYPT_MODE;
        initCipherEncrypt(key, params, secureRandom);
    }

    /**
     * Initialize the cipher for decryption by forwarding it to
     * {@link #initDecrypt(Key, AlgorithmParameterSpec)}.
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using engineGetParameters or
     * engineGetIV (if the parameter is an IV).
     *
     * @param key the encryption or decryption key.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     */
    public final void initDecrypt(Key key)
        throws InvalidKeyException
    {
        try
        {
            initDecrypt(key, null);
        }
        catch (InvalidAlgorithmParameterException iape)
        {
            throw new InvalidParameterException(
                "This cipher needs algorithm parameters for initialization (cannot be null).");
        }
    }

    /**
     * This method initializes the AsymmetricBlockCipher with a certain key for
     * data decryption.
     * <p/>
     * If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes (e.g., for parameter generation), it will get
     * them from random.
     * <p/>
     * Note that when a Cipher object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     * <p/>
     *
     * @param key    the key which has to be used to decrypt data.
     * @param params the algorithm parameters.
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is being initialized for
     * decryption and requires algorithm parameters and params
     * is null.
     */
    public final void initDecrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        opMode = DECRYPT_MODE;
        initCipherDecrypt(key, params);
    }

    /**
     * Continue a multiple-part encryption or decryption operation. This method
     * just writes the input into an internal buffer.
     *
     * @param input byte array containing the next part of the input
     * @param inOff index in the array where the input starts
     * @param inLen length of the input
     * @return a new buffer with the result (always empty)
     */
    public final byte[] update(byte[] input, int inOff, int inLen)
    {
        if (inLen != 0)
        {
            buf.write(input, inOff, inLen);
        }
        return new byte[0];
    }

    /**
     * Continue a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized), processing another data part.
     *
     * @param input  the input buffer
     * @param inOff  the offset where the input starts
     * @param inLen  the input length
     * @param output the output buffer
     * @param outOff the offset where the result is stored
     * @return the length of the output (always 0)
     */
    public final int update(byte[] input, int inOff, int inLen, byte[] output,
                            int outOff)
    {
        update(input, inOff, inLen);
        return 0;
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @param input the input buffer
     * @param inOff the offset where the input starts
     * @param inLen the input length
     * @return a new buffer with the result
     * @throws IllegalBlockSizeException if the plaintext or ciphertext size is too large.
     * @throws BadPaddingException if the ciphertext is invalid.
     */
    public final byte[] doFinal(byte[] input, int inOff, int inLen)
        throws IllegalBlockSizeException, BadPaddingException
    {

        checkLength(inLen);
        update(input, inOff, inLen);
        byte[] mBytes = buf.toByteArray();
        buf.reset();

        switch (opMode)
        {
        case ENCRYPT_MODE:
            return messageEncrypt(mBytes);

        case DECRYPT_MODE:
            return messageDecrypt(mBytes);

        default:
            return null;

        }
    }

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
     * @throws IllegalBlockSizeException if the plaintext or ciphertext size is too large.
     * @throws BadPaddingException if the ciphertext is invalid.
     */
    public final int doFinal(byte[] input, int inOff, int inLen, byte[] output,
                             int outOff)
        throws ShortBufferException, IllegalBlockSizeException,
        BadPaddingException
    {

        if (output.length < getOutputSize(inLen))
        {
            throw new ShortBufferException("Output buffer too short.");
        }

        byte[] out = doFinal(input, inOff, inLen);
        System.arraycopy(out, 0, output, outOff, out.length);
        return out.length;
    }

    /**
     * Since asymmetric block ciphers do not support modes, this method does
     * nothing.
     *
     * @param modeName the cipher mode (unused)
     */
    protected final void setMode(String modeName)
    {
        // empty
    }

    /**
     * Since asymmetric block ciphers do not support padding, this method does
     * nothing.
     *
     * @param paddingName the name of the padding scheme (not used)
     */
    protected final void setPadding(String paddingName)
    {
        // empty
    }

    /**
     * Check if the message length plus the length of the input length can be
     * en/decrypted. This method uses the specific values
     * {@link #maxPlainTextSize} and {@link #cipherTextSize} which are set by
     * the implementations. If the input length plus the length of the internal
     * buffer is greater than {@link #maxPlainTextSize} for encryption or not
     * equal to {@link #cipherTextSize} for decryption, an
     * {@link IllegalBlockSizeException} will be thrown.
     *
     * @param inLen length of the input to check
     * @throws IllegalBlockSizeException if the input length is invalid.
     */
    protected void checkLength(int inLen)
        throws IllegalBlockSizeException
    {

        int inLength = inLen + buf.size();

        if (opMode == ENCRYPT_MODE)
        {
            if (inLength > maxPlainTextSize)
            {
                throw new IllegalBlockSizeException(
                    "The length of the plaintext (" + inLength
                        + " bytes) is not supported by "
                        + "the cipher (max. " + maxPlainTextSize
                        + " bytes).");
            }
        }
        else if (opMode == DECRYPT_MODE)
        {
            if (inLength != cipherTextSize)
            {
                throw new IllegalBlockSizeException(
                    "Illegal ciphertext length (expected " + cipherTextSize
                        + " bytes, was " + inLength + " bytes).");
            }
        }

    }

    /**
     * Initialize the AsymmetricBlockCipher with a certain key for data
     * encryption.
     *
     * @param key    the key which has to be used to encrypt data
     * @param params the algorithm parameters
     * @param sr     the source of randomness
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     * @throws InvalidAlgorithmParameterException if the given parameters are inappropriate for
     * initializing this cipher.
     */
    protected abstract void initCipherEncrypt(Key key,
                                              AlgorithmParameterSpec params, SecureRandom sr)
        throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Initialize the AsymmetricBlockCipher with a certain key for data
     * encryption.
     *
     * @param key    the key which has to be used to decrypt data
     * @param params the algorithm parameters
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws InvalidAlgorithmParameterException if the given parameters are inappropriate for
     * initializing this cipher.
     */
    protected abstract void initCipherDecrypt(Key key,
                                              AlgorithmParameterSpec params)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException;

    /**
     * Encrypt the message stored in input. The method should also perform an
     * additional length check.
     *
     * @param input the message to be encrypted (usually the message length is
     *              less than or equal to maxPlainTextSize)
     * @return the encrypted message (it has length equal to maxCipherTextSize_)
     * @throws IllegalBlockSizeException if the input is inappropriate for this cipher.
     * @throws BadPaddingException if the input format is invalid.
     */
    protected abstract byte[] messageEncrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException;

    /**
     * Decrypt the ciphertext stored in input. The method should also perform an
     * additional length check.
     *
     * @param input the ciphertext to be decrypted (the ciphertext length is
     *              less than or equal to maxCipherTextSize)
     * @return the decrypted message
     * @throws IllegalBlockSizeException if the input is inappropriate for this cipher.
     * @throws BadPaddingException if the input format is invalid.
     */
    protected abstract byte[] messageDecrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException;

}
