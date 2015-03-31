package org.spongycastle.jcajce;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

public class ProviderJcaJceHelper
    implements JcaJceHelper
{
    protected final Provider provider;

    public ProviderJcaJceHelper(Provider provider)
    {
        this.provider = provider;
    }

    public Cipher createCipher(
        String algorithm)
        throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException
    {
        return Cipher.getInstance(algorithm, provider.getName());
    }

    public Mac createMac(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return Mac.getInstance(algorithm, provider.getName());
    }

    public KeyAgreement createKeyAgreement(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyAgreement.getInstance(algorithm, provider.getName());
    }

    public AlgorithmParameterGenerator createAlgorithmParameterGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return AlgorithmParameterGenerator.getInstance(algorithm, provider.getName());
    }

    public AlgorithmParameters createAlgorithmParameters(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return AlgorithmParameters.getInstance(algorithm, provider.getName());
    }

    public KeyGenerator createKeyGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyGenerator.getInstance(algorithm, provider.getName());
    }

    public KeyFactory createKeyFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyFactory.getInstance(algorithm, provider.getName());
    }

    public SecretKeyFactory createSecretKeyFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return SecretKeyFactory.getInstance(algorithm, provider.getName());
    }

    public KeyPairGenerator createKeyPairGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyPairGenerator.getInstance(algorithm, provider.getName());
    }

    public MessageDigest createDigest(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return MessageDigest.getInstance(algorithm, provider.getName());
    }

    public Signature createSignature(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return Signature.getInstance(algorithm, provider.getName());
    }

    public CertificateFactory createCertificateFactory(String algorithm)
        throws NoSuchAlgorithmException, CertificateException, NoSuchProviderException
    {
        return CertificateFactory.getInstance(algorithm, provider.getName());
    }
}
