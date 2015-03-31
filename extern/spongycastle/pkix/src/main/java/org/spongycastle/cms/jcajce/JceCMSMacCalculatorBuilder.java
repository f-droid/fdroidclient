package org.spongycastle.cms.jcajce;

import java.io.OutputStream;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.jcajce.io.MacOutputStream;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.MacCalculator;
import org.spongycastle.operator.jcajce.JceGenericKey;

public class JceCMSMacCalculatorBuilder
{
    private final ASN1ObjectIdentifier macOID;
    private final int                  keySize;

    private EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
    private SecureRandom random;

    public JceCMSMacCalculatorBuilder(ASN1ObjectIdentifier macOID)
    {
        this(macOID, -1);
    }

    public JceCMSMacCalculatorBuilder(ASN1ObjectIdentifier macOID, int keySize)
    {
        this.macOID = macOID;
        this.keySize = keySize;
    }

    public JceCMSMacCalculatorBuilder setProvider(Provider provider)
    {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));

        return this;
    }

    public JceCMSMacCalculatorBuilder setProvider(String providerName)
    {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));

        return this;
    }

    public JceCMSMacCalculatorBuilder setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public MacCalculator build()
        throws CMSException
    {
        return new CMSMacCalculator(macOID, keySize, random);
    }

    private class CMSMacCalculator
        implements MacCalculator
    {
        private SecretKey encKey;
        private AlgorithmIdentifier algorithmIdentifier;
        private Mac mac;
        private SecureRandom random;

        CMSMacCalculator(ASN1ObjectIdentifier macOID, int keySize, SecureRandom random)
            throws CMSException
        {
            KeyGenerator keyGen = helper.createKeyGenerator(macOID);

            if (random == null)
            {
                random = new SecureRandom();
            }

            this.random = random;

            if (keySize < 0)
            {
                keyGen.init(random);
            }
            else
            {
                keyGen.init(keySize, random);
            }

            encKey = keyGen.generateKey();

            AlgorithmParameterSpec paramSpec = generateParameterSpec(macOID, encKey);

            algorithmIdentifier = helper.getAlgorithmIdentifier(macOID, paramSpec);
            mac = helper.createContentMac(encKey, algorithmIdentifier);
        }

        public AlgorithmIdentifier getAlgorithmIdentifier()
        {
            return algorithmIdentifier;
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
            return new JceGenericKey(algorithmIdentifier, encKey);
        }

        protected AlgorithmParameterSpec generateParameterSpec(ASN1ObjectIdentifier macOID, SecretKey encKey)
            throws CMSException
        {
            try
            {
                if (macOID.equals(PKCSObjectIdentifiers.RC2_CBC))
                {
                    byte[] iv = new byte[8];

                    random.nextBytes(iv);

                    return new RC2ParameterSpec(encKey.getEncoded().length * 8, iv);
                }

                AlgorithmParameterGenerator pGen = helper.createAlgorithmParameterGenerator(macOID);

                AlgorithmParameters p = pGen.generateParameters();

                return p.getParameterSpec(IvParameterSpec.class);
            }
            catch (GeneralSecurityException e)
            {
                return null;
            }
        }
    }
}
