package org.spongycastle.jcajce;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;

public class NamedJcaJceHelper
    implements JcaJceHelper
{
    protected final String providerName;

    public NamedJcaJceHelper(String providerName)
    {
        this.providerName = providerName;
    }

    public Cipher createCipher(
        String algorithm)
        throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException
    {
        return Cipher.getInstance(algorithm, providerName);
    }

    public Mac createMac(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return Mac.getInstance(algorithm, providerName);
    }

    public KeyAgreement createKeyAgreement(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyAgreement.getInstance(algorithm, providerName);
    }

    public AlgorithmParameterGenerator createAlgorithmParameterGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return AlgorithmParameterGenerator.getInstance(algorithm, providerName);
    }

    public AlgorithmParameters createAlgorithmParameters(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return AlgorithmParameters.getInstance(algorithm, providerName);
    }

    public KeyGenerator createKeyGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyGenerator.getInstance(algorithm, providerName);
    }

    public KeyFactory createKeyFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyFactory.getInstance(algorithm, providerName);
    }

    public SecretKeyFactory createSecretKeyFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return SecretKeyFactory.getInstance(algorithm, providerName);
    }

    public KeyPairGenerator createKeyPairGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyPairGenerator.getInstance(algorithm, providerName);
    }

    public MessageDigest createDigest(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return MessageDigest.getInstance(algorithm, providerName);
    }

    public Signature createSignature(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return Signature.getInstance(algorithm, providerName);
    }

    public CertificateFactory createCertificateFactory(String algorithm)
        throws NoSuchAlgorithmException, CertificateException, NoSuchProviderException
    {
        return CertificateFactory.getInstance(algorithm, providerName);
    }
}
