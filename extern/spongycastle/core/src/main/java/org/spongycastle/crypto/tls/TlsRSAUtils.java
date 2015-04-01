package org.spongycastle.crypto.tls;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.encodings.PKCS1Encoding;
import org.spongycastle.crypto.engines.RSABlindedEngine;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.util.Arrays;

public class TlsRSAUtils
{
    public static byte[] generateEncryptedPreMasterSecret(TlsContext context, RSAKeyParameters rsaServerPublicKey,
        OutputStream output) throws IOException
    {
        /*
         * Choose a PremasterSecret and send it encrypted to the server
         */
        byte[] premasterSecret = new byte[48];
        context.getSecureRandom().nextBytes(premasterSecret);
        TlsUtils.writeVersion(context.getClientVersion(), premasterSecret, 0);

        PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine());
        encoding.init(true, new ParametersWithRandom(rsaServerPublicKey, context.getSecureRandom()));

        try
        {
            byte[] encryptedPreMasterSecret = encoding.processBlock(premasterSecret, 0, premasterSecret.length);

            if (TlsUtils.isSSL(context))
            {
                // TODO Do any SSLv3 servers actually expect the length?
                output.write(encryptedPreMasterSecret);
            }
            else
            {
                TlsUtils.writeOpaque16(encryptedPreMasterSecret, output);
            }
        }
        catch (InvalidCipherTextException e)
        {
            /*
             * This should never happen, only during decryption.
             */
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        return premasterSecret;
    }

    /**
     * @deprecated {@link TlsEncryptionCredentials#decryptPreMasterSecret(byte[])} is expected to decrypt safely
     */
    public static byte[] safeDecryptPreMasterSecret(TlsContext context, TlsEncryptionCredentials encryptionCredentials,
        byte[] encryptedPreMasterSecret) throws IOException
    {
        return encryptionCredentials.decryptPreMasterSecret(encryptedPreMasterSecret);
    }

    public static byte[] safeDecryptPreMasterSecret(TlsContext context, RSAKeyParameters rsaServerPrivateKey,
        byte[] encryptedPreMasterSecret)
    {
        /*
         * RFC 5246 7.4.7.1.
         */
        ProtocolVersion clientVersion = context.getClientVersion();

        // TODO Provide as configuration option?
        boolean versionNumberCheckDisabled = false;

        /*
         * Generate 48 random bytes we can use as a Pre-Master-Secret, if the
         * PKCS1 padding check should fail.
         */
        byte[] fallback = new byte[48];
        context.getSecureRandom().nextBytes(fallback);

        byte[] M = Arrays.clone(fallback);
        try
        {
            PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine(), fallback);
            encoding.init(false,
                new ParametersWithRandom(rsaServerPrivateKey, context.getSecureRandom()));

            M = encoding.processBlock(encryptedPreMasterSecret, 0, encryptedPreMasterSecret.length);
        }
        catch (Exception e)
        {
            /*
             * This should never happen since the decryption should never throw an exception
             * and return a random value instead.
             *
             * In any case, a TLS server MUST NOT generate an alert if processing an
             * RSA-encrypted premaster secret message fails, or the version number is not as
             * expected. Instead, it MUST continue the handshake with a randomly generated
             * premaster secret.
             */
        }

        /*
         * If ClientHello.client_version is TLS 1.1 or higher, server implementations MUST
         * check the version number [..].
         */
        if (versionNumberCheckDisabled && clientVersion.isEqualOrEarlierVersionOf(ProtocolVersion.TLSv10))
        {
            /*
             * If the version number is TLS 1.0 or earlier, server
             * implementations SHOULD check the version number, but MAY have a
             * configuration option to disable the check.
             *
             * So there is nothing to do here.
             */
        }
        else
        {
            /*
             * OK, we need to compare the version number in the decrypted Pre-Master-Secret with the
             * clientVersion received during the handshake. If they don't match, we replace the
             * decrypted Pre-Master-Secret with a random one.
             */
            int correct = (clientVersion.getMajorVersion() ^ (M[0] & 0xff))
                | (clientVersion.getMinorVersion() ^ (M[1] & 0xff));
            correct |= correct >> 1;
            correct |= correct >> 2;
            correct |= correct >> 4;
            int mask = ~((correct & 1) - 1);

            /*
             * mask will be all bits set to 0xff if the version number differed.
             */
            for (int i = 0; i < 48; i++)
            {
                M[i] = (byte)((M[i] & (~mask)) | (fallback[i] & mask));
            }
        }
        return M;
    }
}
