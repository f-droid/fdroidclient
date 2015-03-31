package org.spongycastle.cms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.cms.OriginatorInfo;
import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.cert.X509CRLHolder;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.util.CollectionStore;
import org.spongycastle.util.Store;

public class OriginatorInformation
{
    private OriginatorInfo originatorInfo;

    OriginatorInformation(OriginatorInfo originatorInfo)
    {
        this.originatorInfo = originatorInfo;
    }

    /**
     * Return the certificates stored in the underlying OriginatorInfo object.
     *
     * @return a Store of X509CertificateHolder objects.
     */
    public Store getCertificates()
    {
        ASN1Set certSet = originatorInfo.getCertificates();

        if (certSet != null)
        {
            List certList = new ArrayList(certSet.size());

            for (Enumeration en = certSet.getObjects(); en.hasMoreElements();)
            {
                ASN1Primitive obj = ((ASN1Encodable)en.nextElement()).toASN1Primitive();

                if (obj instanceof ASN1Sequence)
                {
                    certList.add(new X509CertificateHolder(Certificate.getInstance(obj)));
                }
            }

            return new CollectionStore(certList);
        }

        return new CollectionStore(new ArrayList());
    }

    /**
     * Return the CRLs stored in the underlying OriginatorInfo object.
     *
     * @return a Store of X509CRLHolder objects.
     */
    public Store getCRLs()
    {
        ASN1Set crlSet = originatorInfo.getCRLs();

        if (crlSet != null)
        {
            List    crlList = new ArrayList(crlSet.size());

            for (Enumeration en = crlSet.getObjects(); en.hasMoreElements();)
            {
                ASN1Primitive obj = ((ASN1Encodable)en.nextElement()).toASN1Primitive();

                if (obj instanceof ASN1Sequence)
                {
                    crlList.add(new X509CRLHolder(CertificateList.getInstance(obj)));
                }
            }

            return new CollectionStore(crlList);
        }

        return new CollectionStore(new ArrayList());
    }

    /**
     * Return the underlying ASN.1 object defining this SignerInformation object.
     *
     * @return a OriginatorInfo.
     */
    public OriginatorInfo toASN1Structure()
    {
        return originatorInfo;
    }
}
