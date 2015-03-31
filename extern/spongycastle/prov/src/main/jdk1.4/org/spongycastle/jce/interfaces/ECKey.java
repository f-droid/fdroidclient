package org.spongycastle.jce.interfaces;

import org.spongycastle.jce.spec.ECParameterSpec;

/**
 * generic interface for an Elliptic Curve Key.
 */
public interface ECKey
{
    /**
     * return a parameter specification representing the EC domain parameters
     * for the key.
     * @deprecated this method vanises in JDK 1.5. Use getParameters().
     */
    public ECParameterSpec getParams();
    
    /**
     * return a parameter specification representing the EC domain parameters
     * for the key.
     */
    public ECParameterSpec getParameters();
}
