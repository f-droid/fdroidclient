package org.spongycastle.pkcs.jcajce;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.operator.OutputEncryptor;
import org.spongycastle.pkcs.PKCS12SafeBagBuilder;
import org.spongycastle.pkcs.PKCSIOException;

public class JcaPKCS12SafeBagBuilder
    extends PKCS12SafeBagBuilder
{
    public JcaPKCS12SafeBagBuilder(X509Certificate certificate)
        throws IOException
    {
        super(convertCert(certificate));
    }

    private static Certificate convertCert(X509Certificate certificate)
        throws IOException
    {
        try
        {
            return Certificate.getInstance(certificate.getEncoded());
        }
        catch (CertificateEncodingException e)
        {
            throw new PKCSIOException("cannot encode certificate: " + e.getMessage(), e);
        }
    }

    public JcaPKCS12SafeBagBuilder(PrivateKey privateKey, OutputEncryptor encryptor)
    {
        super(PrivateKeyInfo.getInstance(privateKey.getEncoded()), encryptor);
    }

    public JcaPKCS12SafeBagBuilder(PrivateKey privateKey)
    {
        super(PrivateKeyInfo.getInstance(privateKey.getEncoded()));
    }
}
