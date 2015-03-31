package org.spongycastle.pkcs.jcajce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Hashtable;

import org.spongycastle.asn1.pkcs.CertificationRequest;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.pkcs.PKCS10CertificationRequest;

public class JcaPKCS10CertificationRequest
    extends PKCS10CertificationRequest
{
    private static Hashtable keyAlgorithms = new Hashtable();

    static
    {
        //
        // key types
        //
        keyAlgorithms.put(PKCSObjectIdentifiers.rsaEncryption, "RSA");
        keyAlgorithms.put(X9ObjectIdentifiers.id_dsa, "DSA");
    }

    private JcaJceHelper helper = new DefaultJcaJceHelper();

    public JcaPKCS10CertificationRequest(CertificationRequest certificationRequest)
    {
        super(certificationRequest);
    }

    public JcaPKCS10CertificationRequest(byte[] encoding)
        throws IOException
    {
        super(encoding);
    }

    public JcaPKCS10CertificationRequest(PKCS10CertificationRequest requestHolder)
    {
        super(requestHolder.toASN1Structure());
    }

    public JcaPKCS10CertificationRequest setProvider(String providerName)
    {
        helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public JcaPKCS10CertificationRequest setProvider(Provider provider)
    {
        helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public PublicKey getPublicKey()
        throws InvalidKeyException, NoSuchAlgorithmException
    {
        try
        {
            SubjectPublicKeyInfo keyInfo = this.getSubjectPublicKeyInfo();
            X509EncodedKeySpec xspec = new X509EncodedKeySpec(keyInfo.getEncoded());
            KeyFactory kFact;

            try
            {
                kFact = helper.createKeyFactory(keyInfo.getAlgorithm().getAlgorithm().getId());
            }
            catch (NoSuchAlgorithmException e)
            {
                //
                // try an alternate
                //
                if (keyAlgorithms.get(keyInfo.getAlgorithm().getAlgorithm()) != null)
                {
                    String  keyAlgorithm = (String)keyAlgorithms.get(keyInfo.getAlgorithm().getAlgorithm());

                    kFact = helper.createKeyFactory(keyAlgorithm);
                }
                else
                {
                    throw e;
                }
            }

            return kFact.generatePublic(xspec);
        }
        catch (InvalidKeySpecException e)
        {
            throw new InvalidKeyException("error decoding public key");
        }
        catch (IOException e)
        {
            throw new InvalidKeyException("error extracting key encoding");
        }
        catch (NoSuchProviderException e)
        {
            throw new NoSuchAlgorithmException("cannot find provider: " + e.getMessage());
        }
    }
}
