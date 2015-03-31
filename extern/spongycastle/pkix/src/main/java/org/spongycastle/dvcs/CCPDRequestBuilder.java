package org.spongycastle.dvcs;

import org.spongycastle.asn1.dvcs.DVCSRequestInformationBuilder;
import org.spongycastle.asn1.dvcs.Data;
import org.spongycastle.asn1.dvcs.ServiceType;

/**
 * Builder of CCPD requests (Certify Claim of Possession of Data).
 */
public class CCPDRequestBuilder
    extends DVCSRequestBuilder
{
    public CCPDRequestBuilder()
    {
        super(new DVCSRequestInformationBuilder(ServiceType.CCPD));
    }

    /**
     * Builds CCPD request.
     *
     * @param messageImprint - the message imprint to include.
     * @return
     * @throws DVCSException
     */
    public DVCSRequest build(MessageImprint messageImprint)
        throws DVCSException
    {
        Data data = new Data(messageImprint.toASN1Structure());

        return createDVCRequest(data);
    }
}
