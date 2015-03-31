package org.spongycastle.pkcs.jcajce;

import java.io.OutputStream;
import java.security.Provider;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PKCS12PBEParams;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.generators.PKCS12ParametersGenerator;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.jcajce.io.MacOutputStream;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.MacCalculator;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.pkcs.PKCS12MacCalculatorBuilder;
import org.spongycastle.pkcs.PKCS12MacCalculatorBuilderProvider;

public class JcePKCS12MacCalculatorBuilderProvider
    implements PKCS12MacCalculatorBuilderProvider
{
    private JcaJceHelper helper = new DefaultJcaJceHelper();

    public JcePKCS12MacCalculatorBuilderProvider()
    {
    }

    public JcePKCS12MacCalculatorBuilderProvider setProvider(Provider provider)
    {
        this.helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public JcePKCS12MacCalculatorBuilderProvider setProvider(String providerName)
    {
        this.helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public PKCS12MacCalculatorBuilder get(final AlgorithmIdentifier algorithmIdentifier)
    {
        return new PKCS12MacCalculatorBuilder()
        {
            public MacCalculator build(final char[] password)
                throws OperatorCreationException
            {
                final PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                try
                {
                    final ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();

                    final Mac mac = helper.createMac(algorithm.getId());

                    SecretKeyFactory keyFact = helper.createSecretKeyFactory(algorithm.getId());
                    PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
                    PBEKeySpec pbeSpec = new PBEKeySpec(password);
                    SecretKey key = keyFact.generateSecret(pbeSpec);

                    mac.init(key, defParams);

                    return new MacCalculator()
                    {
                        public AlgorithmIdentifier getAlgorithmIdentifier()
                        {
                            return new AlgorithmIdentifier(algorithm, pbeParams);
                        }

                        public OutputStream getOutputStream()
                        {
                            return new MacOutputStream(mac);
                        }

                        public byte[] getMac()
                        {
                            return mac.doFinal();
                        }

                        public GenericKey getKey()
                        {
                            return new GenericKey(getAlgorithmIdentifier(), PKCS12ParametersGenerator.PKCS12PasswordToBytes(password));
                        }
                    };
                }
                catch (Exception e)
                {
                    throw new OperatorCreationException("unable to create MAC calculator: " + e.getMessage(), e);
                }
            }

            public AlgorithmIdentifier getDigestAlgorithmIdentifier()
            {
                return new AlgorithmIdentifier(algorithmIdentifier.getAlgorithm(), DERNull.INSTANCE);
            }
        };
    }
}
