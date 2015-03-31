package org.spongycastle.cms.jcajce;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.TBSCertificateStructure;
import org.spongycastle.cms.CMSException;
import org.spongycastle.jcajce.JcaJceUtils;

class CMSUtils
{
    static TBSCertificateStructure getTBSCertificateStructure(
        X509Certificate cert)
        throws CertificateEncodingException
    {
            return TBSCertificateStructure.getInstance(cert.getTBSCertificate());
    }

    static IssuerAndSerialNumber getIssuerAndSerialNumber(X509Certificate cert)
        throws CertificateEncodingException
    {
        Certificate certStruct = Certificate.getInstance(cert.getEncoded());

        return new IssuerAndSerialNumber(certStruct.getIssuer(), cert.getSerialNumber());
    }


    static byte[] getSubjectKeyId(X509Certificate cert)
    {
        byte[] ext = cert.getExtensionValue(Extension.subjectKeyIdentifier.getId());

        if (ext != null)
        {
            return ASN1OctetString.getInstance(ASN1OctetString.getInstance(ext).getOctets()).getOctets();
        }
        else
        {
            return null;
        }
    }

    static EnvelopedDataHelper createContentHelper(Provider provider)
    {
        if (provider != null)
        {
            return new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));
        }
        else
        {
            return new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
        }
    }

    static EnvelopedDataHelper createContentHelper(String providerName)
    {
        if (providerName != null)
        {
            return new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));
        }
        else
        {
            return new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
        }
    }

    static ASN1Encodable extractParameters(AlgorithmParameters params)
        throws CMSException
    {
        try
        {
            return JcaJceUtils.extractParameters(params);
        }
        catch (IOException e)
        {
            throw new CMSException("cannot extract parameters: " + e.getMessage(), e);
        }
    }

    static void loadParameters(AlgorithmParameters params, ASN1Encodable sParams)
        throws CMSException
    {
        try
        {
            JcaJceUtils.loadParameters(params, sParams);
        }
        catch (IOException e)
        {
            throw new CMSException("error encoding algorithm parameters.", e);
        }
    }
}