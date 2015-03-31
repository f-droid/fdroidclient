package org.spongycastle.dvcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spongycastle.asn1.dvcs.Data;
import org.spongycastle.asn1.dvcs.TargetEtcChain;

/**
 * Data piece of DVCS request to VPKC service (Verify Public Key Certificates).
 * It contains VPKC-specific interface.
 * <p/>
 * This objects are constructed internally,
 * to build DVCS request to VPKC service use VPKCRequestBuilder.
 */
public class VPKCRequestData
    extends DVCSRequestData
{
    private List chains;

    VPKCRequestData(Data data)
        throws DVCSConstructionException
    {
        super(data);

        TargetEtcChain[] certs = data.getCerts();

        if (certs == null)
        {
            throw new DVCSConstructionException("DVCSRequest.data.certs should be specified for VPKC service");
        }

        chains = new ArrayList(certs.length);

        for (int i = 0; i != certs.length; i++)
        {
            chains.add(new TargetChain(certs[i]));
        }
    }

    /**
     * Get contained certs choice data..
     *
     * @return a list of CertChain objects.
     */
    public List getCerts()
    {
        return Collections.unmodifiableList(chains);
    }
}
