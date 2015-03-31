package org.spongycastle.pkcs.bc;

import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCS12PBEParams;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.MacCalculator;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcDigestProvider;
import org.spongycastle.pkcs.PKCS12MacCalculatorBuilder;
import org.spongycastle.pkcs.PKCS12MacCalculatorBuilderProvider;

public class BcPKCS12MacCalculatorBuilderProvider
    implements PKCS12MacCalculatorBuilderProvider
{
    private BcDigestProvider digestProvider;

    public BcPKCS12MacCalculatorBuilderProvider(BcDigestProvider digestProvider)
    {
        this.digestProvider = digestProvider;
    }

    public PKCS12MacCalculatorBuilder get(final AlgorithmIdentifier algorithmIdentifier)
    {
        return new PKCS12MacCalculatorBuilder()
        {
            public MacCalculator build(final char[] password)
                throws OperatorCreationException
            {
                PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                return PKCS12PBEUtils.createMacCalculator(algorithmIdentifier.getAlgorithm(), digestProvider.get(algorithmIdentifier), pbeParams, password);
            }

            public AlgorithmIdentifier getDigestAlgorithmIdentifier()
            {
                return new AlgorithmIdentifier(algorithmIdentifier.getAlgorithm(), DERNull.INSTANCE);
            }
        };
    }
}
