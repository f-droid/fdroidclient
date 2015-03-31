package org.spongycastle.jcajce.provider.digest;

import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.macs.OldHMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;

public class SHA384
{
    private SHA384()
    {

    }

    static public class Digest
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest()
        {
            super(new SHA384Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest d = (Digest)super.clone();
            d.digest = new SHA384Digest((SHA384Digest)digest);

            return d;
        }
    }

    public static class HashMac
        extends BaseMac
    {
        public HashMac()
        {
            super(new HMac(new SHA384Digest()));
        }
    }

    /**
     * HMACSHA384
     */
    public static class KeyGenerator
        extends BaseKeyGenerator
    {
        public KeyGenerator()
        {
            super("HMACSHA384", 384, new CipherKeyGenerator());
        }
    }

    public static class OldSHA384
        extends BaseMac
    {
        public OldSHA384()
        {
            super(new OldHMac(new SHA384Digest()));
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = SHA384.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.SHA-384", PREFIX + "$Digest");
            provider.addAlgorithm("Alg.Alias.MessageDigest.SHA384", "SHA-384");
            provider.addAlgorithm("Alg.Alias.MessageDigest." + NISTObjectIdentifiers.id_sha384, "SHA-384");
            provider.addAlgorithm("Mac.OLDHMACSHA384", PREFIX + "$OldSHA384");

            addHMACAlgorithm(provider, "SHA384", PREFIX + "$HashMac",  PREFIX + "$KeyGenerator");
            addHMACAlias(provider, "SHA384", PKCSObjectIdentifiers.id_hmacWithSHA384);
        }
    }
}
