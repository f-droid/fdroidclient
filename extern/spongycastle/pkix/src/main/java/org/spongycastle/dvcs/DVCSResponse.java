package org.spongycastle.dvcs;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.SignedData;
import org.spongycastle.asn1.dvcs.DVCSObjectIdentifiers;
import org.spongycastle.cms.CMSSignedData;

/**
 * DVCResponse is general response to DVCS (RFC 3029).
 * It represents responses for all types of services.
 */
public class DVCSResponse
    extends DVCSMessage
{
    private org.spongycastle.asn1.dvcs.DVCSResponse asn1;

    /**
     * Constructs DVCRequest from CMS SignedData object.
     *
     * @param signedData the CMS SignedData object containing the request
     * @throws org.spongycastle.dvcs.DVCSConstructionException
     */
    public DVCSResponse(CMSSignedData signedData)
        throws DVCSConstructionException
    {
        this(SignedData.getInstance(signedData.toASN1Structure().getContent()).getEncapContentInfo());
    }

    /**
     * Construct a DVCS Request from a ContentInfo
     *
     * @param contentInfo the contentInfo representing the DVCSRequest
     * @throws org.spongycastle.dvcs.DVCSConstructionException
     */
    public DVCSResponse(ContentInfo contentInfo)
        throws DVCSConstructionException
    {
        super(contentInfo);

        if (!DVCSObjectIdentifiers.id_ct_DVCSResponseData.equals(contentInfo.getContentType()))
        {
            throw new DVCSConstructionException("ContentInfo not a DVCS Request");
        }

        try
        {
            if (contentInfo.getContent().toASN1Primitive() instanceof ASN1Sequence)
            {
                this.asn1 = org.spongycastle.asn1.dvcs.DVCSResponse.getInstance(contentInfo.getContent());
            }
            else
            {
                this.asn1 = org.spongycastle.asn1.dvcs.DVCSResponse.getInstance(ASN1OctetString.getInstance(contentInfo.getContent()).getOctets());
            }
        }
        catch (Exception e)
        {
            throw new DVCSConstructionException("Unable to parse content: " + e.getMessage(), e);
        }
    }

    /**
     * Return the ASN.1 DVCSResponse structure making up the body of this response.
     *
     * @return an org.spongycastle.asn1.dvcs.DVCSResponse object.
     */
    public ASN1Encodable getContent()
    {
        return asn1;
    }
}
