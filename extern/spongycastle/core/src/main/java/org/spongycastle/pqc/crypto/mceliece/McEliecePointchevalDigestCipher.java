package org.spongycastle.pqc.crypto.mceliece;


import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.pqc.crypto.MessageEncryptor;

// TODO should implement some interface?
public class McEliecePointchevalDigestCipher
{

    private final Digest messDigest;

    private final MessageEncryptor mcElieceCCA2Cipher;

    private boolean forEncrypting;


    public McEliecePointchevalDigestCipher(MessageEncryptor mcElieceCCA2Cipher, Digest messDigest)
    {
        this.mcElieceCCA2Cipher = mcElieceCCA2Cipher;
        this.messDigest = messDigest;
    }


    public void init(boolean forEncrypting,
                     CipherParameters param)
    {

        this.forEncrypting = forEncrypting;
        AsymmetricKeyParameter k;

        if (param instanceof ParametersWithRandom)
        {
            k = (AsymmetricKeyParameter)((ParametersWithRandom)param).getParameters();
        }
        else
        {
            k = (AsymmetricKeyParameter)param;
        }

        if (forEncrypting && k.isPrivate())
        {
            throw new IllegalArgumentException("Encrypting Requires Public Key.");
        }

        if (!forEncrypting && !k.isPrivate())
        {
            throw new IllegalArgumentException("Decrypting Requires Private Key.");
        }

        reset();

        mcElieceCCA2Cipher.init(forEncrypting, param);
    }


    public byte[] messageEncrypt()
    {
        if (!forEncrypting)
        {
            throw new IllegalStateException("McEliecePointchevalDigestCipher not initialised for encrypting.");
        }

        byte[] hash = new byte[messDigest.getDigestSize()];
        messDigest.doFinal(hash, 0);
        byte[] enc = null;

        try
        {
            enc = mcElieceCCA2Cipher.messageEncrypt(hash);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        return enc;
    }


    public byte[] messageDecrypt(byte[] ciphertext)
    {
        byte[] output = null;
        if (forEncrypting)
        {
            throw new IllegalStateException("McEliecePointchevalDigestCipher not initialised for decrypting.");
        }


        try
        {
            output = mcElieceCCA2Cipher.messageDecrypt(ciphertext);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        return output;
    }


    public void update(byte b)
    {
        messDigest.update(b);

    }

    public void update(byte[] in, int off, int len)
    {
        messDigest.update(in, off, len);

    }


    public void reset()
    {
        messDigest.reset();

    }


}
