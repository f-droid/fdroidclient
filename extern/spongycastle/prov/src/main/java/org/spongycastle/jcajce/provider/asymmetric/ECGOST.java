package org.spongycastle.jcajce.provider.asymmetric;

import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.jcajce.provider.asymmetric.ecgost.KeyFactorySpi;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;

public class ECGOST
{
    private static final String PREFIX = "org.spongycastle.jcajce.provider.asymmetric" + ".ecgost.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }
        
        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("KeyFactory.ECGOST3410", PREFIX + "KeyFactorySpi");
            provider.addAlgorithm("Alg.Alias.KeyFactory.GOST-3410-2001", "ECGOST3410");
            provider.addAlgorithm("Alg.Alias.KeyFactory.ECGOST-3410", "ECGOST3410");

            registerOid(provider, CryptoProObjectIdentifiers.gostR3410_2001, "ECGOST3410", new KeyFactorySpi());
            registerOidAlgorithmParameters(provider, CryptoProObjectIdentifiers.gostR3410_2001, "ECGOST3410");

            provider.addAlgorithm("KeyPairGenerator.ECGOST3410", PREFIX + "KeyPairGeneratorSpi");
            provider.addAlgorithm("Alg.Alias.KeyPairGenerator.ECGOST-3410", "ECGOST3410");
            provider.addAlgorithm("Alg.Alias.KeyPairGenerator.GOST-3410-2001", "ECGOST3410");

            provider.addAlgorithm("Signature.ECGOST3410", PREFIX + "SignatureSpi");
            provider.addAlgorithm("Alg.Alias.Signature.ECGOST-3410", "ECGOST3410");
            provider.addAlgorithm("Alg.Alias.Signature.GOST-3410-2001", "ECGOST3410");

            addSignatureAlgorithm(provider, "GOST3411", "ECGOST3410", PREFIX + "SignatureSpi", CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_2001);
        }
    }
}
