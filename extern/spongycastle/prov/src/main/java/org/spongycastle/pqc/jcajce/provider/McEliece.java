package org.spongycastle.pqc.jcajce.provider;

import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import org.spongycastle.pqc.asn1.PQCObjectIdentifiers;

public class McEliece
{
    private static final String PREFIX = "org.spongycastle.pqc.jcajce.provider" + ".mceliece.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            // McElieceKobaraImai
            provider.addAlgorithm("KeyPairGenerator.McElieceKobaraImai", PREFIX + "McElieceKeyPairGeneratorSpi$McElieceCCA2");
            // McEliecePointcheval
            provider.addAlgorithm("KeyPairGenerator.McEliecePointcheval", PREFIX + "McElieceKeyPairGeneratorSpi$McElieceCCA2");
            // McElieceFujisaki
            provider.addAlgorithm("KeyPairGenerator.McElieceFujisaki", PREFIX + "McElieceKeyPairGeneratorSpi$McElieceCCA2");
            // McEliecePKCS
            provider.addAlgorithm("KeyPairGenerator.McEliecePKCS", PREFIX + "McElieceKeyPairGeneratorSpi$McEliece");

            provider.addAlgorithm("KeyPairGenerator." + PQCObjectIdentifiers.mcEliece, PREFIX + "McElieceKeyPairGeneratorSpi$McEliece");
            provider.addAlgorithm("KeyPairGenerator." + PQCObjectIdentifiers.mcElieceCca2, PREFIX + "McElieceKeyPairGeneratorSpi$McElieceCCA2");

            provider.addAlgorithm("Cipher.McEliecePointcheval", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval");
            provider.addAlgorithm("Cipher.McEliecePointchevalWithSHA1", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval");
            provider.addAlgorithm("Cipher.McEliecePointchevalWithSHA224", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval224");
            provider.addAlgorithm("Cipher.McEliecePointchevalWithSHA256", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval256");
            provider.addAlgorithm("Cipher.McEliecePointchevalWithSHA384", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval384");
            provider.addAlgorithm("Cipher.McEliecePointchevalWithSHA512", PREFIX + "McEliecePointchevalCipherSpi$McEliecePointcheval512");

            provider.addAlgorithm("Cipher.McEliecePKCS", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS");
            provider.addAlgorithm("Cipher.McEliecePKCSWithSHA1", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS");
            provider.addAlgorithm("Cipher.McEliecePKCSWithSHA224", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS224");
            provider.addAlgorithm("Cipher.McEliecePKCSWithSHA256", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS256");
            provider.addAlgorithm("Cipher.McEliecePKCSWithSHA384", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS384");
            provider.addAlgorithm("Cipher.McEliecePKCSWithSHA512", PREFIX + "McEliecePKCSCipherSpi$McEliecePKCS512");

            provider.addAlgorithm("Cipher.McElieceKobaraImai", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai");
            provider.addAlgorithm("Cipher.McElieceKobaraImaiWithSHA1", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai");
            provider.addAlgorithm("Cipher.McElieceKobaraImaiWithSHA224", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai224");
            provider.addAlgorithm("Cipher.McElieceKobaraImaiWithSHA256", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai256");
            provider.addAlgorithm("Cipher.McElieceKobaraImaiWithSHA384", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai384");
            provider.addAlgorithm("Cipher.McElieceKobaraImaiWithSHA512", PREFIX + "McElieceKobaraImaiCipherSpi$McElieceKobaraImai512");

            provider.addAlgorithm("Cipher.McElieceFujisaki", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki");
            provider.addAlgorithm("Cipher.McElieceFujisakiWithSHA1", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki");
            provider.addAlgorithm("Cipher.McElieceFujisakiWithSHA224", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki224");
            provider.addAlgorithm("Cipher.McElieceFujisakiWithSHA256", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki256");
            provider.addAlgorithm("Cipher.McElieceFujisakiWithSHA384", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki384");
            provider.addAlgorithm("Cipher.McElieceFujisakiWithSHA512", PREFIX + "McElieceFujisakiCipherSpi$McElieceFujisaki512");

        }
    }
}
