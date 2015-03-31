package org.spongycastle.jcajce.provider.digest;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.digests.SkeinDigest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.macs.SkeinMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;

public class Skein
{
    private Skein()
    {
    }

    public static class DigestSkein256
        extends BCMessageDigest
        implements Cloneable
    {
        public DigestSkein256(int outputSize)
        {
            super(new SkeinDigest(SkeinDigest.SKEIN_256, outputSize));
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            BCMessageDigest d = (BCMessageDigest)super.clone();
            d.digest = new SkeinDigest((SkeinDigest)digest);

            return d;
        }
    }

    public static class Digest_256_128
        extends DigestSkein256
    {
        public Digest_256_128()
        {
            super(128);
        }
    }

    public static class Digest_256_160
        extends DigestSkein256
    {
        public Digest_256_160()
        {
            super(160);
        }
    }

    public static class Digest_256_224
        extends DigestSkein256
    {
        public Digest_256_224()
        {
            super(224);
        }
    }

    public static class Digest_256_256
        extends DigestSkein256
    {
        public Digest_256_256()
        {
            super(256);
        }
    }

    public static class DigestSkein512
        extends BCMessageDigest
        implements Cloneable
    {
        public DigestSkein512(int outputSize)
        {
            super(new SkeinDigest(SkeinDigest.SKEIN_512, outputSize));
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            BCMessageDigest d = (BCMessageDigest)super.clone();
            d.digest = new SkeinDigest((SkeinDigest)digest);

            return d;
        }
    }

    public static class Digest_512_128
        extends DigestSkein512
    {
        public Digest_512_128()
        {
            super(128);
        }
    }

    public static class Digest_512_160
        extends DigestSkein512
    {
        public Digest_512_160()
        {
            super(160);
        }
    }

    public static class Digest_512_224
        extends DigestSkein512
    {
        public Digest_512_224()
        {
            super(224);
        }
    }

    public static class Digest_512_256
        extends DigestSkein512
    {
        public Digest_512_256()
        {
            super(256);
        }
    }

    public static class Digest_512_384
        extends DigestSkein512
    {
        public Digest_512_384()
        {
            super(384);
        }
    }

    public static class Digest_512_512
        extends DigestSkein512
    {
        public Digest_512_512()
        {
            super(512);
        }
    }

    public static class DigestSkein1024
        extends BCMessageDigest
        implements Cloneable
    {
        public DigestSkein1024(int outputSize)
        {
            super(new SkeinDigest(SkeinDigest.SKEIN_1024, outputSize));
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            BCMessageDigest d = (BCMessageDigest)super.clone();
            d.digest = new SkeinDigest((SkeinDigest)digest);

            return d;
        }
    }

    public static class Digest_1024_384
        extends DigestSkein1024
    {
        public Digest_1024_384()
        {
            super(384);
        }
    }

    public static class Digest_1024_512
        extends DigestSkein1024
    {
        public Digest_1024_512()
        {
            super(512);
        }
    }

    public static class Digest_1024_1024
        extends DigestSkein1024
    {
        public Digest_1024_1024()
        {
            super(1024);
        }
    }

