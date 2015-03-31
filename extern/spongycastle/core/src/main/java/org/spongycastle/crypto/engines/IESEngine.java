package org.spongycastle.crypto.engines;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.spongycastle.crypto.BasicAgreement;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DerivationFunction;
import org.spongycastle.crypto.EphemeralKeyPair;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.KeyParser;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.IESParameters;
import org.spongycastle.crypto.params.IESWithCipherParameters;
import org.spongycastle.crypto.params.KDFParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.util.Pack;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;

/**
 * Support class for constructing integrated encryption ciphers
 * for doing basic message exchanges on top of key agreement ciphers.
 * Follows the description given in IEEE Std 1363a.
 */
public class IESEngine
{
    BasicAgreement agree;
    DerivationFunction kdf;
    Mac mac;
    BufferedBlockCipher cipher;
    byte[] macBuf;

    boolean forEncryption;
    CipherParameters privParam, pubParam;
    IESParameters param;

    byte[] V;
    private EphemeralKeyPairGenerator keyPairGenerator;
    private KeyParser keyParser;


    /**
     * set up for use with stream mode, where the key derivation function
     * is used to provide a stream of bytes to xor with the message.
     *
     * @param agree the key agreement used as the basis for the encryption
     * @param kdf   the key derivation function used for byte generation
     * @param mac   the message authentication code generator for the message
     */
    public IESEngine(
        BasicAgreement agree,
        DerivationFunction kdf,
        Mac mac)
    {
        this.agree = agree;
        this.kdf = kdf;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = null;
    }


    /**
     * set up for use in conjunction with a block cipher to handle the
     * message.
     *
     * @param agree  the key agreement used as the basis for the encryption
     * @param kdf    the key derivation function used for byte generation
     * @param mac    the message authentication code generator for the message
     * @param cipher the cipher to used for encrypting the message
     */
    public IESEngine(
        BasicAgreement agree,
        DerivationFunction kdf,
        Mac mac,
        BufferedBlockCipher cipher)
    {
        this.agree = agree;
        this.kdf = kdf;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = cipher;
    }


    /**
     * Initialise the encryptor.
     *
     * @param forEncryption whether or not this is encryption/decryption.
     * @param privParam     our private key parameters
     * @param pubParam      the recipient's/sender's public key parameters
     * @param param         encoding and derivation parameters.
     */
    public void init(
        boolean forEncryption,
        CipherParameters privParam,
        CipherParameters pubParam,
        CipherParameters param)
    {
        this.forEncryption = forEncryption;
        this.privParam = privParam;
        this.pubParam = pubParam;
        this.param = (IESParameters)param;
        this.V = new byte[0];
    }


    /**
     * Initialise the encryptor.
     *
     * @param publicKey      the recipient's/sender's public key parameters
     * @param params         encoding and derivation parameters.
     * @param ephemeralKeyPairGenerator             the ephemeral key pair generator to use.
     */
    public void init(AsymmetricKeyParameter publicKey, CipherParameters params, EphemeralKeyPairGenerator ephemeralKeyPairGenerator)
    {
        this.forEncryption = true;
        this.pubParam = publicKey;
        this.param = (IESParameters)params;
        this.keyPairGenerator = ephemeralKeyPairGenerator;
    }

    /**
     * Initialise the encryptor.
     *
     * @param privateKey      the recipient's private key.
     * @param params          encoding and derivation parameters.
     * @param publicKeyParser the parser for reading the ephemeral public key.
     */
    public void init(AsymmetricKeyParameter privateKey, CipherParameters params, KeyParser publicKeyParser)
    {
        this.forEncryption = false;
        this.privParam = privateKey;
        this.param = (IESParameters)params;
        this.keyParser = publicKeyParser;
    }

    public BufferedBlockCipher getCipher()
    {
        return cipher;
    }

    public Mac getMac()
    {
        return mac;
    }

    private byte[] encryptBlock(
        byte[] in,
        int inOff,
        int inLen)
        throws InvalidCipherTextException
    {
        byte[] C = null, K = null, K1 = null, K2 = null;
        int len;

        if (cipher == null)
        {
            // Streaming mode.
            K1 = new byte[inLen];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);

            if (V.length != 0)
            {
                System.arraycopy(K, 0, K2, 0, K2.length);
                System.arraycopy(K, K2.length, K1, 0, K1.length);
            }
            else
            {
                System.arraycopy(K, 0, K1, 0, K1.length);
                System.arraycopy(K, inLen, K2, 0, K2.length);
            }

            C = new byte[inLen];

            for (int i = 0; i != inLen; i++)
            {
                C[i] = (byte)(in[inOff + i] ^ K1[i]);
            }
            len = inLen;
        }
        else
        {
            // Block cipher mode.
            K1 = new byte[((IESWithCipherParameters)param).getCipherKeySize() / 8];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);
            System.arraycopy(K, 0, K1, 0, K1.length);
            System.arraycopy(K, K1.length, K2, 0, K2.length);

