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
import org.spongycastle.pqc.crypto.mceliece.McEliecePointchevalCipher;
import org.spongycastle.pqc.jcajce.provider.util.AsymmetricHybridCipher;

public class McEliecePointchevalCipherSpi
    extends AsymmetricHybridCipher
    implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
    // TODO digest needed?
    private Digest digest;
    private McEliecePointchevalCipher cipher;

    /**
     * buffer to store the input data
     */
    private ByteArrayOutputStream buf = new ByteArrayOutputStream();


    protected McEliecePointchevalCipherSpi(Digest digest, McEliecePointchevalCipher cipher)
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
     * @throws BadPaddingException on deryption errors.
     */
    public byte[] doFinal(byte[] input, int inOff, int inLen)
        throws BadPaddingException
    {
        update(input, inOff, inLen);
        byte[] data = buf.toByteArray();
        buf.reset();
        if (opMode == ENCRYPT_MODE)
        {

            try
            {
                return cipher.messageEncrypt(data);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
        else if (opMode == DECRYPT_MODE)
        {

            try
            {
                return cipher.messageDecrypt(data);
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
        CipherParameters param;
        param = McElieceCCA2KeysToParams.generatePublicKeyParameter((PublicKey)key);

        param = new ParametersWithRandom(param, sr);
        digest.reset();
        cipher.init(true, param);
    }

    protected void initCipherDecrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        CipherParameters param;
        param = McElieceCCA2KeysToParams.generatePrivateKeyParameter((PrivateKey)key);

        digest.reset();
        cipher.init(false, param);
    }

    public String getName()
    {
        return "McEliecePointchevalCipher";
    }


    public int getKeySize(Key key)
        throws InvalidKeyException
    {
        McElieceCCA2KeyParameters mcElieceCCA2KeyParameters;
        if (key instanceof PublicKey)
        {
            mcElieceCCA2KeyParameters = (McElieceCCA2KeyParameters)McElieceCCA2KeysToParams.generatePublicKeyParameter((PublicKey)key);
        }
        else
        {
            mcElieceCCA2KeyParameters = (McElieceCCA2KeyParameters)McElieceCCA2KeysToParams.generatePrivateKeyParameter((PrivateKey)key);
        }

        return cipher.getKeySize(mcElieceCCA2KeyParameters);
    }

    public byte[] messageEncrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException
    {
        byte[] output = null;
        try
        {
            output = cipher.messageEncrypt(input);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return output;
    }


    public byte[] messageDecrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException
    {
        byte[] output = null;
        try
        {
            output = cipher.messageDecrypt(input);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return output;
    }


    //////////////////////////////////////////////////////////////////////////////////77

    static public class McEliecePointcheval
        extends McEliecePointchevalCipherSpi
    {
        public McEliecePointcheval()
        {
            super(new SHA1Digest(), new McEliecePointchevalCipher());
        }
    }

    static public class McEliecePointcheval224
        extends McEliecePointchevalCipherSpi
    {
        public McEliecePointcheval224()
        {
            super(new SHA224Digest(), new McEliecePointchevalCipher());
        }
    }

    static public class McEliecePointcheval256
        extends McEliecePointchevalCipherSpi
    {
        public McEliecePointcheval256()
        {
            super(new SHA256Digest(), new McEliecePointchevalCipher());
        }
    }

    static public class McEliecePointcheval384
        extends McEliecePointchevalCipherSpi
    {
        public McEliecePointcheval384()
        {
            super(new SHA384Digest(), new McEliecePointchevalCipher());
        }
    }

    static public class McEliecePointcheval512
        extends McEliecePointchevalCipherSpi
    {
        public McEliecePointcheval512()
        {
            super(new SHA512Digest(), new McEliecePointchevalCipher());
        }
    }


}
