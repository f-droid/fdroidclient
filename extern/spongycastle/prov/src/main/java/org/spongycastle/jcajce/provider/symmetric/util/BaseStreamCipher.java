package org.spongycastle.jcajce.provider.symmetric.util;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamBlockCipher;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class BaseStreamCipher
    extends BaseWrapCipher
    implements PBE
{
    //
    // specs we can handle.
    //
    private Class[]                 availableSpecs =
                                    {
                                        RC2ParameterSpec.class,
                                        RC5ParameterSpec.class,
                                        IvParameterSpec.class,
                                        PBEParameterSpec.class
                                    };

    private StreamCipher       cipher;
    private ParametersWithIV   ivParam;

    private int                     ivLength = 0;

    private PBEParameterSpec        pbeSpec = null;
    private String                  pbeAlgorithm = null;

    protected BaseStreamCipher(
        StreamCipher engine,
        int ivLength)
    {
        cipher = engine;
        this.ivLength = ivLength;
    }

    protected BaseStreamCipher(
        BlockCipher engine,
        int ivLength)
    {
        this.ivLength = ivLength;

        cipher = new StreamBlockCipher(engine);
    }

    protected int engineGetBlockSize()
    {
        return 0;
    }

    protected byte[] engineGetIV()
    {
        return (ivParam != null) ? ivParam.getIV() : null;
    }

    protected int engineGetKeySize(
        Key     key)
    {
        return key.getEncoded().length * 8;
    }

    protected int engineGetOutputSize(
        int     inputLen)
    {
        return inputLen;
    }

    protected AlgorithmParameters engineGetParameters()
    {
        if (engineParams == null)
        {
            if (pbeSpec != null)
            {
                try
                {
                    AlgorithmParameters engineParams = AlgorithmParameters.getInstance(pbeAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
                    engineParams.init(pbeSpec);

                    return engineParams;
                }
                catch (Exception e)
                {
                    return null;
                }
            }
        }

        return engineParams;
    }

    /**
     * should never be called.
     */
    protected void engineSetMode(
        String  mode)
    {
        if (!mode.equalsIgnoreCase("ECB"))
        {
            throw new IllegalArgumentException("can't support mode " + mode);
        }
    }

    /**
     * should never be called.
     */
    protected void engineSetPadding(
        String  padding)
    throws NoSuchPaddingException
    {
        if (!padding.equalsIgnoreCase("NoPadding"))
        {
            throw new NoSuchPaddingException("Padding " + padding + " unknown.");
        }
    }

    protected void engineInit(
        int                     opmode,
        Key                     key,
        AlgorithmParameterSpec  params,
        SecureRandom            random)
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        CipherParameters        param;

        this.pbeSpec = null;
        this.pbeAlgorithm = null;

        this.engineParams = null;

        //
        // basic key check
        //
        if (!(key instanceof SecretKey))
        {
            throw new InvalidKeyException("Key for algorithm " + key.getAlgorithm() + " not suitable for symmetric enryption.");
        }

        if (key instanceof BCPBEKey)
        {
            BCPBEKey k = (BCPBEKey)key;

            if (k.getOID() != null)
            {
                pbeAlgorithm = k.getOID().getId();
            }
            else
            {
                pbeAlgorithm = k.getAlgorithm();
            }

            if (k.getParam() != null)
            {
                param = k.getParam();
                pbeSpec = new PBEParameterSpec(k.getSalt(), k.getIterationCount());
            }
            else if (params instanceof PBEParameterSpec)
            {
                param = PBE.Util.makePBEParameters(k, params, cipher.getAlgorithmName());
                pbeSpec = (PBEParameterSpec)params;
            }
            else
            {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            
            if (k.getIvSize() != 0)
            {
                ivParam = (ParametersWithIV)param;
            }
        }
        else if (params == null)
        {
            param = new KeyParameter(key.getEncoded());
        }
        else if (params instanceof IvParameterSpec)
        {
            param = new ParametersWithIV(new KeyParameter(key.getEncoded()), ((IvParameterSpec)params).getIV());
            ivParam = (ParametersWithIV)param;
        }
        else
        {
            throw new InvalidAlgorithmParameterException("unknown parameter type.");
        }

        if ((ivLength != 0) && !(param instanceof ParametersWithIV))
        {
            SecureRandom    ivRandom = random;

            if (ivRandom == null)
            {
                ivRandom = new SecureRandom();
            }

            if ((opmode == Cipher.ENCRYPT_MODE) || (opmode == Cipher.WRAP_MODE))
            {
                byte[]  iv = new byte[ivLength];

                ivRandom.nextBytes(iv);
                param = new ParametersWithIV(param, iv);
                ivParam = (ParametersWithIV)param;
            }
            else
            {
                throw new InvalidAlgorithmParameterException("no IV set when one expected");
            }
        }

        try
        {
            switch (opmode)
            {
            case Cipher.ENCRYPT_MODE:
            case Cipher.WRAP_MODE:
                cipher.init(true, param);
                break;
            case Cipher.DECRYPT_MODE:
            case Cipher.UNWRAP_MODE:
                cipher.init(false, param);
                break;
            default:
                throw new InvalidParameterException("unknown opmode " + opmode + " passed");
            }
        }
        catch (Exception e)
        {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    protected void engineInit(
        int                 opmode,
        Key                 key,
        AlgorithmParameters params,
        SecureRandom        random) 
        throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        AlgorithmParameterSpec  paramSpec = null;

        if (params != null)
        {
            for (int i = 0; i != availableSpecs.length; i++)
            {
                try
                {
                    paramSpec = params.getParameterSpec(availableSpecs[i]);
                    break;
                }
                catch (Exception e)
                {
                    continue;
                }
            }

            if (paramSpec == null)
            {
                throw new InvalidAlgorithmParameterException("can't handle parameter " + params.toString());
            }
        }

        engineInit(opmode, key, paramSpec, random);
        engineParams = params;
    }

    protected void engineInit(
        int                 opmode,
        Key                 key,
        SecureRandom        random) 
        throws InvalidKeyException
    {
        try
        {
            engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    protected byte[] engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen) 
    {
        byte[]  out = new byte[inputLen];

        cipher.processBytes(input, inputOffset, inputLen, out, 0);

        return out;
    }

    protected int engineUpdate(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset) 
        throws ShortBufferException 
    {
        try
        {
        cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);

        return inputLen;
        }
        catch (DataLengthException e)
        {
            throw new ShortBufferException(e.getMessage());
        }
    }

    protected byte[] engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen) 
    {
        if (inputLen != 0)
        {
            byte[] out = engineUpdate(input, inputOffset, inputLen);

            cipher.reset();
            
            return out;
        }

        cipher.reset();
        
        return new byte[0];
    }

    protected int engineDoFinal(
        byte[]  input,
        int     inputOffset,
        int     inputLen,
        byte[]  output,
        int     outputOffset) 
    {
        if (inputLen != 0)
        {
            cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
        }

        cipher.reset();
        
        return inputLen;
    }
}
