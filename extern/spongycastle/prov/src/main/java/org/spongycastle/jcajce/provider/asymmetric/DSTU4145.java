package org.spongycastle.jcajce.provider.asymmetric;

import org.spongycastle.asn1.ua.UAObjectIdentifiers;
import org.spongycastle.jcajce.provider.asymmetric.dstu.KeyFactorySpi;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;

public class DSTU4145 
{
    private static final String PREFIX = "org.spongycastle.jcajce.provider.asymmetric" + ".dstu.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }
        
        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("KeyFactory.DSTU4145", PREFIX + "KeyFactorySpi");
            provider.addAlgorithm("Alg.Alias.KeyFactory.DSTU-4145-2002", "DSTU4145");
            provider.addAlgorithm("Alg.Alias.KeyFactory.DSTU4145-3410", "DSTU4145");

            registerOid(provider, UAObjectIdentifiers.dstu4145le, "DSTU4145", new KeyFactorySpi());
            registerOidAlgorithmParameters(provider, UAObjectIdentifiers.dstu4145le, "DSTU4145");
            registerOid(provider, UAObjectIdentifiers.dstu4145be, "DSTU4145", new KeyFactorySpi());
            registerOidAlgorithmParameters(provider, UAObjectIdentifiers.dstu4145be, "DSTU4145");

            provider.addAlgorithm("KeyPairGenerator.DSTU4145", PREFIX + "KeyPairGeneratorSpi");
            provider.addAlgorithm("Alg.Alias.KeyPairGenerator.DSTU-4145", "DSTU4145");
            provider.addAlgorithm("Alg.Alias.KeyPairGenerator.DSTU-4145-2002", "DSTU4145");

            provider.addAlgorithm("Signature.DSTU4145", PREFIX + "SignatureSpi");
            provider.addAlgorithm("Alg.Alias.Signature.DSTU-4145", "DSTU4145");
            provider.addAlgorithm("Alg.Alias.Signature.DSTU-4145-2002", "DSTU4145");

            addSignatureAlgorithm(provider, "GOST3411", "DSTU4145LE", PREFIX + "SignatureSpiLe", UAObjectIdentifiers.dstu4145le);
            addSignatureAlgorithm(provider, "GOST3411", "DSTU4145", PREFIX + "SignatureSpi", UAObjectIdentifiers.dstu4145be);
        }
    }
}
