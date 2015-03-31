package org.spongycastle.crypto.params;

import org.spongycastle.math.ec.ECPoint;

public class ECPublicKeyParameters
    extends ECKeyParameters
{
    ECPoint Q;

    public ECPublicKeyParameters(
        ECPoint             Q,
        ECDomainParameters  params)
    {
        super(false, params);
        this.Q = Q.normalize();
    }

    public ECPoint getQ()
    {
        return Q;
    }
}
