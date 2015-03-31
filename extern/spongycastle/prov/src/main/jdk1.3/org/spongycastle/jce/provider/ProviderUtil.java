package org.spongycastle.jce.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;

import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.config.ProviderConfigurationPermission;
import org.spongycastle.jce.spec.ECParameterSpec;

public class ProviderUtil
{
    private static Permission BC_EC_LOCAL_PERMISSION = new ProviderConfigurationPermission(
                                                   "SC", ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA);
    private static Permission BC_EC_PERMISSION = new ProviderConfigurationPermission(
                                                   "SC", ConfigurableProvider.EC_IMPLICITLY_CA);

    private static ThreadLocal threadSpec = new ThreadLocal();
    private static volatile ECParameterSpec ecImplicitCaParams;

    static void setParameter(String parameterName, Object parameter)
    {
        SecurityManager securityManager = System.getSecurityManager();

        if (parameterName.equals(ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA))
        {
            ECParameterSpec curveSpec;

            if (securityManager != null)
            {
                securityManager.checkPermission(BC_EC_LOCAL_PERMISSION);
            }

            curveSpec = (ECParameterSpec)parameter;

            threadSpec.set(curveSpec);
        }
        else if (parameterName.equals(ConfigurableProvider.EC_IMPLICITLY_CA))
        {
            if (securityManager != null)
            {
                securityManager.checkPermission(BC_EC_PERMISSION);
            }

            ecImplicitCaParams = (ECParameterSpec)parameter;
        }
    }

    public static ECParameterSpec getEcImplicitlyCa()
    {
        ECParameterSpec spec = (ECParameterSpec)threadSpec.get();

        if (spec != null)
        {
            return spec;
        }

        return ecImplicitCaParams;
    }

    static int getReadLimit(InputStream in)
        throws IOException
    {
        if (in instanceof ByteArrayInputStream)
        {
            return in.available();
        }

        return Integer.MAX_VALUE;
    }
}