            cipher.init(true, new KeyParameter(K1));
            C = new byte[cipher.getOutputSize(inLen)];
            len = cipher.processBytes(in, inOff, inLen, C, 0);
            len += cipher.doFinal(C, len);
        }


        // Convert the length of the encoding vector into a byte array.
        byte[] P2 = param.getEncodingV();
        byte[] L2 = new byte[4];
        if (V.length != 0 && P2 != null)
        {
            Pack.intToBigEndian(P2.length * 8, L2, 0);
        }


        // Apply the MAC.
        byte[] T = new byte[mac.getMacSize()];

        mac.init(new KeyParameter(K2));
        mac.update(C, 0, C.length);
        if (P2 != null)
        {
            mac.update(P2, 0, P2.length);
        }
        if (V.length != 0)
        {
            mac.update(L2, 0, L2.length);
        }
        mac.doFinal(T, 0);


        // Output the triple (V,C,T).
        byte[] Output = new byte[V.length + len + T.length];
        System.arraycopy(V, 0, Output, 0, V.length);
        System.arraycopy(C, 0, Output, V.length, len);
        System.arraycopy(T, 0, Output, V.length + len, T.length);
        return Output;
    }

    private byte[] decryptBlock(
        byte[] in_enc,
        int inOff,
        int inLen)
        throws InvalidCipherTextException
    {
        byte[] M = null, K = null, K1 = null, K2 = null;
        int len;

        if (cipher == null)
        {
            // Streaming mode.
            K1 = new byte[inLen - V.length - mac.getMacSize()];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);

            if (V.length != 0)
            {
                System.arraycopy(K, 0, K2, 0, K2.length);
                System.arraycopy(K, K2.length, K1, 0, K1.length);
            }
            else
            {
                System.arraycopy(K, 0, K1, 0, K1.length);
                System.arraycopy(K, K1.length, K2, 0, K2.length);
            }

            M = new byte[K1.length];

            for (int i = 0; i != K1.length; i++)
            {
                M[i] = (byte)(in_enc[inOff + V.length + i] ^ K1[i]);
            }

            len = K1.length;
        }
        else
        {
            // Block cipher mode.        
            K1 = new byte[((IESWithCipherParameters)param).getCipherKeySize() / 8];
            K2 = new byte[param.getMacKeySize() / 8];
            K = new byte[K1.length + K2.length];

            kdf.generateBytes(K, 0, K.length);
            System.arraycopy(K, 0, K1, 0, K1.length);
            System.arraycopy(K, K1.length, K2, 0, K2.length);

            cipher.init(false, new KeyParameter(K1));

            M = new byte[cipher.getOutputSize(inLen - V.length - mac.getMacSize())];
            len = cipher.processBytes(in_enc, inOff + V.length, inLen - V.length - mac.getMacSize(), M, 0);
            len += cipher.doFinal(M, len);
        }


        // Convert the length of the encoding vector into a byte array.
        byte[] P2 = param.getEncodingV();
        byte[] L2 = new byte[4];
        if (V.length != 0 && P2 != null)
        {
            Pack.intToBigEndian(P2.length * 8, L2, 0);
        }


        // Verify the MAC.
        int end = inOff + inLen;
        byte[] T1 = Arrays.copyOfRange(in_enc, end - mac.getMacSize(), end);

        byte[] T2 = new byte[T1.length];
        mac.init(new KeyParameter(K2));
        mac.update(in_enc, inOff + V.length, inLen - V.length - T2.length);

        if (P2 != null)
        {
            mac.update(P2, 0, P2.length);
        }
        if (V.length != 0)
        {
            mac.update(L2, 0, L2.length);
        }
        mac.doFinal(T2, 0);

        if (!Arrays.constantTimeAreEqual(T1, T2))
        {
            throw new InvalidCipherTextException("Invalid MAC.");
        }


        // Output the message.
        return Arrays.copyOfRange(M, 0, len);
    }


    public byte[] processBlock(
        byte[] in,
        int inOff,
        int inLen)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            if (keyPairGenerator != null)
            {
                EphemeralKeyPair ephKeyPair = keyPairGenerator.generate();

                this.privParam = ephKeyPair.getKeyPair().getPrivate();
                this.V = ephKeyPair.getEncodedPublicKey();
            }
        }
        else
        {
            if (keyParser != null)
            {
                ByteArrayInputStream bIn = new ByteArrayInputStream(in, inOff, inLen);

                try
                {
                    this.pubParam = keyParser.readKey(bIn);
                }
                catch (IOException e)
                {
                    throw new InvalidCipherTextException("unable to recover ephemeral public key: " + e.getMessage(), e);
                }

                int encLength = (inLen - bIn.available());
                this.V = Arrays.copyOfRange(in, inOff, inOff + encLength);
            }
        }

        // Compute the common value and convert to byte array. 
        agree.init(privParam);
        BigInteger z = agree.calculateAgreement(pubParam);
        byte[] Z = BigIntegers.asUnsignedByteArray(agree.getFieldSize(), z);

        // Create input to KDF.  
        byte[] VZ;
        if (V.length != 0)
        {
            VZ = new byte[V.length + Z.length];
            System.arraycopy(V, 0, VZ, 0, V.length);
            System.arraycopy(Z, 0, VZ, V.length, Z.length);
        }
        else
        {
            VZ = Z;
        }

        // Initialise the KDF.
        KDFParameters kdfParam = new KDFParameters(VZ, param.getDerivationV());
        kdf.init(kdfParam);

        return forEncryption
            ? encryptBlock(in, inOff, inLen)
            : decryptBlock(in, inOff, inLen);
    }
}
