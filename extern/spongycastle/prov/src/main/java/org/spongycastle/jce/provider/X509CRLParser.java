package org.spongycastle.jce.provider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.SignedData;
import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.x509.X509StreamParserSpi;
import org.spongycastle.x509.util.StreamParsingException;

public class X509CRLParser
    extends X509StreamParserSpi
{
    private static final PEMUtil PEM_PARSER = new PEMUtil("CRL");

    private ASN1Set     sData = null;
    private int         sDataObjectCount = 0;
    private InputStream currentStream = null;

    private CRL readDERCRL(
        InputStream in)
        throws IOException, CRLException
    {
        ASN1InputStream dIn = new ASN1InputStream(in);
        ASN1Sequence seq = (ASN1Sequence)dIn.readObject();

        if (seq.size() > 1
                && seq.getObjectAt(0) instanceof DERObjectIdentifier)
        {
            if (seq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
            {
                sData = new SignedData(ASN1Sequence.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true)).getCRLs();

                return getCRL();
            }
        }

        return new X509CRLObject(CertificateList.getInstance(seq));
    }

    private CRL getCRL()
        throws CRLException
    {
        if (sData == null || sDataObjectCount >= sData.size())
        {
            return null;
        }

        return new X509CRLObject(
                        CertificateList.getInstance(
                                sData.getObjectAt(sDataObjectCount++)));
    }

    private CRL readPEMCRL(
        InputStream  in)
        throws IOException, CRLException
    {
        ASN1Sequence seq = PEM_PARSER.readPEMObject(in);

        if (seq != null)
        {
            return new X509CRLObject(CertificateList.getInstance(seq));
        }

        return null;
    }

    public void engineInit(InputStream in)
    {
        currentStream = in;
        sData = null;
        sDataObjectCount = 0;

        if (!currentStream.markSupported())
        {
            currentStream = new BufferedInputStream(currentStream);
        }
    }

    public Object engineRead()
        throws StreamParsingException
    {
        try
        {
            if (sData != null)
            {
                if (sDataObjectCount != sData.size())
                {
                    return getCRL();
                }
                else
                {
                    sData = null;
                    sDataObjectCount = 0;
                    return null;
                }
            }

            currentStream.mark(10);
            int    tag = currentStream.read();

            if (tag == -1)
            {
                return null;
            }

            if (tag != 0x30)  // assume ascii PEM encoded.
            {
                currentStream.reset();
                return readPEMCRL(currentStream);
            }
            else
            {
                currentStream.reset();
                return readDERCRL(currentStream);
            }
        }
        catch (Exception e)
        {
            throw new StreamParsingException(e.toString(), e);
        }
    }

    public Collection engineReadAll()
        throws StreamParsingException
    {
        CRL     crl;
        List certs = new ArrayList();

        while ((crl = (CRL)engineRead()) != null)
        {
            certs.add(crl);
        }

        return certs;
    }
}
