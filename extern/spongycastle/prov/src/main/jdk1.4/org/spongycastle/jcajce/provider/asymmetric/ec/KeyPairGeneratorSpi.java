package org.spongycastle.jcajce.provider.asymmetric.ec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.spongycastle.asn1.x9.X962NamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jcajce.provider.config.ProviderConfiguration;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveGenParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.util.Integers;

public abstract class KeyPairGeneratorSpi
    extends java.security.KeyPairGenerator
{
    public KeyPairGeneratorSpi(String algorithmName)
    {
        super(algorithmName);
    }

    public static class EC
        extends KeyPairGeneratorSpi
    {
        ECKeyGenerationParameters   param;
        ECKeyPairGenerator          engine = new ECKeyPairGenerator();
        ECParameterSpec             ecParams = null;
        int                         strength = 239;
        int                         certainty = 50;
        SecureRandom                random = new SecureRandom();
        boolean                     initialised = false;
        String                      algorithm;
        ProviderConfiguration       configuration;

        static private Hashtable    ecParameters;

        static {
            ecParameters = new Hashtable();

            ecParameters.put(Integers.valueOf(192),
                ECNamedCurveTable.getParameterSpec("prime192v1"));
            ecParameters.put(Integers.valueOf(239),
                ECNamedCurveTable.getParameterSpec("prime239v1"));
            ecParameters.put(Integers.valueOf(256),
                ECNamedCurveTable.getParameterSpec("prime256v1"));
        }

        public EC()
        {
            super("EC");
            this.algorithm = "EC";
            this.configuration = BouncyCastleProvider.CONFIGURATION;
        }

        public EC(
            String  algorithm,
            ProviderConfiguration configuration)
        {
            super(algorithm);
            this.algorithm = algorithm;
            this.configuration = configuration;
        }

        public void initialize(
            int             strength,
            SecureRandom    random)
        {
            this.strength = strength;
            this.random = random;
            this.ecParams = (ECParameterSpec)ecParameters.get(Integers.valueOf(strength));

            if (ecParams != null)
            {
                param = new ECKeyGenerationParameters(new ECDomainParameters(ecParams.getCurve(), ecParams.getG(), ecParams.getN()), random);

                engine.init(param);
                initialised = true;
            }
            else
            {
                throw new InvalidParameterException("unknown key size.");
            }
        }

        public void initialize(
            AlgorithmParameterSpec  params,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (params instanceof ECParameterSpec)
            {
                ECParameterSpec p = (ECParameterSpec)params;
                this.ecParams = (ECParameterSpec)params;

                param = new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN()), random);

                engine.init(param);
                initialised = true;
            }
            else if (params instanceof ECNamedCurveGenParameterSpec)
            {
                String curveName;

                curveName = ((ECNamedCurveGenParameterSpec)params).getName();

                X9ECParameters  ecP = X962NamedCurves.getByName(curveName);
                if (ecP == null)
                {
                    ecP = SECNamedCurves.getByName(curveName);
                    if (ecP == null)
                    {
                        ecP = NISTNamedCurves.getByName(curveName);
                    }
                    if (ecP == null)
                    {
                        ecP = TeleTrusTNamedCurves.getByName(curveName);
                    }
                    if (ecP == null)
                    {
                        // See if it's actually an OID string (SunJSSE ServerHandshaker setupEphemeralECDHKeys bug)
                        try
                        {
                            ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(curveName);
                            ecP = X962NamedCurves.getByOID(oid);
                            if (ecP == null)
                            {
                                ecP = SECNamedCurves.getByOID(oid);
                            }
                            if (ecP == null)
                            {
                                ecP = NISTNamedCurves.getByOID(oid);
                            }
                            if (ecP == null)
                            {
                                ecP = TeleTrusTNamedCurves.getByOID(oid);
                            }
                            if (ecP == null)
                            {
                                throw new InvalidAlgorithmParameterException("unknown curve OID: " + curveName);
                            }
                        }
                        catch (IllegalArgumentException ex)
                        {
                            throw new InvalidAlgorithmParameterException("unknown curve name: " + curveName);
                        }
                    }
                }

                this.ecParams = new ECNamedCurveParameterSpec(
                        curveName,
                        ecP.getCurve(),
                        ecP.getG(),
                        ecP.getN(),
                        ecP.getH(),
                        null); // ecP.getSeed());   Work-around JDK bug -- it won't look up named curves properly if seed is present

                param = new ECKeyGenerationParameters(new ECDomainParameters(ecParams.getCurve(), ecParams.getG(), ecParams.getN()), random);

                engine.init(param);
                initialised = true;
            }
            else if (params == null && configuration.getEcImplicitlyCa() != null)
            {
                ECParameterSpec p = configuration.getEcImplicitlyCa();
                this.ecParams = (ECParameterSpec)params;

                param = new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN()), random);

                engine.init(param);
                initialised = true;
            }
            else if (params == null && configuration.getEcImplicitlyCa() == null)
            {
                throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
            }
            else
            {
                throw new InvalidAlgorithmParameterException("parameter object not a ECParameterSpec");
            }
        }

        public KeyPair generateKeyPair()
        {
            if (!initialised)
            {
                throw new IllegalStateException("EC Key Pair Generator not initialised");
            }

            AsymmetricCipherKeyPair     pair = engine.generateKeyPair();
            ECPublicKeyParameters       pub = (ECPublicKeyParameters)pair.getPublic();
            ECPrivateKeyParameters      priv = (ECPrivateKeyParameters)pair.getPrivate();

            if (ecParams == null)
            {
               return new KeyPair(new BCECPublicKey(algorithm, pub, configuration),
                                   new BCECPrivateKey(algorithm, priv, configuration));
            }
            else
            {
                ECParameterSpec p = (ECParameterSpec)ecParams;
                BCECPublicKey pubKey = new BCECPublicKey(algorithm, pub, p, configuration);
                
                return new KeyPair(pubKey, new BCECPrivateKey(algorithm, priv, pubKey, p, configuration));
            }
        }
    }

    public static class ECDSA
        extends EC
    {
        public ECDSA()
        {
            super("ECDSA", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDH
        extends EC
    {
        public ECDH()
        {
            super("ECDH", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDHC
        extends EC
    {
        public ECDHC()
        {
            super("ECDHC", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECMQV
        extends EC
    {
        public ECMQV()
        {
            super("ECMQV", BouncyCastleProvider.CONFIGURATION);
        }
    }
}
