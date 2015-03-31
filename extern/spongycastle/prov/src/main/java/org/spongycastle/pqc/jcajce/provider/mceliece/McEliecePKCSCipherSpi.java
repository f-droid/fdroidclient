package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
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
import org.spongycastle.pqc.crypto.mceliece.McElieceKeyParameters;
import org.spongycastle.pqc.crypto.mceliece.McEliecePKCSCipher;
import org.spongycastle.pqc.jcajce.provider.util.AsymmetricBlockCipher;

public class McEliecePKCSCipherSpi
    extends AsymmetricBlockCipher
    implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
    // TODO digest needed?
    private Digest digest;
    private McEliecePKCSCipher cipher;

    public McEliecePKCSCipherSpi(Digest digest, McEliecePKCSCipher cipher)
    {
        this.digest = digest;
        this.cipher = cipher;
    }

    protected void initCipherEncrypt(Key key, AlgorithmParameterSpec params,
                                     SecureRandom sr)
        throws InvalidKeyException,
        InvalidAlgorithmParameterException
    {

        CipherParameters param;
        param = McElieceKeysToParams.generatePublicKeyParameter((PublicKey)key);

        param = new ParametersWithRandom(param, sr);
        digest.reset();
        cipher.init(true, param);
        this.maxPlainTextSize = cipher.maxPlainTextSize;
        this.cipherTextSize = cipher.cipherTextSize;
    }

    protected void initCipherDecrypt(Key key, AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        CipherParameters param;
        param = McElieceKeysToParams.generatePrivateKeyParameter((PrivateKey)key);

        digest.reset();
        cipher.init(false, param);
        this.maxPlainTextSize = cipher.maxPlainTextSize;
        this.cipherTextSize = cipher.cipherTextSize;
    }

    protected byte[] messageEncrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException
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

    protected byte[] messageDecrypt(byte[] input)
        throws IllegalBlockSizeException, BadPaddingException
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

    public String getName()
    {
        return "McEliecePKCS";
    }

    public int getKeySize(Key key)
        throws InvalidKeyException
    {
        McElieceKeyParameters mcElieceKeyParameters;
        if (key instanceof PublicKey)
        {
            mcElieceKeyParameters = (McElieceKeyParameters)McElieceKeysToParams.generatePublicKeyParameter((PublicKey)key);
        }
        else
        {
            mcElieceKeyParameters = (McElieceKeyParameters)McElieceKeysToParams.generatePrivateKeyParameter((PrivateKey)key);

        }


        return cipher.getKeySize(mcElieceKeyParameters);
    }

    //////////////////////////////////////////////////////////////////////////////////

    static public class McEliecePKCS
        extends McEliecePKCSCipherSpi
    {
        public McEliecePKCS()
        {
            super(new SHA1Digest(), new McEliecePKCSCipher());
        }
    }

    static public class McEliecePKCS224
        extends McEliecePKCSCipherSpi
    {
        public McEliecePKCS224()
        {
            super(new SHA224Digest(), new McEliecePKCSCipher());
        }
    }

    static public class McEliecePKCS256
        extends McEliecePKCSCipherSpi
    {
        public McEliecePKCS256()
        {
            super(new SHA256Digest(), new McEliecePKCSCipher());
        }
    }

    static public class McEliecePKCS384
        extends McEliecePKCSCipherSpi
    {
        public McEliecePKCS384()
        {
            super(new SHA384Digest(), new McEliecePKCSCipher());
        }
    }

    static public class McEliecePKCS512
        extends McEliecePKCSCipherSpi
    {
        public McEliecePKCS512()
        {
            super(new SHA512Digest(), new McEliecePKCSCipher());
        }
    }


}