    /**
     * Skein HMac
     */
    public static class HashMac_256_128
        extends BaseMac
    {
        public HashMac_256_128()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_256, 128)));
        }
    }

    public static class HashMac_256_160
        extends BaseMac
    {
        public HashMac_256_160()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_256, 160)));
        }
    }

    public static class HashMac_256_224
        extends BaseMac
    {
        public HashMac_256_224()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_256, 224)));
        }
    }

    public static class HashMac_256_256
        extends BaseMac
    {
        public HashMac_256_256()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_256, 256)));
        }
    }

    public static class HashMac_512_128
        extends BaseMac
    {
        public HashMac_512_128()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 128)));
        }
    }

    public static class HashMac_512_160
        extends BaseMac
    {
        public HashMac_512_160()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 160)));
        }
    }

    public static class HashMac_512_224
        extends BaseMac
    {
        public HashMac_512_224()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 224)));
        }
    }

    public static class HashMac_512_256
        extends BaseMac
    {
        public HashMac_512_256()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 256)));
        }
    }

    public static class HashMac_512_384
        extends BaseMac
    {
        public HashMac_512_384()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 384)));
        }
    }

    public static class HashMac_512_512
        extends BaseMac
    {
        public HashMac_512_512()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_512, 512)));
        }
    }

    public static class HashMac_1024_384
        extends BaseMac
    {
        public HashMac_1024_384()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_1024, 384)));
        }
    }

    public static class HashMac_1024_512
        extends BaseMac
    {
        public HashMac_1024_512()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_1024, 512)));
        }
    }

    public static class HashMac_1024_1024
        extends BaseMac
    {
        public HashMac_1024_1024()
        {
            super(new HMac(new SkeinDigest(SkeinDigest.SKEIN_1024, 1024)));
        }
    }

    public static class HMacKeyGenerator_256_128
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_256_128()
        {
            super("HMACSkein-256-128", 128, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_256_160
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_256_160()
        {
            super("HMACSkein-256-160", 160, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_256_224
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_256_224()
        {
            super("HMACSkein-256-224", 224, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_256_256
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_256_256()
        {
            super("HMACSkein-256-256", 256, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_128
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_128()
        {
            super("HMACSkein-512-128", 128, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_160
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_160()
        {
            super("HMACSkein-512-160", 160, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_224
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_224()
        {
            super("HMACSkein-512-224", 224, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_256
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_256()
        {
            super("HMACSkein-512-256", 256, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_384
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_384()
        {
            super("HMACSkein-512-384", 384, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_512_512
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_512_512()
        {
            super("HMACSkein-512-512", 512, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_1024_384
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_1024_384()
        {
            super("HMACSkein-1024-384", 384, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_1024_512
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_1024_512()
        {
            super("HMACSkein-1024-512", 512, new CipherKeyGenerator());
        }
    }

    public static class HMacKeyGenerator_1024_1024
        extends BaseKeyGenerator
    {
        public HMacKeyGenerator_1024_1024()
        {
            super("HMACSkein-1024-1024", 1024, new CipherKeyGenerator());
        }
    }

    /*
     * Skein-MAC
     */
    public static class SkeinMac_256_128
        extends BaseMac
    {
        public SkeinMac_256_128()
        {
            super(new SkeinMac(SkeinMac.SKEIN_256, 128));
        }
    }

    public static class SkeinMac_256_160
        extends BaseMac
    {
        public SkeinMac_256_160()
        {
            super(new SkeinMac(SkeinMac.SKEIN_256, 160));
        }
    }

    public static class SkeinMac_256_224
        extends BaseMac
    {
        public SkeinMac_256_224()
        {
            super(new SkeinMac(SkeinMac.SKEIN_256, 224));
        }
    }

    public static class SkeinMac_256_256
        extends BaseMac
    {
        public SkeinMac_256_256()
        {
            super(new SkeinMac(SkeinMac.SKEIN_256, 256));
        }
    }

    public static class SkeinMac_512_128
        extends BaseMac
    {
        public SkeinMac_512_128()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 128));
        }
    }

    public static class SkeinMac_512_160
        extends BaseMac
    {
        public SkeinMac_512_160()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 160));
        }
    }

    public static class SkeinMac_512_224
        extends BaseMac
    {
        public SkeinMac_512_224()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 224));
        }
    }

    public static class SkeinMac_512_256
        extends BaseMac
    {
        public SkeinMac_512_256()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 256));
        }
    }

    public static class SkeinMac_512_384
        extends BaseMac
    {
        public SkeinMac_512_384()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 384));
        }
    }

    public static class SkeinMac_512_512
        extends BaseMac
    {
        public SkeinMac_512_512()
        {
            super(new SkeinMac(SkeinMac.SKEIN_512, 512));
        }
    }

    public static class SkeinMac_1024_384
        extends BaseMac
    {
        public SkeinMac_1024_384()
        {
            super(new SkeinMac(SkeinMac.SKEIN_1024, 384));
        }
    }

    public static class SkeinMac_1024_512
        extends BaseMac
    {
        public SkeinMac_1024_512()
        {
            super(new SkeinMac(SkeinMac.SKEIN_1024, 512));
        }
    }

    public static class SkeinMac_1024_1024
        extends BaseMac
    {
        public SkeinMac_1024_1024()
        {
            super(new SkeinMac(SkeinMac.SKEIN_1024, 1024));
        }
    }

    public static class SkeinMacKeyGenerator_256_128
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_256_128()
        {
            super("Skein-MAC-256-128", 128, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_256_160
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_256_160()
        {
            super("Skein-MAC-256-160", 160, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_256_224
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_256_224()
        {
            super("Skein-MAC-256-224", 224, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_256_256
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_256_256()
        {
            super("Skein-MAC-256-256", 256, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_128
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_128()
        {
            super("Skein-MAC-512-128", 128, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_160
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_160()
        {
            super("Skein-MAC-512-160", 160, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_224
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_224()
        {
            super("Skein-MAC-512-224", 224, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_256
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_256()
        {
            super("Skein-MAC-512-256", 256, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_384
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_384()
        {
            super("Skein-MAC-512-384", 384, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_512_512
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_512_512()
        {
            super("Skein-MAC-512-512", 512, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_1024_384
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_1024_384()
        {
            super("Skein-MAC-1024-384", 384, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_1024_512
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_1024_512()
        {
            super("Skein-MAC-1024-512", 512, new CipherKeyGenerator());
        }
    }

    public static class SkeinMacKeyGenerator_1024_1024
        extends BaseKeyGenerator
    {
        public SkeinMacKeyGenerator_1024_1024()
        {
            super("Skein-MAC-1024-1024", 1024, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = Skein.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            // Skein sizes as used in "The Skein Hash Function Family 1.3"

            provider.addAlgorithm("MessageDigest.Skein-256-128", PREFIX + "$Digest_256_128");
            provider.addAlgorithm("MessageDigest.Skein-256-160", PREFIX + "$Digest_256_160");
            provider.addAlgorithm("MessageDigest.Skein-256-224", PREFIX + "$Digest_256_224");
            provider.addAlgorithm("MessageDigest.Skein-256-256", PREFIX + "$Digest_256_256");

            provider.addAlgorithm("MessageDigest.Skein-512-128", PREFIX + "$Digest_512_128");
            provider.addAlgorithm("MessageDigest.Skein-512-160", PREFIX + "$Digest_512_160");
            provider.addAlgorithm("MessageDigest.Skein-512-224", PREFIX + "$Digest_512_224");
            provider.addAlgorithm("MessageDigest.Skein-512-256", PREFIX + "$Digest_512_256");
            provider.addAlgorithm("MessageDigest.Skein-512-384", PREFIX + "$Digest_512_384");
            provider.addAlgorithm("MessageDigest.Skein-512-512", PREFIX + "$Digest_512_512");

            provider.addAlgorithm("MessageDigest.Skein-1024-384", PREFIX + "$Digest_1024_384");
            provider.addAlgorithm("MessageDigest.Skein-1024-512", PREFIX + "$Digest_1024_512");
            provider.addAlgorithm("MessageDigest.Skein-1024-1024", PREFIX + "$Digest_1024_1024");

            addHMACAlgorithm(provider, "Skein-256-128", PREFIX + "$HashMac_256_128", PREFIX + "$HMacKeyGenerator_256_128");
            addHMACAlgorithm(provider, "Skein-256-160", PREFIX + "$HashMac_256_160", PREFIX + "$HMacKeyGenerator_256_160");
            addHMACAlgorithm(provider, "Skein-256-224", PREFIX + "$HashMac_256_224", PREFIX + "$HMacKeyGenerator_256_224");
            addHMACAlgorithm(provider, "Skein-256-256", PREFIX + "$HashMac_256_256", PREFIX + "$HMacKeyGenerator_256_256");

            addHMACAlgorithm(provider, "Skein-512-128", PREFIX + "$HashMac_512_128", PREFIX + "$HMacKeyGenerator_512_128");
            addHMACAlgorithm(provider, "Skein-512-160", PREFIX + "$HashMac_512_160", PREFIX + "$HMacKeyGenerator_512_160");
            addHMACAlgorithm(provider, "Skein-512-224", PREFIX + "$HashMac_512_224", PREFIX + "$HMacKeyGenerator_512_224");
            addHMACAlgorithm(provider, "Skein-512-256", PREFIX + "$HashMac_512_256", PREFIX + "$HMacKeyGenerator_512_256");
            addHMACAlgorithm(provider, "Skein-512-384", PREFIX + "$HashMac_512_384", PREFIX + "$HMacKeyGenerator_512_384");
            addHMACAlgorithm(provider, "Skein-512-512", PREFIX + "$HashMac_512_512", PREFIX + "$HMacKeyGenerator_512_512");

            addHMACAlgorithm(provider, "Skein-1024-384", PREFIX + "$HashMac_1024_384", PREFIX + "$HMacKeyGenerator_1024_384");
            addHMACAlgorithm(provider, "Skein-1024-512", PREFIX + "$HashMac_1024_512", PREFIX + "$HMacKeyGenerator_1024_512");
            addHMACAlgorithm(provider, "Skein-1024-1024", PREFIX + "$HashMac_1024_1024", PREFIX + "$HMacKeyGenerator_1024_1024");

            addSkeinMacAlgorithm(provider, 256, 128);
            addSkeinMacAlgorithm(provider, 256, 160);
            addSkeinMacAlgorithm(provider, 256, 224);
            addSkeinMacAlgorithm(provider, 256, 256);

            addSkeinMacAlgorithm(provider, 512, 128);
            addSkeinMacAlgorithm(provider, 512, 160);
            addSkeinMacAlgorithm(provider, 512, 224);
            addSkeinMacAlgorithm(provider, 512, 256);
            addSkeinMacAlgorithm(provider, 512, 384);
            addSkeinMacAlgorithm(provider, 512, 512);

            addSkeinMacAlgorithm(provider, 1024, 384);
            addSkeinMacAlgorithm(provider, 1024, 512);
            addSkeinMacAlgorithm(provider, 1024, 1024);
        }

        private void addSkeinMacAlgorithm(ConfigurableProvider provider, int blockSize, int outputSize)
        {
            String mainName = "Skein-MAC-" + blockSize + "-" + outputSize;
            String algorithmClassName = PREFIX + "$SkeinMac_" + blockSize + "_" + outputSize;
            String keyGeneratorClassName = PREFIX + "$SkeinMacKeyGenerator_" + blockSize + "_" + outputSize;

            provider.addAlgorithm("Mac." + mainName, algorithmClassName);
            provider.addAlgorithm("Alg.Alias.Mac.Skein-MAC" + blockSize + "/" + outputSize, mainName);
            provider.addAlgorithm("KeyGenerator." + mainName, keyGeneratorClassName);
            provider.addAlgorithm("Alg.Alias.KeyGenerator.Skein-MAC" + blockSize + "/" + outputSize, mainName);
        }

    }
}
