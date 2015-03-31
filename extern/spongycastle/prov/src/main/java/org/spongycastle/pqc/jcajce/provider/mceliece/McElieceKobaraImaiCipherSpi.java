package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA224Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2KeyParameters;
import org.spongycastle.pqc.crypto.mceliece.McElieceKobaraImaiCipher;
import org.spongycastle.pqc.jcajce.provider.util.AsymmetricHybridCipher;

public class McElieceKobaraImaiCipherSpi
    extends AsymmetricHybridCipher
    implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{

    // TODO digest needed?
    private Digest digest;
    private McElieceKobaraImaiCipher cipher;

    /**
     * buffer to store the input data
     */
    private ByteArrayOutputStream buf = new ByteArrayOutputStream();


    public McElieceKobaraImaiCipherSpi()
    {
        buf = new ByteArrayOutputStream();
    }

    protected McElieceKobaraImaiCipherSpi(Digest digest, McElieceKobaraImaiCipher cipher)
    {
        this.digest = digest;
        this.cipher = cipher;
        buf = new ByteArrayOutputStream();
    }

    /**
     * Continue a multiple-part encryption or decryption operation.
     *
     * @param input byte array containing the next part of the input
     * @param inOff index in the array where the input starts
     * @param inLen length of the input
     * @return the processed byte array.
     */
    public byte[] update(byte[] input, int inOff, int inLen)
    {
        buf.write(input, inOff, inLen);
        return new byte[0];
    }


    /**
     * Encrypts or decrypts data in a single-part operation, or finishes a
     * multiple-part operation. The data is encrypted or decrypted, depending on
     * how this cipher was initialized.
     *
     * @param input the input buffer
     * @param inOff the offset in input where the input starts
     * @param inLen the input length
     * @return the new buffer with the result
     * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has
     * been requested, but the decrypted data is not bounded by
     * the appropriate padding bytes
     */
    public byte[] doFinal(byte[] input, int inOff, int inLen)
        throws BadPaddingException
    {
        update(input, inOff, inLen);
        if (opMode == ENCRYPT_MODE)
        {

            try
            {
                return cipher.messageEncrypt(this.pad());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
        else if (opMode == DECRYPT_MODE)
        {
            byte[] inputOfDecr = buf.toByteArray();
            buf.reset();

            try
            {
                return unpad(cipher.messageDecrypt(inputOfDecr));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
        return null;
    }

    protected int encryptOutputSize(int inLen)
    {
        return 0;
    }

    protected int decryptOutputSize(int inLen)
    {
        return 0;
    }

    protected void initCipherEncrypt(Key key, AlgorithmParameterSpec params,
                                     SecureRandom sr)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException
    {

        buf.reset();
        CipherParameters param;
        param = McElieceCCA2KeysToParams.generatePublicKeyParameter((PublicKey)key);

        param = new ParametersWithRandom(param, sr);
        digest.reset();
        cipher.init(true, param);
    }

    protected void initCipherDecrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {

        buf.reset();
        CipherParameters param;
        param = McElieceCCA2KeysToParams.generatePrivateKeyParameter((PrivateKey)key);

        digest.reset();
        cipher.init(false, param);
    }

    public String getName()
    {
        return "McElieceKobaraImaiCipher";
    }

    public int getKeySize(Key key)
        throws InvalidKeyException
    {
        McElieceCCA2KeyParameters mcElieceCCA2KeyParameters;
        if (key instanceof PublicKey)
        {
            mcElieceCCA2KeyParameters = (McElieceCCA2KeyParameters)McElieceCCA2KeysToParams.generatePublicKeyParameter((PublicKey)key);
            return cipher.getKeySize(mcElieceCCA2KeyParameters);
        }
        else if (key instanceof PrivateKey)
        {
            mcElieceCCA2KeyParameters = (McElieceCCA2KeyParameters)McElieceCCA2KeysToParams.generatePrivateKeyParameter((PrivateKey)key);
            return cipher.getKeySize(mcElieceCCA2KeyParameters);
        }
        else
        {
            throw new InvalidKeyException();
        }


    }

    /**
     * Pad and return the message stored in the message buffer.
     *
     * @return the padded message
     */
    private byte[] pad()
    {
        buf.write(0x01);
        byte[] result = buf.toByteArray();
        buf.reset();
        return result;
    }

    /**
     * Unpad a message.
     *
     * @param pmBytes the padded message
     * @return the message
     * @throws BadPaddingException if the padded message is invalid.
     */
    private byte[] unpad(byte[] pmBytes)
        throws BadPaddingException
    {
        // find first non-zero byte
        int index;
        for (index = pmBytes.length - 1; index >= 0 && pmBytes[index] == 0; index--)
        {
            ;
        }

        // check if padding byte is valid
        if (pmBytes[index] != 0x01)
        {
            throw new BadPaddingException("invalid ciphertext");
        }

        // extract and return message
        byte[] mBytes = new byte[index];
        System.arraycopy(pmBytes, 0, mBytes, 0, index);
        return mBytes;
    }


    public byte[] messageEncrypt()
        throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException
    {
        byte[] output = null;
        try
        {
            output = cipher.messageEncrypt((this.pad()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return output;
    }


    public byte[] messageDecrypt()
        throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException
    {
        byte[] output = null;
        byte[] inputOfDecr = buf.toByteArray();
        buf.reset();
        try
        {
            output = unpad(cipher.messageDecrypt(inputOfDecr));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return output;
    }


    static public class McElieceKobaraImai
        extends McElieceKobaraImaiCipherSpi
    {
        public McElieceKobaraImai()
        {
            super(new SHA1Digest(), new McElieceKobaraImaiCipher());
        }
    }

    static public class McElieceKobaraImai224
        extends McElieceKobaraImaiCipherSpi
    {
        public McElieceKobaraImai224()
        {
            super(new SHA224Digest(), new McElieceKobaraImaiCipher());
        }
    }

    static public class McElieceKobaraImai256
        extends McElieceKobaraImaiCipherSpi
    {
        public McElieceKobaraImai256()
        {
            super(new SHA256Digest(), new McElieceKobaraImaiCipher());
        }
    }

    static public class McElieceKobaraImai384
        extends McElieceKobaraImaiCipherSpi
    {
        public McElieceKobaraImai384()
        {
            super(new SHA384Digest(), new McElieceKobaraImaiCipher());
        }
    }

    static public class McElieceKobaraImai512
        extends McElieceKobaraImaiCipherSpi
    {
        public McElieceKobaraImai512()
        {
            super(new SHA512Digest(), new McElieceKobaraImaiCipher());
        }
    }


}
