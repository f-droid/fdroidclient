package org.spongycastle.cert;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.TBSCertList;

/**
 * Holding class for an X.509 CRL Entry structure.
 */
public class X509CRLEntryHolder
{
    private TBSCertList.CRLEntry entry;
    private GeneralNames ca;

    X509CRLEntryHolder(TBSCertList.CRLEntry entry, boolean isIndirect, GeneralNames previousCA)
    {
        this.entry = entry;
        this.ca = previousCA;

        if (isIndirect && entry.hasExtensions())
        {
            Extension currentCaName = entry.getExtensions().getExtension(Extension.certificateIssuer);

            if (currentCaName != null)
            {
                ca = GeneralNames.getInstance(currentCaName.getParsedValue());
            }
        }
    }

    /**
     * Return the serial number of the certificate associated with this CRLEntry.
     *
     * @return the revoked certificate's serial number.
     */
    public BigInteger getSerialNumber()
    {
        return entry.getUserCertificate().getValue();
    }

    /**
     * Return the date on which the certificate associated with this CRLEntry was revoked.
     *
     * @return the revocation date for the revoked certificate.
     */
    public Date getRevocationDate()
    {
        return entry.getRevocationDate().getDate();
    }

    /**
     * Return whether or not the holder's CRL entry contains extensions.
     *
     * @return true if extension are present, false otherwise.
     */
    public boolean hasExtensions()
    {
        return entry.hasExtensions();
    }

    /**
     * Return the available names for the certificate issuer for the certificate referred to by this CRL entry.
     * <p>
     * Note: this will be the issuer of the CRL unless it has been specified that the CRL is indirect
     * in the IssuingDistributionPoint extension and either a previous entry, or the current one,
     * has specified a different CA via the certificateIssuer extension.
     * </p>
     *
     * @return the revoked certificate's issuer.
     */
    public GeneralNames getCertificateIssuer()
    {
        return this.ca;
    }

    /**
     * Look up the extension associated with the passed in OID.
     *
     * @param oid the OID of the extension of interest.
     *
     * @return the extension if present, null otherwise.
     */
    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        Extensions extensions = entry.getExtensions();

        if (extensions != null)
        {
            return extensions.getExtension(oid);
        }

        return null;
    }

    /**
     * Return the extensions block associated with this CRL entry if there is one.
     *
     * @return the extensions block, null otherwise.
     */
    public Extensions getExtensions()
    {
        return entry.getExtensions();
    }

    /**
     * Returns a list of ASN1ObjectIdentifier objects representing the OIDs of the
     * extensions contained in this holder's CRL entry.
     *
     * @return a list of extension OIDs.
     */
    public List getExtensionOIDs()
    {
        return CertUtils.getExtensionOIDs(entry.getExtensions());
    }

    /**
     * Returns a set of ASN1ObjectIdentifier objects representing the OIDs of the
     * critical extensions contained in this holder's CRL entry.
     *
     * @return a set of critical extension OIDs.
     */
    public Set getCriticalExtensionOIDs()
    {
        return CertUtils.getCriticalExtensionOIDs(entry.getExtensions());
    }

    /**
     * Returns a set of ASN1ObjectIdentifier objects representing the OIDs of the
     * non-critical extensions contained in this holder's CRL entry.
     *
     * @return a set of non-critical extension OIDs.
     */
    public Set getNonCriticalExtensionOIDs()
    {
        return CertUtils.getNonCriticalExtensionOIDs(entry.getExtensions());
    }
}
