
package java.security.cert;

import java.util.Set;

public interface X509Extension 
{
    public abstract Set getCriticalExtensionOIDs();
    public abstract byte[] getExtensionValue(String oid);
    public abstract Set getNonCriticalExtensionOIDs();
    public abstract boolean hasUnsupportedCriticalExtension();
}
