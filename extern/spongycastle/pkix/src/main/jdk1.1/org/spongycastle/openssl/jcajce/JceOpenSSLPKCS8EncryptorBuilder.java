package org.spongycastle.openssl.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.KeyDerivationFunc;
import org.spongycastle.asn1.pkcs.PBES2Parameters;
import org.spongycastle.asn1.pkcs.PBKDF2Params;
import org.spongycastle.asn1.pkcs.PKCS12PBEParams;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.OutputEncryptor;
import org.spongycastle.operator.jcajce.JceGenericKey;

public class JceOpenSSLPKCS8EncryptorBuilder
{
    public static final String AES_128_CBC = NISTObjectIdentifiers.id_aes128_CBC.getId();
    public static final String AES_192_CBC = NISTObjectIdentifiers.id_aes192_CBC.getId();
    public static final String AES_256_CBC = NISTObjectIdentifiers.id_aes256_CBC.getId();

    public static final String DES3_CBC = PKCSObjectIdentifiers.des_EDE3_CBC.getId();

    public static final String PBE_SHA1_RC4_128 = PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4.getId();
    public static final String PBE_SHA1_RC4_40 = PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4.getId();
    public static final String PBE_SHA1_3DES = PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC.getId();
    public static final String PBE_SHA1_2DES = PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC.getId();
    public static final String PBE_SHA1_RC2_128 = PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC2_CBC.getId();
    public static final String PBE_SHA1_RC2_40 = PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC.getId();

    private JcaJceHelper helper = new DefaultJcaJceHelper();

    private AlgorithmParameters params;
    private ASN1ObjectIdentifier algOID;
    byte[] salt;
    int iterationCount;
    private Cipher cipher;
    private SecureRandom random;
    private AlgorithmParameterGenerator paramGen;
    private SecretKeyFactory secKeyFact;
    private char[] password;

    private SecretKey key;

    public JceOpenSSLPKCS8EncryptorBuilder(ASN1ObjectIdentifier algorithm)
    {
        algOID = algorithm;

        this.iterationCount = 2048;
    }

    public JceOpenSSLPKCS8EncryptorBuilder setRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public JceOpenSSLPKCS8EncryptorBuilder setPasssword(char[] password)
    {
        this.password = password;

        return this;
    }

    public JceOpenSSLPKCS8EncryptorBuilder setIterationCount(int iterationCount)
    {
        this.iterationCount = iterationCount;

        return this;
    }

    public JceOpenSSLPKCS8EncryptorBuilder setProvider(String providerName)
    {
        helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public JceOpenSSLPKCS8EncryptorBuilder setProvider(Provider provider)
    {
        helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public OutputEncryptor build()
        throws OperatorCreationException
    {
        final AlgorithmIdentifier algID;

        salt = new byte[20];

        if (random == null)
        {
            random = new SecureRandom();
        }

        random.nextBytes(salt);

        try
        {
            this.cipher = helper.createCipher(algOID.getId());

            if (PEMUtilities.isPKCS5Scheme2(algOID))
            {
                this.paramGen = helper.createAlgorithmParameterGenerator(algOID.getId());
            }
            else
            {
                this.secKeyFact = helper.createSecretKeyFactory(algOID.getId());
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new OperatorCreationException(algOID + " not available: " + e.getMessage(), e);
        }
        catch (NoSuchProviderException e)
        {
            throw new OperatorCreationException(algOID + " not available: " + e.getMessage(), e);
        }
        catch (GeneralSecurityException e)
        {
            throw new OperatorCreationException(algOID + " not available: " + e.getMessage(), e);
        }

        if (PEMUtilities.isPKCS5Scheme2(algOID))
        {
            params = paramGen.generateParameters();

            try
            {
                KeyDerivationFunc scheme = new KeyDerivationFunc(algOID, ASN1Primitive.fromByteArray(params.getEncoded()));
                KeyDerivationFunc func = new KeyDerivationFunc(PKCSObjectIdentifiers.id_PBKDF2, new PBKDF2Params(salt, iterationCount));

                ASN1EncodableVector v = new ASN1EncodableVector();

                v.add(func);
                v.add(scheme);

                algID = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_PBES2, PBES2Parameters.getInstance(new DERSequence(v)));
            }
            catch (IOException e)
            {
                throw new OperatorCreationException(e.getMessage(), e);
            }

            key = PEMUtilities.generateSecretKeyForPKCS5Scheme2(algOID.getId(), password, salt, iterationCount);

            try
            {
                cipher.init(Cipher.ENCRYPT_MODE, key, params);
            }
            catch (InvalidKeyException e)
            {
                throw new OperatorCreationException(e.getMessage(), e);
            }
            catch (GeneralSecurityException e)
            {
                throw new OperatorCreationException(e.getMessage(), e);
            }
        }
        else if (PEMUtilities.isPKCS12(algOID))
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(new DEROctetString(salt));
            v.add(new ASN1Integer(iterationCount));

            algID = new AlgorithmIdentifier(algOID, PKCS12PBEParams.getInstance(new DERSequence(v)));

            try
            {
                PBEKeySpec pbeSpec = new PBEKeySpec(password);
                PBEParameterSpec defParams = new PBEParameterSpec(salt, iterationCount);

                key = secKeyFact.generateSecret(pbeSpec);

                cipher.init(Cipher.ENCRYPT_MODE, key, defParams);
            }
            catch (InvalidKeyException e)
            {
                throw new OperatorCreationException(e.getMessage(), e);
            }
            catch (GeneralSecurityException e)
            {
                throw new OperatorCreationException(e.getMessage(), e);
            }
        }
        else
        {
            throw new OperatorCreationException("unknown algorithm: " + algOID, null);
        }

        return new OutputEncryptor()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return algID;
            }

            public OutputStream getOutputStream(OutputStream encOut)
            {
                return new CipherOutputStream(encOut, cipher);
            }

            public GenericKey getKey()
            {
                return new JceGenericKey(algID, key);
            }
        };
    }
}
