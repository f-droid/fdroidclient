package org.spongycastle.jcajce.provider.digest;

import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;

public class SHA256
{
    private SHA256()
    {

    }

    static public class Digest
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest()
        {
            super(new SHA256Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest d = (Digest)super.clone();
            d.digest = new SHA256Digest((SHA256Digest)digest);

            return d;
        }
    }

    public static class HashMac
        extends BaseMac
    {
        public HashMac()
        {
            super(new HMac(new SHA256Digest()));
        }
    }

    /**
     * PBEWithHmacSHA
     */
    public static class PBEWithMacKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithMacKeyFactory()
        {
            super("PBEwithHmacSHA256", null, false, PKCS12, SHA256, 256, 0);
        }
    }

    /**
     * HMACSHA256
     */
    public static class KeyGenerator
        extends BaseKeyGenerator
    {
        public KeyGenerator()
        {
            super("HMACSHA256", 256, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = SHA256.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.SHA-256", PREFIX + "$Digest");
            provider.addAlgorithm("Alg.Alias.MessageDigest.SHA256", "SHA-256");
            provider.addAlgorithm("Alg.Alias.MessageDigest." + NISTObjectIdentifiers.id_sha256, "SHA-256");

            provider.addAlgorithm("SecretKeyFactory.PBEWITHHMACSHA256", PREFIX + "$PBEWithMacKeyFactory");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHHMACSHA-256", "PBEWITHHMACSHA256");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + NISTObjectIdentifiers.id_sha256, "PBEWITHHMACSHA256");

            addHMACAlgorithm(provider, "SHA256", PREFIX + "$HashMac",  PREFIX + "$KeyGenerator");
            addHMACAlias(provider, "SHA256", PKCSObjectIdentifiers.id_hmacWithSHA256);
            addHMACAlias(provider, "SHA256", NISTObjectIdentifiers.id_sha256);
        }
    }
}
