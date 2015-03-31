package org.spongycastle.jcajce.provider.digest;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.digests.SHA3Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;

public class SHA3
{
    private SHA3()
    {

    }

    static public class DigestSHA3
        extends BCMessageDigest
        implements Cloneable
    {
        public DigestSHA3(int size)
        {
            super(new SHA3Digest(size));
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            BCMessageDigest d = (BCMessageDigest)super.clone();
            d.digest = new SHA3Digest((SHA3Digest)digest);

            return d;
        }
    }

    static public class Digest224
        extends DigestSHA3
    {
        public Digest224()
        {
            super(224);
        }
    }

    static public class Digest256
        extends DigestSHA3
    {
        public Digest256()
        {
            super(256);
        }
    }

    static public class Digest384
        extends DigestSHA3
    {
        public Digest384()
        {
            super(384);
        }
    }

    static public class Digest512
        extends DigestSHA3
    {
        public Digest512()
        {
            super(512);
        }
    }

    /**
     * SHA3 HMac
     */
    public static class HashMac224
        extends BaseMac
    {
        public HashMac224()
        {
            super(new HMac(new SHA3Digest(224)));
        }
    }

    public static class HashMac256
        extends BaseMac
    {
        public HashMac256()
        {
            super(new HMac(new SHA3Digest(256)));
        }
    }

    public static class HashMac384
        extends BaseMac
    {
        public HashMac384()
        {
            super(new HMac(new SHA3Digest(384)));
        }
    }

    public static class HashMac512
        extends BaseMac
    {
        public HashMac512()
        {
            super(new HMac(new SHA3Digest(512)));
        }
    }

    public static class KeyGenerator224
        extends BaseKeyGenerator
    {
        public KeyGenerator224()
        {
            super("HMACSHA3-224", 224, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator256
        extends BaseKeyGenerator
    {
        public KeyGenerator256()
        {
            super("HMACSHA3-256", 256, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator384
        extends BaseKeyGenerator
    {
        public KeyGenerator384()
        {
            super("HMACSHA3-384", 384, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator512
        extends BaseKeyGenerator
    {
        public KeyGenerator512()
        {
            super("HMACSHA3-512", 512, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = SHA3.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.SHA3-224", PREFIX + "$Digest224");
            provider.addAlgorithm("MessageDigest.SHA3-256", PREFIX + "$Digest256");
            provider.addAlgorithm("MessageDigest.SHA3-384", PREFIX + "$Digest384");
            provider.addAlgorithm("MessageDigest.SHA3-512", PREFIX + "$Digest512");
            // look for an object identifier (NIST???) for SHA3 family
            // provider.addAlgorithm("Alg.Alias.MessageDigest." + OIWObjectIdentifiers.idSHA3, "SHA3-224"); // *****

            addHMACAlgorithm(provider, "SHA3-224", PREFIX + "$HashMac224", PREFIX + "$KeyGenerator224");
            addHMACAlgorithm(provider, "SHA3-256", PREFIX + "$HashMac256", PREFIX + "$KeyGenerator256");
            addHMACAlgorithm(provider, "SHA3-384", PREFIX + "$HashMac384", PREFIX + "$KeyGenerator384");
            addHMACAlgorithm(provider, "SHA3-512", PREFIX + "$HashMac512", PREFIX + "$KeyGenerator512");
        }
    }
}
