package org.spongycastle.jcajce.provider.digest;

import org.spongycastle.asn1.iana.IANAObjectIdentifiers;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;

public class RIPEMD160
{
    private RIPEMD160()
    {

    }

    static public class Digest
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest()
        {
            super(new RIPEMD160Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest d = (Digest)super.clone();
            d.digest = new RIPEMD160Digest((RIPEMD160Digest)digest);

            return d;
        }
    }

    /**
     * RIPEMD160 HMac
     */
    public static class HashMac
        extends BaseMac
    {
        public HashMac()
        {
            super(new HMac(new RIPEMD160Digest()));
        }
    }

    public static class KeyGenerator
        extends BaseKeyGenerator
    {
        public KeyGenerator()
        {
            super("HMACRIPEMD160", 160, new CipherKeyGenerator());
        }
    }


    //
    // PKCS12 states that the same algorithm should be used
    // for the key generation as is used in the HMAC, so that
    // is what we do here.
    //

    /**
     * PBEWithHmacRIPEMD160
     */
    public static class PBEWithHmac
        extends BaseMac
    {
        public PBEWithHmac()
        {
            super(new HMac(new RIPEMD160Digest()), PKCS12, RIPEMD160, 160);
        }
    }

    /**
     * PBEWithHmacRIPEMD160
     */
    public static class PBEWithHmacKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithHmacKeyFactory()
        {
            super("PBEwithHmacRIPEMD160", null, false, PKCS12, RIPEMD160, 160, 0);
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = RIPEMD160.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.RIPEMD160", PREFIX + "$Digest");
            provider.addAlgorithm("Alg.Alias.MessageDigest." + TeleTrusTObjectIdentifiers.ripemd160, "RIPEMD160");

            addHMACAlgorithm(provider, "RIPEMD160", PREFIX + "$HashMac", PREFIX + "$KeyGenerator");
            addHMACAlias(provider, "RIPEMD160", IANAObjectIdentifiers.hmacRIPEMD160);


            provider.addAlgorithm("SecretKeyFactory.PBEWITHHMACRIPEMD160", PREFIX + "$PBEWithHmacKeyFactory");
            provider.addAlgorithm("Mac.PBEWITHHMACRIPEMD160", PREFIX + "$PBEWithHmac");
        }
    }
}
