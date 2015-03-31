package org.spongycastle.jcajce.provider.asymmetric.dh;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.KeyEncoder;
import org.spongycastle.crypto.agreement.DHBasicAgreement;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.DESedeEngine;
import org.spongycastle.crypto.engines.IESEngine;
import org.spongycastle.crypto.generators.DHKeyPairGenerator;
import org.spongycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.spongycastle.crypto.generators.KDF2BytesGenerator;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.DHKeyGenerationParameters;
import org.spongycastle.crypto.params.DHKeyParameters;
import org.spongycastle.crypto.params.DHParameters;
import org.spongycastle.crypto.params.DHPublicKeyParameters;
import org.spongycastle.crypto.params.IESParameters;
import org.spongycastle.crypto.params.IESWithCipherParameters;
import org.spongycastle.crypto.parsers.DHIESPublicKeyParser;
import org.spongycastle.jcajce.provider.asymmetric.util.DHUtil;
import org.spongycastle.jcajce.provider.asymmetric.util.IESUtil;
import org.spongycastle.jce.interfaces.IESKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.IESParameterSpec;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.Strings;


public class IESCipher
    extends CipherSpi
{
    private IESEngine engine;
    private int state = -1;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private AlgorithmParameters engineParam = null;
    private IESParameterSpec engineSpec = null;
    private AsymmetricKeyParameter key;
    private SecureRandom random;
    private boolean dhaesMode = false;
    private AsymmetricKeyParameter otherKeyParameter = null;

    public IESCipher(IESEngine engine)
    {
        this.engine = engine;
    }


    public int engineGetBlockSize()
    {
        if (engine.getCipher() != null)
        {
            return engine.getCipher().getBlockSize();
        }
        else
        {
            return 0;
        }
    }


    public int engineGetKeySize(Key key)
    {
        if (key instanceof DHKey)
        {
            return ((DHKey)key).getParams().getP().bitLength();
        }
        else
        {
            throw new IllegalArgumentException("not a DH key");
        }
    }


    public byte[] engineGetIV()
    {
        return null;
    }

    public AlgorithmParameters engineGetParameters()
    {
        if (engineParam == null && engineSpec != null)
        {
            try
            {
                engineParam = AlgorithmParameters.getInstance("IES", BouncyCastleProvider.PROVIDER_NAME);
                engineParam.init(engineSpec);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.toString());
            }
        }

        return engineParam;
    }


    public void engineSetMode(String mode)
        throws NoSuchAlgorithmException
    {
        String modeName = Strings.toUpperCase(mode);

        if (modeName.equals("NONE"))
        {
            dhaesMode = false;
        }
        else if (modeName.equals("DHAES"))
        {
            dhaesMode = true;
        }
        else
        {
            throw new IllegalArgumentException("can't support mode " + mode);
        }
    }

    public int engineGetOutputSize(int inputLen)
    {
        int len1, len2, len3;

        len1 = engine.getMac().getMacSize();

        if (key != null)
        {
            len2 = ((DHKey)key).getParams().getP().bitLength() / 8 + 1;
        }
        else
        {
            throw new IllegalStateException("cipher not initialised");
        }

        if (engine.getCipher() == null)
        {
            len3 = inputLen;
        }
        else if (state == Cipher.ENCRYPT_MODE || state == Cipher.WRAP_MODE)
        {
            len3 = engine.getCipher().getOutputSize(inputLen);
        }
        else if (state == Cipher.DECRYPT_MODE || state == Cipher.UNWRAP_MODE)
        {
            len3 = engine.getCipher().getOutputSize(inputLen - len1 - len2);
        }
        else
        {
            throw new IllegalStateException("cipher not initialised");
        }

        if (state == Cipher.ENCRYPT_MODE || state == Cipher.WRAP_MODE)
        {
            return buffer.size() + len1 + len2 + len3;
        }
        else if (state == Cipher.DECRYPT_MODE || state == Cipher.UNWRAP_MODE)
        {
            return buffer.size() - len1 - len2 + len3;
        }
        else
        {
            throw new IllegalStateException("IESCipher not initialised");
        }

    }

    public void engineSetPadding(String padding)
        throws NoSuchPaddingException
    {
        String paddingName = Strings.toUpperCase(padding);

        // TDOD: make this meaningful...
        if (paddingName.equals("NOPADDING"))
        {

        }
        else if (paddingName.equals("PKCS5PADDING") || paddingName.equals("PKCS7PADDING"))
        {

        }
        else
        {
            throw new NoSuchPaddingException("padding not available with IESCipher");
        }
    }

    // Initialisation methods

    public void engineInit(
        int opmode,
        Key key,
        AlgorithmParameters params,
        SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        AlgorithmParameterSpec paramSpec = null;

        if (params != null)
        {
            try
            {
                paramSpec = params.getParameterSpec(IESParameterSpec.class);
            }
            catch (Exception e)
            {
                throw new InvalidAlgorithmParameterException("cannot recognise parameters: " + e.toString());
            }
        }

        engineParam = params;
        engineInit(opmode, key, paramSpec, random);
    }


    public void engineInit(
        int opmode,
        Key key,
        AlgorithmParameterSpec engineSpec,
        SecureRandom random)
        throws InvalidAlgorithmParameterException, InvalidKeyException
    {
        // Use default parameters (including cipher key size) if none are specified
        if (engineSpec == null)
        {
            this.engineSpec = IESUtil.guessParameterSpec(engine);
        }
        else if (engineSpec instanceof IESParameterSpec)
        {
            this.engineSpec = (IESParameterSpec)engineSpec;
        }
        else
        {
            throw new InvalidAlgorithmParameterException("must be passed IES parameters");
        }

        // Parse the recipient's key
        if (opmode == Cipher.ENCRYPT_MODE || opmode == Cipher.WRAP_MODE)
        {
            if (key instanceof DHPublicKey)
            {
                this.key = DHUtil.generatePublicKeyParameter((PublicKey)key);
            }
            else if (key instanceof IESKey)
            {
                IESKey ieKey = (IESKey)key;

                this.key = DHUtil.generatePublicKeyParameter(ieKey.getPublic());
                this.otherKeyParameter = DHUtil.generatePrivateKeyParameter(ieKey.getPrivate());
            }
            else
            {
                throw new InvalidKeyException("must be passed recipient's public DH key for encryption");
            }
        }
        else if (opmode == Cipher.DECRYPT_MODE || opmode == Cipher.UNWRAP_MODE)
        {
            if (key instanceof DHPrivateKey)
            {
                this.key = DHUtil.generatePrivateKeyParameter((PrivateKey)key);
            }
            else if (key instanceof IESKey)
            {
                IESKey ieKey = (IESKey)key;

                this.otherKeyParameter = DHUtil.generatePublicKeyParameter(ieKey.getPublic());
                this.key = DHUtil.generatePrivateKeyParameter(ieKey.getPrivate());
            }
            else
            {
                throw new InvalidKeyException("must be passed recipient's private DH key for decryption");
            }
        }
        else
        {
            throw new InvalidKeyException("must be passed EC key");
        }

        this.random = random;
        this.state = opmode;
        buffer.reset();

    }


    public void engineInit(
        int opmode,
        Key key,
        SecureRandom random)
        throws InvalidKeyException
    {
        try
        {
            engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new IllegalArgumentException("can't handle supplied parameter spec");
        }

    }


    // Update methods - buffer the input

    public byte[] engineUpdate(
        byte[] input,
        int inputOffset,
        int inputLen)
    {
        buffer.write(input, inputOffset, inputLen);
        return null;
    }


    public int engineUpdate(
        byte[] input,
        int inputOffset,
        int inputLen,
        byte[] output,
        int outputOffset)
    {
        buffer.write(input, inputOffset, inputLen);
        return 0;
    }


    // Finalisation methods

    public byte[] engineDoFinal(
        byte[] input,
        int inputOffset,
        int inputLen)
        throws IllegalBlockSizeException, BadPaddingException
    {
        if (inputLen != 0)
        {
            buffer.write(input, inputOffset, inputLen);
        }

        byte[] in = buffer.toByteArray();
        buffer.reset();

        // Convert parameters for use in IESEngine
        IESParameters params = new IESWithCipherParameters(engineSpec.getDerivationV(),
            engineSpec.getEncodingV(),
            engineSpec.getMacKeySize(),
            engineSpec.getCipherKeySize());

        DHParameters dhParams = ((DHKeyParameters)key).getParameters();

        byte[] V;
        if (otherKeyParameter != null)
        {
            try
            {
                if (state == Cipher.ENCRYPT_MODE || state == Cipher.WRAP_MODE)
                {
                    engine.init(true, otherKeyParameter, key, params);
                }
                else
                {
                    engine.init(false, key, otherKeyParameter, params);
                }
                return engine.processBlock(in, 0, in.length);
            }
            catch (Exception e)
            {
                throw new BadPaddingException(e.getMessage());
            }
        }

        if (state == Cipher.ENCRYPT_MODE || state == Cipher.WRAP_MODE)
        {
            // Generate the ephemeral key pair
            DHKeyPairGenerator gen = new DHKeyPairGenerator();
            gen.init(new DHKeyGenerationParameters(random, dhParams));

            EphemeralKeyPairGenerator kGen = new EphemeralKeyPairGenerator(gen, new KeyEncoder()
            {
                public byte[] getEncoded(AsymmetricKeyParameter keyParameter)
                {
                    byte[] Vloc = new byte[(((DHKeyParameters)keyParameter).getParameters().getP().bitLength() + 7) / 8];
                    byte[] Vtmp = BigIntegers.asUnsignedByteArray(((DHPublicKeyParameters)keyParameter).getY());

                    if (Vtmp.length > Vloc.length)
                    {
                        throw new IllegalArgumentException("Senders's public key longer than expected.");
                    }
                    else
                    {
                        System.arraycopy(Vtmp, 0, Vloc, Vloc.length - Vtmp.length, Vtmp.length);
                    }

                    return Vloc;
                }
            });

            // Encrypt the buffer
            try
            {
                engine.init(key, params, kGen);

                return engine.processBlock(in, 0, in.length);
            }
            catch (Exception e)
            {
                throw new BadPaddingException(e.getMessage());
            }
        }
        else if (state == Cipher.DECRYPT_MODE || state == Cipher.UNWRAP_MODE)
        {
            // Decrypt the buffer
            try
            {
                engine.init(key, params, new DHIESPublicKeyParser(((DHKeyParameters)key).getParameters()));

                return engine.processBlock(in, 0, in.length);
            }
            catch (InvalidCipherTextException e)
            {
                throw new BadPaddingException(e.getMessage());
            }
        }
        else
        {
            throw new IllegalStateException("IESCipher not initialised");
        }

    }


    public int engineDoFinal(
        byte[] input,
        int inputOffset,
        int inputLength,
        byte[] output,
        int outputOffset)
        throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
    {

        byte[] buf = engineDoFinal(input, inputOffset, inputLength);
        System.arraycopy(buf, 0, output, outputOffset, buf.length);
        return buf.length;

    }


    /**
     * Classes that inherit from us
     */

    static public class IES
        extends IESCipher
    {
        public IES()
        {
            super(new IESEngine(new DHBasicAgreement(),
                new KDF2BytesGenerator(new SHA1Digest()),
                new HMac(new SHA1Digest())));
        }
    }

    static public class IESwithDESede
        extends IESCipher
    {
        public IESwithDESede()
        {
            super(new IESEngine(new DHBasicAgreement(),
                new KDF2BytesGenerator(new SHA1Digest()),
                new HMac(new SHA1Digest()),
                new PaddedBufferedBlockCipher(new DESedeEngine())));
        }
    }

    static public class IESwithAES
        extends IESCipher
    {
        public IESwithAES()
        {
            super(new IESEngine(new DHBasicAgreement(),
                new KDF2BytesGenerator(new SHA1Digest()),
                new HMac(new SHA1Digest()),
                new PaddedBufferedBlockCipher(new AESEngine())));
        }
    }
}
