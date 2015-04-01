package org.spongycastle.crypto.encodings;

import java.security.SecureRandom;

import org.spongycastle.crypto.AsymmetricBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;

/**
 * this does your basic PKCS 1 v1.5 padding - whether or not you should be using this
 * depends on your application - see PKCS1 Version 2 for details.
 */
public class PKCS1Encoding
    implements AsymmetricBlockCipher
{
    /**
     * some providers fail to include the leading zero in PKCS1 encoded blocks. If you need to
     * work with one of these set the system property org.spongycastle.pkcs1.strict to false.
     * <p>
     * The system property is checked during construction of the encoding object, it is set to 
     * true by default.
     * </p>
     */
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "org.spongycastle.pkcs1.strict";
    
    private static final int HEADER_LENGTH = 10;

    private SecureRandom            random;
    private AsymmetricBlockCipher   engine;
    private boolean                 forEncryption;
    private boolean                 forPrivateKey;
    private boolean                 useStrictLength;
    private int                     pLen = -1;
    private byte[]                  fallback = null;

    /**
     * Basic constructor.
     * @param cipher
     */
    public PKCS1Encoding(
        AsymmetricBlockCipher   cipher)
    {
        this.engine = cipher;
        this.useStrictLength = useStrict();
    }   

    /**
     * Constructor for decryption with a fixed plaintext length.
     * 
     * @param cipher The cipher to use for cryptographic operation.
     * @param pLen Length of the expected plaintext.
     */
    public PKCS1Encoding(
        AsymmetricBlockCipher   cipher,
        int pLen)
    {
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.pLen = pLen;
    }

	/**
	 * Constructor for decryption with a fixed plaintext length and a fallback
	 * value that is returned, if the padding is incorrect.
	 * 
	 * @param cipher
	 *            The cipher to use for cryptographic operation.
	 * @param fallback
	 *            The fallback value, we don't to a arraycopy here.
	 */
	public PKCS1Encoding(
    	AsymmetricBlockCipher   cipher,
        byte[] fallback)
    {
    	this.engine = cipher;
    	this.useStrictLength = useStrict();
    	this.fallback = fallback;
    	this.pLen = fallback.length;
    }
        

    
    //
    // for J2ME compatibility
    //
    private boolean useStrict()
    {
        // required if security manager has been installed.
        String strict = System.getProperty(STRICT_LENGTH_ENABLED_PROPERTY);

        return strict == null || strict.equals("true");
    }

    public AsymmetricBlockCipher getUnderlyingCipher()
    {
        return engine;
    }

    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter)param;
        }

        engine.init(forEncryption, param);

        this.forPrivateKey = kParam.isPrivate();
        this.forEncryption = forEncryption;
    }

    public int getInputBlockSize()
    {
        int     baseBlockSize = engine.getInputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize - HEADER_LENGTH;
        }
        else
        {
            return baseBlockSize;
        }
    }

    public int getOutputBlockSize()
    {
        int     baseBlockSize = engine.getOutputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize;
        }
        else
        {
            return baseBlockSize - HEADER_LENGTH;
        }
    }

    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            return encodeBlock(in, inOff, inLen);
        }
        else
        {
            return decodeBlock(in, inOff, inLen);
        }
    }

    private byte[] encodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (inLen > getInputBlockSize())
        {
            throw new IllegalArgumentException("input data too large");
        }
        
        byte[]  block = new byte[engine.getInputBlockSize()];

        if (forPrivateKey)
        {
            block[0] = 0x01;                        // type code 1

            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                block[i] = (byte)0xFF;
            }
        }
        else
        {
            random.nextBytes(block);                // random fill

            block[0] = 0x02;                        // type code 2

            //
            // a zero byte marks the end of the padding, so all
            // the pad bytes must be non-zero.
            //
            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                while (block[i] == 0)
                {
                    block[i] = (byte)random.nextInt();
                }
            }
        }

        block[block.length - inLen - 1] = 0x00;       // mark the end of the padding
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);

        return engine.processBlock(block, 0, block.length);
    }
    
    /**
     * Checks if the argument is a correctly PKCS#1.5 encoded Plaintext
     * for encryption.
     * 
     * @param encoded The Plaintext.
     * @param pLen Expected length of the plaintext.
     * @return Either 0, if the encoding is correct, or -1, if it is incorrect.
     */
	private static int checkPkcs1Encoding(byte[] encoded, int pLen) {
		int correct = 0;
		/*
		 * Check if the first two bytes are 0 2
		 */
		correct |= (encoded[0] ^ 2);

		/*
		 * Now the padding check, check for no 0 byte in the padding
		 */
		int plen = encoded.length - (
				  pLen /* Lenght of the PMS */
				+  1 /* Final 0-byte before PMS */
		);

		for (int i = 1; i < plen; i++) {
			int tmp = encoded[i];
			tmp |= tmp >> 1;
			tmp |= tmp >> 2;
			tmp |= tmp >> 4;
			correct |= (tmp & 1) - 1;
		}

		/*
		 * Make sure the padding ends with a 0 byte.
		 */
		correct |= encoded[encoded.length - (pLen +1)];

		/*
		 * Return 0 or 1, depending on the result.
		 */
		correct |= correct >> 1;
		correct |= correct >> 2;
		correct |= correct >> 4;
		return ~((correct & 1) - 1);
	}
    

    /**
     * Decode PKCS#1.5 encoding, and return a random value if the padding is not correct.
     * 
     * @param in The encrypted block.
     * @param inOff Offset in the encrypted block.
     * @param inLen Length of the encrypted block.
     * //@param pLen Length of the desired output.
     * @return The plaintext without padding, or a random value if the padding was incorrect.
     * 
     * @throws InvalidCipherTextException
     */
    private byte[] decodeBlockOrRandom(byte[] in, int inOff, int inLen)
        throws InvalidCipherTextException
    {
        if (!forPrivateKey)
        {
            throw new InvalidCipherTextException("sorry, this method is only for decryption, not for signing");
        }

        byte[] block = engine.processBlock(in, inOff, inLen);
        byte[] random = null;
        if (this.fallback == null)
        {
            random = new byte[this.pLen];
            this.random.nextBytes(random);
        }
        else
        {
            random = fallback;
        }

		/*
		 * TODO: This is a potential dangerous side channel. However, you can
		 * fix this by changing the RSA engine in a way, that it will always
		 * return blocks of the same length and prepend them with 0 bytes if
		 * needed.
		 */
        if (block.length < getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block truncated");
        }

		/*
		 * TODO: Potential side channel. Fix it by making the engine always
		 * return blocks of the correct length.
		 */
        if (useStrictLength && block.length != engine.getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block incorrect size");
        }

		/*
		 * Check the padding.
		 */
        int correct = PKCS1Encoding.checkPkcs1Encoding(block, this.pLen);
		
		/*
		 * Now, to a constant time constant memory copy of the decrypted value
		 * or the random value, depending on the validity of the padding.
		 */
        byte[] result = new byte[this.pLen];
        for (int i = 0; i < this.pLen; i++)
        {
            result[i] = (byte)((block[i + (block.length - pLen)] & (~correct)) | (random[i] & correct));
        }

        return result;
    }

    /**
     * @exception InvalidCipherTextException if the decrypted block is not in PKCS1 format.
     */
    private byte[] decodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        /*
         * If the length of the expected plaintext is known, we use a constant-time decryption.
         * If the decryption fails, we return a random value.
         */
		if (this.pLen != -1) {
    		return this.decodeBlockOrRandom(in, inOff, inLen);
    	}
    	
        byte[] block = engine.processBlock(in, inOff, inLen);

        if (block.length < getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block truncated");
        }

        byte type = block[0];

        if (forPrivateKey)
        {
            if (type != 2)
            {
                throw new InvalidCipherTextException("unknown block type");
            }
        }
        else
        {
            if (type != 1)
            {
                throw new InvalidCipherTextException("unknown block type");
            }
        }

        if (useStrictLength && block.length != engine.getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block incorrect size");
        }
        
        //
        // find and extract the message block.
        //
        int start;
        
        for (start = 1; start != block.length; start++)
        {
            byte pad = block[start];
            
            if (pad == 0)
            {
                break;
            }
            if (type == 1 && pad != (byte)0xff)
            {
                throw new InvalidCipherTextException("block padding incorrect");
            }
        }

        start++;           // data should start at the next byte

        if (start > block.length || start < HEADER_LENGTH)
        {
            throw new InvalidCipherTextException("no data in block");
        }

        byte[]  result = new byte[block.length - start];

        System.arraycopy(block, start, result, 0, result.length);

        return result;
    }
}
