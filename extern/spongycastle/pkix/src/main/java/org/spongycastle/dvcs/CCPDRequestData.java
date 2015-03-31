package org.spongycastle.dvcs;

import org.spongycastle.asn1.dvcs.Data;

/**
 * Data piece of DVCRequest for CCPD service (Certify Claim of Possession of Data).
 * It contains CCPD-specific selector interface.
 * <p/>
 * This objects are constructed internally,
 * to build DVCS request to CCPD service use CCPDRequestBuilder.
 */
public class CCPDRequestData
    extends DVCSRequestData
{
    /**
     * Construct from corresponding ASN.1 Data structure.
     * Note, that data should have messageImprint choice,
     * otherwise DVCSConstructionException is thrown.
     *
     * @param data
     * @throws DVCSConstructionException
     */
    CCPDRequestData(Data data)
        throws DVCSConstructionException
    {
        super(data);
        initDigest();
    }

    private void initDigest()
        throws DVCSConstructionException
    {
        if (data.getMessageImprint() == null)
        {
            throw new DVCSConstructionException("DVCSRequest.data.messageImprint should be specified for CCPD service");
        }
    }

    /**
     * Get MessageImprint value
     *
     * @return
     */
    public MessageImprint getMessageImprint()
    {
        return new MessageImprint(data.getMessageImprint());
    }
}
