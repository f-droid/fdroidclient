package org.spongycastle.pqc.jcajce.provider.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;

/**
 * The AsymmetricHybridCipher class extends CipherSpiExt.
 * NOTE: Some Ciphers are using Padding. OneAndZeroesPadding is used as default
 * padding. However padding can still be specified, but mode is not supported;
 * if you try to instantiate the cipher with something else than "NONE" as mode,
 * NoSuchAlgorithmException is thrown.
 */
public abstract class AsymmetricHybridCipher
    extends CipherSpiExt
{

    /**
     * ParameterSpec used with this cipher
     */
    protected AlgorithmParameterSpec paramSpec;

    /**
     * Since asymmetric hybrid ciphers do not support modes, this method does
     * nothing.
     *
     * @param modeName the cipher mode (unused)
     */
    protected final void setMode(String modeName)
    {
        // empty
    }

    /**
     * Since asymmetric hybrid ciphers do not support padding, this method does
     * nothing.
     *
     * @param paddingName the name of the padding scheme (not used)
     */
    protected final void setPadding(String paddingName)
    {
        // empty
    }

    /**
     * @return <tt>null</tt> since no initialization vector is used.
     */
    public final byte[] getIV()
    {
        return null;
    }

    /**
     * @return 0 since the implementing algorithms are not block ciphers
     */
    public final int getBlockSize()
    {
        return 0;
    }

    /**
     * Return the parameters used with this cipher.
     * <p/>
     * The returned parameters may be the same that were used to initialize this
     * cipher, or may contain the default set of parameters or a set of randomly
     * generated parameters used by the underlying cipher implementation
     * (provided that the underlying cipher implementation uses a default set of
     * parameters or creates new parameters if it needs parameters but was not
     * initialized with any).
     *
     * @return the parameters used with this cipher, or <tt>null</tt> if this
     *         cipher does not use any parameters.
     */
    public final AlgorithmParameterSpec getParameters()
    {
        return paramSpec;
    }

    /**
     * Return the length in bytes that an output buffer would need to be in
     * order to hold the result of the next update or doFinal operation, given
     * the input length <tt>inLen</tt> (in bytes). This call takes into
     * account any unprocessed (buffered) data from a previous update call, and
     * padding. The actual output length of the next update() or doFinal() call
     * may be smaller than the length returned by this method.
     *
     * @param inLen the length of the input
     * @return the length of the output of the next <tt>update()</tt> or
     *         <tt>doFinal()</tt> call
     */
    public final int getOutputSize(int inLen)
    {
        return opMode == ENCRYPT_MODE ? encryptOutputSize(inLen)
            : decryptOutputSize(inLen);
    }

    /**
     * Initialize the cipher for encryption by forwarding it to
     * {@link #initEncrypt(Key, AlgorithmParameterSpec, SecureRandom)}.
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using {@link #getParameters()}.
     *
     * @param key the encryption key
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     * @throws InvalidParameterException if this cipher needs algorithm parameters for
     * initialization and cannot generate parameters itself.
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
     * {@link #initEncrypt(Key, AlgorithmParameterSpec, SecureRandom)}.
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using {@link #getParameters()}.
     *
     * @param key    the encryption key
     * @param random the source of randomness
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     * @throws InvalidParameterException if this cipher needs algorithm parameters for
     * initialization and cannot generate parameters itself.
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
     * Initialize the cipher for encryption by forwarding it to initEncrypt(Key,
     * FlexiSecureRandom, AlgorithmParameterSpec).
     *
     * @param key    the encryption key
     * @param params the algorithm parameters
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher.
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is initialized with
     * <tt>null</tt> parameters and cannot generate parameters
     * itself.
     */
    public final void initEncrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        initEncrypt(key, params, new SecureRandom());
    }

    /**
     * Initialize the cipher with a certain key for data encryption.
     * <p/>
     * If this cipher requires any random bytes (e.g., for parameter
     * generation), it will get them from <tt>random</tt>.
     * <p/>
     * Note that when a Cipher object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it.
     *
     * @param key    the encryption key
     * @param random the source of randomness
     * @param params the algorithm parameters
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is initialized with
     * <tt>null</tt> parameters and cannot generate parameters
     * itself.
     */
    public final void initEncrypt(Key key, AlgorithmParameterSpec params,
                                  SecureRandom random)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException
    {
        opMode = ENCRYPT_MODE;
        initCipherEncrypt(key, params, random);
    }

    /**
     * Initialize the cipher for decryption by forwarding it to initDecrypt(Key,
     * FlexiSecureRandom).
     * <p/>
     * If this cipher requires any algorithm parameters that cannot be derived
     * from the given key, the underlying cipher implementation is supposed to
     * generate the required parameters itself (using provider-specific default
     * or random values) if it is being initialized for encryption, and raise an
     * InvalidKeyException if it is being initialized for decryption. The
     * generated parameters can be retrieved using {@link #getParameters()}.
     *
     * @param key the decryption key
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
     * Initialize the cipher with a certain key for data decryption.
     * <p/>
     * If this cipher requires any random bytes (e.g., for parameter
     * generation), it will get them from <tt>random</tt>.
     * <p/>
     * Note that when a Cipher object is initialized, it loses all
     * previously-acquired state. In other words, initializing a Cipher is
     * equivalent to creating a new instance of that Cipher and initializing it
     *
     * @param key    the decryption key
     * @param params the algorithm parameters
     * @throws InvalidKeyException if the given key is inappropriate for initializing this
     * cipher
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for
     * this cipher, or if this cipher is initialized with
     * <tt>null</tt> parameters and cannot generate parameters
     * itself.
     */
    public final void initDecrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        opMode = DECRYPT_MODE;
        initCipherDecrypt(key, params);
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
    public final int update(byte[] input, int inOff, int inLen, byte[] output,
                            int outOff)
        throws ShortBufferException
    {
        if (output.length < getOutputSize(inLen))
        {
            throw new ShortBufferException("output");
        }
        byte[] out = update(input, inOff, inLen);
        System.arraycopy(out, 0, output, outOff, out.length);
        return out.length;
    }

    /**
     * Finish a multiple-part encryption or decryption operation (depending on
     * how this cipher was initialized).
     *
     * @param input the input buffer
     * @param inOff the offset where the input starts
     * @param inLen the input length
     * @return a new buffer with the result
     * @throws BadPaddingException if the ciphertext is invalid.
     */
    public abstract byte[] doFinal(byte[] input, int inOff, int inLen)
        throws BadPaddingException;

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
     * @throws BadPaddingException if the ciphertext is invalid.
     */
    public final int doFinal(byte[] input, int inOff, int inLen, byte[] output,
                             int outOff)
        throws ShortBufferException, BadPaddingException
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
     * Compute the output size of an update() or doFinal() operation of a hybrid
     * asymmetric cipher in encryption mode when given input of the specified
     * length.
     *
     * @param inLen the length of the input
     * @return the output size
     */
    protected abstract int encryptOutputSize(int inLen);

    /**
     * Compute the output size of an update() or doFinal() operation of a hybrid
     * asymmetric cipher in decryption mode when given input of the specified
     * length.
     *
     * @param inLen the length of the input
     * @return the output size
     */
    protected abstract int decryptOutputSize(int inLen);

    /**
     * Initialize the AsymmetricHybridCipher with a certain key for data
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
     * Initialize the AsymmetricHybridCipher with a certain key for data
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

}
