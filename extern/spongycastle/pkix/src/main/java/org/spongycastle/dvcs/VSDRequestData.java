package org.spongycastle.dvcs;

import org.spongycastle.asn1.dvcs.Data;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSSignedData;

/**
 * Data piece of DVCS request to VSD service (Verify Signed Document).
 * It contains VSD-specific selector interface.
 * Note: the request should contain CMS SignedData object as message.
 * <p/>
 * This objects are constructed internally,
 * to build DVCS request to VSD service use VSDRequestBuilder.
 */
public class VSDRequestData
    extends DVCSRequestData
{
    private CMSSignedData doc;

    VSDRequestData(Data data)
        throws DVCSConstructionException
    {
        super(data);
        initDocument();
    }

    private void initDocument()
        throws DVCSConstructionException
    {
        if (doc == null)
        {
            if (data.getMessage() == null)
            {
                throw new DVCSConstructionException("DVCSRequest.data.message should be specified for VSD service");
            }
            try
            {
                doc = new CMSSignedData(data.getMessage().getOctets());
            }
            catch (CMSException e)
            {
                throw new DVCSConstructionException("Can't read CMS SignedData from input", e);
            }
        }
    }

    /**
     * Get contained message (data to be certified).
     *
     * @return
     */
    public byte[] getMessage()
    {
        return data.getMessage().getOctets();
    }

    /**
     * Get the CMS SignedData object represented by the encoded message.
     *
     * @return
     */
    public CMSSignedData getParsedMessage()
    {
        return doc;
    }
}
