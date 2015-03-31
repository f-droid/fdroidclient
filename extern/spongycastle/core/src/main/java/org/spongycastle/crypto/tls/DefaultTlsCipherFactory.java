package org.spongycastle.crypto.tls;

import java.io.IOException;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.digests.MD5Digest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.engines.CamelliaEngine;
import org.spongycastle.crypto.engines.DESedeEngine;
import org.spongycastle.crypto.engines.RC4Engine;
import org.spongycastle.crypto.engines.SEEDEngine;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.modes.AEADBlockCipher;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.modes.CCMBlockCipher;
import org.spongycastle.crypto.modes.GCMBlockCipher;

public class DefaultTlsCipherFactory
    extends AbstractTlsCipherFactory
{
    public TlsCipher createCipher(TlsContext context, int encryptionAlgorithm, int macAlgorithm)
        throws IOException
    {
        switch (encryptionAlgorithm)
        {
        case EncryptionAlgorithm._3DES_EDE_CBC:
            return createDESedeCipher(context, macAlgorithm);
        case EncryptionAlgorithm.AES_128_CBC:
            return createAESCipher(context, 16, macAlgorithm);
        case EncryptionAlgorithm.AES_128_CCM:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_CCM(context, 16, 16);
        case EncryptionAlgorithm.AES_128_CCM_8:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_CCM(context, 16, 8);
        case EncryptionAlgorithm.AES_256_CCM:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_CCM(context, 32, 16);
        case EncryptionAlgorithm.AES_256_CCM_8:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_CCM(context, 32, 8);
        case EncryptionAlgorithm.AES_128_GCM:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_GCM(context, 16, 16);
        case EncryptionAlgorithm.AES_256_CBC:
            return createAESCipher(context, 32, macAlgorithm);
        case EncryptionAlgorithm.AES_256_GCM:
            // NOTE: Ignores macAlgorithm
            return createCipher_AES_GCM(context, 32, 16);
        case EncryptionAlgorithm.CAMELLIA_128_CBC:
            return createCamelliaCipher(context, 16, macAlgorithm);
        case EncryptionAlgorithm.CAMELLIA_256_CBC:
            return createCamelliaCipher(context, 32, macAlgorithm);
        case EncryptionAlgorithm.ESTREAM_SALSA20:
            return createSalsa20Cipher(context, 12, 32, macAlgorithm);
        case EncryptionAlgorithm.NULL:
            return createNullCipher(context, macAlgorithm);
        case EncryptionAlgorithm.RC4_128:
            return createRC4Cipher(context, 16, macAlgorithm);
        case EncryptionAlgorithm.SALSA20:
            return createSalsa20Cipher(context, 20, 32, macAlgorithm);
        case EncryptionAlgorithm.SEED_CBC:
            return createSEEDCipher(context, macAlgorithm);
        default:
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    protected TlsBlockCipher createAESCipher(TlsContext context, int cipherKeySize, int macAlgorithm)
        throws IOException
    {
        return new TlsBlockCipher(context, createAESBlockCipher(), createAESBlockCipher(),
            createHMACDigest(macAlgorithm), createHMACDigest(macAlgorithm), cipherKeySize);
    }

    protected TlsAEADCipher createCipher_AES_CCM(TlsContext context, int cipherKeySize, int macSize)
        throws IOException
    {
        return new TlsAEADCipher(context, createAEADBlockCipher_AES_CCM(),
            createAEADBlockCipher_AES_CCM(), cipherKeySize, macSize);
    }

    protected TlsAEADCipher createCipher_AES_GCM(TlsContext context, int cipherKeySize, int macSize)
        throws IOException
    {
        return new TlsAEADCipher(context, createAEADBlockCipher_AES_GCM(),
            createAEADBlockCipher_AES_GCM(), cipherKeySize, macSize);
    }

    protected TlsBlockCipher createCamelliaCipher(TlsContext context, int cipherKeySize, int macAlgorithm)
        throws IOException
    {
        return new TlsBlockCipher(context, createCamelliaBlockCipher(),
            createCamelliaBlockCipher(), createHMACDigest(macAlgorithm),
            createHMACDigest(macAlgorithm), cipherKeySize);
    }

    protected TlsBlockCipher createDESedeCipher(TlsContext context, int macAlgorithm)
        throws IOException
    {
        return new TlsBlockCipher(context, createDESedeBlockCipher(), createDESedeBlockCipher(),
            createHMACDigest(macAlgorithm), createHMACDigest(macAlgorithm), 24);
    }

    protected TlsNullCipher createNullCipher(TlsContext context, int macAlgorithm)
        throws IOException
    {
        return new TlsNullCipher(context, createHMACDigest(macAlgorithm),
            createHMACDigest(macAlgorithm));
    }

    protected TlsStreamCipher createRC4Cipher(TlsContext context, int cipherKeySize, int macAlgorithm)
        throws IOException
    {
        return new TlsStreamCipher(context, createRC4StreamCipher(), createRC4StreamCipher(),
            createHMACDigest(macAlgorithm), createHMACDigest(macAlgorithm), cipherKeySize);
    }

    protected TlsStreamCipher createSalsa20Cipher(TlsContext context, int rounds, int cipherKeySize, int macAlgorithm)
        throws IOException
    {
        /*
         * TODO To be able to support UMAC96, we need to give the TlsStreamCipher a Mac instead of
         * assuming HMAC and passing a digest.
         */
        return new TlsStreamCipher(context, createSalsa20StreamCipher(rounds), createSalsa20StreamCipher(rounds),
            createHMACDigest(macAlgorithm), createHMACDigest(macAlgorithm), cipherKeySize);
    }

    protected TlsBlockCipher createSEEDCipher(TlsContext context, int macAlgorithm)
        throws IOException
    {
        return new TlsBlockCipher(context, createSEEDBlockCipher(), createSEEDBlockCipher(),
            createHMACDigest(macAlgorithm), createHMACDigest(macAlgorithm), 16);
    }

    protected BlockCipher createAESBlockCipher()
    {
        return new CBCBlockCipher(new AESFastEngine());
    }

    protected AEADBlockCipher createAEADBlockCipher_AES_CCM()
    {
        return new CCMBlockCipher(new AESFastEngine());
    }

    protected AEADBlockCipher createAEADBlockCipher_AES_GCM()
    {
        // TODO Consider allowing custom configuration of multiplier
        return new GCMBlockCipher(new AESFastEngine());
    }

    protected BlockCipher createCamelliaBlockCipher()
    {
        return new CBCBlockCipher(new CamelliaEngine());
    }

    protected BlockCipher createDESedeBlockCipher()
    {
        return new CBCBlockCipher(new DESedeEngine());
    }

    protected StreamCipher createRC4StreamCipher()
    {
        return new RC4Engine();
    }

    protected StreamCipher createSalsa20StreamCipher(int rounds)
    {
        return new Salsa20Engine(rounds);
    }

    protected BlockCipher createSEEDBlockCipher()
    {
        return new CBCBlockCipher(new SEEDEngine());
    }

    protected Digest createHMACDigest(int macAlgorithm) throws IOException
    {
        switch (macAlgorithm)
        {
        case MACAlgorithm._null:
            return null;
        case MACAlgorithm.hmac_md5:
            return new MD5Digest();
        case MACAlgorithm.hmac_sha1:
            return new SHA1Digest();
        case MACAlgorithm.hmac_sha256:
            return new SHA256Digest();
        case MACAlgorithm.hmac_sha384:
            return new SHA384Digest();
        case MACAlgorithm.hmac_sha512:
            return new SHA512Digest();
        default:
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    protected Mac createMac(int macAlgorithm) throws IOException
    {
        switch (macAlgorithm)
        {
        // TODO Need an implementation of UMAC
//        case MACAlgorithm.umac96:
//            return
        default:
            return new HMac(createHMACDigest(macAlgorithm));
        }
    }
}
