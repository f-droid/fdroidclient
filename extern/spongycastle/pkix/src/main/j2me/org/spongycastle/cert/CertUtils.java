package org.spongycastle.cert;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DERGeneralizedTime;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.AttributeCertificate;
import org.spongycastle.asn1.x509.AttributeCertificateInfo;
import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.asn1.x509.TBSCertList;
import org.spongycastle.asn1.x509.TBSCertificate;
import org.spongycastle.operator.ContentSigner;

class CertUtils
{
    private static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());
    private static List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());

    static X509CertificateHolder generateFullCert(ContentSigner signer, TBSCertificate tbsCert)
    {
        try
        {
            return new X509CertificateHolder(generateStructure(tbsCert, signer.getAlgorithmIdentifier(), generateSig(signer, tbsCert)));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("cannot produce certificate signature");
        }
    }

    static X509AttributeCertificateHolder generateFullAttrCert(ContentSigner signer, AttributeCertificateInfo attrInfo)
    {
        try
        {
            return new X509AttributeCertificateHolder(generateAttrStructure(attrInfo, signer.getAlgorithmIdentifier(), generateSig(signer, attrInfo)));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("cannot produce attribute certificate signature");
        }
    }

    static X509CRLHolder generateFullCRL(ContentSigner signer, TBSCertList tbsCertList)
    {
        try
        {
            return new X509CRLHolder(generateCRLStructure(tbsCertList, signer.getAlgorithmIdentifier(), generateSig(signer, tbsCertList)));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("cannot produce certificate signature");
        }
    }

    private static byte[] generateSig(ContentSigner signer, ASN1Encodable tbsObj)
        throws IOException
    {
        OutputStream sOut = signer.getOutputStream();
        DEROutputStream dOut = new DEROutputStream(sOut);

        dOut.writeObject(tbsObj);

        sOut.close();

        return signer.getSignature();
    }

    private static Certificate generateStructure(TBSCertificate tbsCert, AlgorithmIdentifier sigAlgId, byte[] signature)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));

        return Certificate.getInstance(new DERSequence(v));
    }

    private static AttributeCertificate generateAttrStructure(AttributeCertificateInfo attrInfo, AlgorithmIdentifier sigAlgId, byte[] signature)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(attrInfo);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));

        return AttributeCertificate.getInstance(new DERSequence(v));
    }

    private static CertificateList generateCRLStructure(TBSCertList tbsCertList, AlgorithmIdentifier sigAlgId, byte[] signature)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCertList);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));

        return CertificateList.getInstance(new DERSequence(v));
    }

    static Set getCriticalExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getCriticalExtensionOIDs())));
    }

    static Set getNonCriticalExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        // TODO: should probably produce a set that imposes correct ordering
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getNonCriticalExtensionOIDs())));
    }

    static List getExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_LIST;
        }

        return Collections.unmodifiableList(Arrays.asList(extensions.getExtensionOIDs()));
    }

    static void addExtension(ExtensionsGenerator extGenerator, ASN1ObjectIdentifier oid, boolean isCritical, ASN1Encodable value)
        throws CertIOException
    {
        try
        {
            extGenerator.addExtension(oid, isCritical, value);
        }
        catch (IOException e)
        {
            throw new CertIOException("cannot encode extension: " + e.getMessage(), e);
        }
    }

    static DERBitString booleanToBitString(boolean[] id)
    {
        byte[] bytes = new byte[(id.length + 7) / 8];

        for (int i = 0; i != id.length; i++)
        {
            bytes[i / 8] |= (id[i]) ? (1 << ((7 - (i % 8)))) : 0;
        }

        int pad = id.length % 8;

        if (pad == 0)
        {
            return new DERBitString(bytes);
        }
        else
        {
            return new DERBitString(bytes, 8 - pad);
        }
    }

    static boolean[] bitStringToBoolean(DERBitString bitString)
    {
        if (bitString != null)
        {
            byte[]          bytes = bitString.getBytes();
            boolean[]       boolId = new boolean[bytes.length * 8 - bitString.getPadBits()];

            for (int i = 0; i != boolId.length; i++)
            {
                boolId[i] = (bytes[i / 8] & (0x80 >>> (i % 8))) != 0;
            }

            return boolId;
        }

        return null;
    }

    static Date recoverDate(DERGeneralizedTime time)
    {
        return time.getDate();
    }

    static boolean dateBefore(Date d1, Date d2)
    {
        return d1.getTime() < d2.getTime();
    }

    static boolean dateAfter(Date d1, Date d2)
    {
        return d1.getTime() > d2.getTime();
    }

    static boolean isAlgIdEqual(AlgorithmIdentifier id1, AlgorithmIdentifier id2)
    {
        if (!id1.getAlgorithm().equals(id2.getAlgorithm()))
        {
            return false;
        }

        if (id1.getParameters() == null)
        {
            if (id2.getParameters() != null && !id2.getParameters().equals(DERNull.INSTANCE))
            {
                return false;
            }

            return true;
        }

        if (id2.getParameters() == null)
        {
            if (id1.getParameters() != null && !id1.getParameters().equals(DERNull.INSTANCE))
            {
                return false;
            }

            return true;
        }

        return id1.getParameters().equals(id2.getParameters());
    }
}
