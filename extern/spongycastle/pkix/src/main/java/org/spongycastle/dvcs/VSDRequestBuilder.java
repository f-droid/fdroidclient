package org.spongycastle.dvcs;

import java.io.IOException;
import java.util.Date;

import org.spongycastle.asn1.dvcs.DVCSRequestInformationBuilder;
import org.spongycastle.asn1.dvcs.DVCSTime;
import org.spongycastle.asn1.dvcs.Data;
import org.spongycastle.asn1.dvcs.ServiceType;
import org.spongycastle.cms.CMSSignedData;

/**
 * Builder of DVCS requests to VSD service (Verify Signed Document).
 */
public class VSDRequestBuilder
    extends DVCSRequestBuilder
{
    public VSDRequestBuilder()
    {
        super(new DVCSRequestInformationBuilder(ServiceType.VSD));
    }

    public void setRequestTime(Date requestTime)
    {
        requestInformationBuilder.setRequestTime(new DVCSTime(requestTime));
    }

    /**
     * Build VSD request from CMS SignedData object.
     *
     * @param document
     * @return
     * @throws DVCSException
     */
    public DVCSRequest build(CMSSignedData document)
        throws DVCSException
    {
        try
        {
            Data data = new Data(document.getEncoded());

            return createDVCRequest(data);
        }
        catch (IOException e)
        {
            throw new DVCSException("Failed to encode CMS signed data", e);
        }
    }
}
