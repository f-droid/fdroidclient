package org.spongycastle.jce.spec;

import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.util.Arrays;

/**
 * Parameter spec for an integrated encryptor, as in IEEE P1363a
 */
public class IESParameterSpec
    implements AlgorithmParameterSpec
{
    private byte[] derivation;
    private byte[] encoding;
    private int macKeySize;
    private int cipherKeySize;
    private byte[] nonce;


    /**
     * Set the IES engine parameters.
     *
     * @param derivation the optional derivation vector for the KDF.
     * @param encoding   the optional encoding vector for the KDF.
     * @param macKeySize the key size (in bits) for the MAC.
     */
    public IESParameterSpec(
        byte[] derivation,
        byte[] encoding,
        int macKeySize)
    {
        this(derivation, encoding, macKeySize, -1);
    }


    /**
     * Set the IES engine parameters.
     *
     * @param derivation    the optional derivation vector for the KDF.
     * @param encoding      the optional encoding vector for the KDF.
     * @param macKeySize    the key size (in bits) for the MAC.
     * @param cipherKeySize the key size (in bits) for the block cipher.
     */
    public IESParameterSpec(
        byte[] derivation,
        byte[] encoding,
        int macKeySize,
        int cipherKeySize)
    {
        this(derivation, encoding, macKeySize, cipherKeySize, null);
    }

    /**
     * Set the IES engine parameters.
     *
     * @param derivation    the optional derivation vector for the KDF.
     * @param encoding      the optional encoding vector for the KDF.
     * @param macKeySize    the key size (in bits) for the MAC.
     * @param cipherKeySize the key size (in bits) for the block cipher.
     * @param nonce         an IV to use initialising the block cipher.
     */
    public IESParameterSpec(
        byte[] derivation,
        byte[] encoding,
        int macKeySize,
        int cipherKeySize,
        byte[] nonce)
    {
        if (derivation != null)
        {
            this.derivation = new byte[derivation.length];
            System.arraycopy(derivation, 0, this.derivation, 0, derivation.length);
        }
        else
        {
            this.derivation = null;
        }

        if (encoding != null)
        {
            this.encoding = new byte[encoding.length];
            System.arraycopy(encoding, 0, this.encoding, 0, encoding.length);
        }
        else
        {
            this.encoding = null;
        }

        this.macKeySize = macKeySize;
        this.cipherKeySize = cipherKeySize;
        this.nonce = Arrays.clone(nonce);
    }

    /**
     * return the derivation vector.
     */
    public byte[] getDerivationV()
    {
        return Arrays.clone(derivation);
    }

    /**
     * return the encoding vector.
     */
    public byte[] getEncodingV()
    {
        return Arrays.clone(encoding);
    }

    /**
     * return the key size in bits for the MAC used with the message
     */
    public int getMacKeySize()
    {
        return macKeySize;
    }

    /**
     * return the key size in bits for the block cipher used with the message
     */
    public int getCipherKeySize()
    {
        return cipherKeySize;
    }

    /**
     * Return the nonce (IV) value to be associated with message.
     *
     * @return block cipher IV for message.
     */
    public byte[] getNonce()
    {
        return Arrays.clone(nonce);
    }
}
