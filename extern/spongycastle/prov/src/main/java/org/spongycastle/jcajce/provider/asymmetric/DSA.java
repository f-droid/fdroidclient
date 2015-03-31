package org.spongycastle.jcajce.provider.asymmetric;

import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.spongycastle.jcajce.provider.asymmetric.dsa.KeyFactorySpi;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public class DSA
{
    private static final String PREFIX = "org.spongycastle.jcajce.provider.asymmetric" + ".dsa.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }
        
        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("AlgorithmParameters.DSA", PREFIX + "AlgorithmParametersSpi");

            provider.addAlgorithm("AlgorithmParameterGenerator.DSA", PREFIX + "AlgorithmParameterGeneratorSpi");

            provider.addAlgorithm("KeyPairGenerator.DSA", PREFIX + "KeyPairGeneratorSpi");
            provider.addAlgorithm("KeyFactory.DSA", PREFIX + "KeyFactorySpi");

            provider.addAlgorithm("Signature.DSA", PREFIX + "DSASigner$stdDSA");
            provider.addAlgorithm("Signature.NONEWITHDSA", PREFIX + "DSASigner$noneDSA");

            provider.addAlgorithm("Alg.Alias.Signature.RAWDSA", "NONEWITHDSA");

            provider.addAlgorithm("Signature.DETDSA", PREFIX + "DSASigner$detDSA");
            provider.addAlgorithm("Signature.SHA1WITHDETDSA", PREFIX + "DSASigner$detDSA");
            provider.addAlgorithm("Signature.SHA224WITHDETDSA", PREFIX + "DSASigner$detDSA224");
            provider.addAlgorithm("Signature.SHA256WITHDETDSA", PREFIX + "DSASigner$detDSA256");
            provider.addAlgorithm("Signature.SHA384WITHDETDSA", PREFIX + "DSASigner$detDSA384");
            provider.addAlgorithm("Signature.SHA512WITHDETDSA", PREFIX + "DSASigner$detDSA512");

            addSignatureAlgorithm(provider, "SHA224", "DSA", PREFIX + "DSASigner$dsa224", NISTObjectIdentifiers.dsa_with_sha224);
            addSignatureAlgorithm(provider, "SHA256", "DSA", PREFIX + "DSASigner$dsa256", NISTObjectIdentifiers.dsa_with_sha256);
            addSignatureAlgorithm(provider, "SHA384", "DSA", PREFIX + "DSASigner$dsa384", NISTObjectIdentifiers.dsa_with_sha384);
            addSignatureAlgorithm(provider, "SHA512", "DSA", PREFIX + "DSASigner$dsa512", NISTObjectIdentifiers.dsa_with_sha512);

            provider.addAlgorithm("Alg.Alias.Signature.SHA/DSA", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.SHA1withDSA", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.SHA1WITHDSA", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.10040.4.1", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.10040.4.3", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.DSAwithSHA1", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.DSAWITHSHA1", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.SHA1WithDSA", "DSA");
            provider.addAlgorithm("Alg.Alias.Signature.DSAWithSHA1", "DSA");

            provider.addAlgorithm("Alg.Alias.Signature.1.2.840.10040.4.3", "DSA");

            AsymmetricKeyInfoConverter keyFact = new KeyFactorySpi();

            for (int i = 0; i != DSAUtil.dsaOids.length; i++)
            {
                provider.addAlgorithm("Alg.Alias.Signature." + DSAUtil.dsaOids[i], "DSA");

                registerOid(provider, DSAUtil.dsaOids[i], "DSA", keyFact);
                registerOidAlgorithmParameters(provider, DSAUtil.dsaOids[i], "DSA");
            }
        }
    }
}
