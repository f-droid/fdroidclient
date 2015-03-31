package org.spongycastle.crypto.tls;

import java.io.IOException;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.encodings.PKCS1Encoding;
import org.spongycastle.crypto.engines.RSABlindedEngine;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.params.RSAKeyParameters;

public class DefaultTlsEncryptionCredentials
    extends AbstractTlsEncryptionCredentials
{
    protected TlsContext context;
    protected Certificate certificate;
    protected AsymmetricKeyParameter privateKey;

    public DefaultTlsEncryptionCredentials(TlsContext context, Certificate certificate,
        AsymmetricKeyParameter privateKey)
    {
        if (certificate == null)
        {
            throw new IllegalArgumentException("'certificate' cannot be null");
        }
        if (certificate.isEmpty())
        {
            throw new IllegalArgumentException("'certificate' cannot be empty");
        }
        if (privateKey == null)
        {
            throw new IllegalArgumentException("'privateKey' cannot be null");
        }
        if (!privateKey.isPrivate())
        {
            throw new IllegalArgumentException("'privateKey' must be private");
        }

        if (privateKey instanceof RSAKeyParameters)
        {
        }
        else
        {
            throw new IllegalArgumentException("'privateKey' type not supported: "
                + privateKey.getClass().getName());
        }

        this.context = context;
        this.certificate = certificate;
        this.privateKey = privateKey;
    }

    public Certificate getCertificate()
    {
        return certificate;
    }

    public byte[] decryptPreMasterSecret(byte[] encryptedPreMasterSecret)
        throws IOException
    {

        PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine());
        encoding.init(false, new ParametersWithRandom(this.privateKey, context.getSecureRandom()));

        try
        {
            return encoding.processBlock(encryptedPreMasterSecret, 0,
                encryptedPreMasterSecret.length);
        }
        catch (InvalidCipherTextException e)
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }
}
