package org.spongycastle.jce.provider;

import java.security.Permission;

import javax.crypto.spec.DHParameterSpec;

import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.config.ProviderConfiguration;
import org.spongycastle.jcajce.provider.config.ProviderConfigurationPermission;
import org.spongycastle.jce.spec.ECParameterSpec;

class BouncyCastleProviderConfiguration
    implements ProviderConfiguration
{
    private static Permission BC_EC_LOCAL_PERMISSION = new ProviderConfigurationPermission(
        BouncyCastleProvider.PROVIDER_NAME, ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA);
    private static Permission BC_EC_PERMISSION = new ProviderConfigurationPermission(
        BouncyCastleProvider.PROVIDER_NAME, ConfigurableProvider.EC_IMPLICITLY_CA);
    private static Permission BC_DH_LOCAL_PERMISSION = new ProviderConfigurationPermission(
        BouncyCastleProvider.PROVIDER_NAME, ConfigurableProvider.THREAD_LOCAL_DH_DEFAULT_PARAMS);
    private static Permission BC_DH_PERMISSION = new ProviderConfigurationPermission(
        BouncyCastleProvider.PROVIDER_NAME, ConfigurableProvider.DH_DEFAULT_PARAMS);

    private ThreadLocal ecThreadSpec = new ThreadLocal();
    private ThreadLocal dhThreadSpec = new ThreadLocal();

    private volatile ECParameterSpec ecImplicitCaParams;
    private volatile Object dhDefaultParams;

    void setParameter(String parameterName, Object parameter)
    {
        SecurityManager securityManager = System.getSecurityManager();

        if (parameterName.equals(ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA))
        {
            ECParameterSpec curveSpec;

            if (securityManager != null)
            {
                securityManager.checkPermission(BC_EC_LOCAL_PERMISSION);
            }

            if (parameter instanceof ECParameterSpec || parameter == null)
            {
                curveSpec = (ECParameterSpec)parameter;
            }
            else  // assume java.security.spec
            {
                curveSpec = EC5Util.convertSpec((java.security.spec.ECParameterSpec)parameter, false);
            }

            if (curveSpec == null)
            {
                ecThreadSpec.remove();
            }
            else
            {
                ecThreadSpec.set(curveSpec);
            }
        }
        else if (parameterName.equals(ConfigurableProvider.EC_IMPLICITLY_CA))
        {
            if (securityManager != null)
            {
                securityManager.checkPermission(BC_EC_PERMISSION);
            }

            if (parameter instanceof ECParameterSpec || parameter == null)
            {
                ecImplicitCaParams = (ECParameterSpec)parameter;
            }
            else  // assume java.security.spec
            {
                ecImplicitCaParams = EC5Util.convertSpec((java.security.spec.ECParameterSpec)parameter, false);
            }
        }
        else if (parameterName.equals(ConfigurableProvider.THREAD_LOCAL_DH_DEFAULT_PARAMS))
        {
            Object dhSpec;

            if (securityManager != null)
            {
                securityManager.checkPermission(BC_DH_LOCAL_PERMISSION);
            }

            if (parameter instanceof DHParameterSpec || parameter instanceof DHParameterSpec[] || parameter == null)
            {
                dhSpec = parameter;
            }
            else
            {
                throw new IllegalArgumentException("not a valid DHParameterSpec");
            }

            if (dhSpec == null)
            {
                dhThreadSpec.remove();
            }
            else
            {
                dhThreadSpec.set(dhSpec);
            }
        }
        else if (parameterName.equals(ConfigurableProvider.DH_DEFAULT_PARAMS))
        {
            if (securityManager != null)
            {
                securityManager.checkPermission(BC_DH_PERMISSION);
            }

            if (parameter instanceof DHParameterSpec || parameter instanceof DHParameterSpec[] || parameter == null)
            {
                dhDefaultParams = parameter;
            }
            else
            {
                throw new IllegalArgumentException("not a valid DHParameterSpec or DHParameterSpec[]");
            }
        }
    }

    public ECParameterSpec getEcImplicitlyCa()
    {
        ECParameterSpec spec = (ECParameterSpec)ecThreadSpec.get();

        if (spec != null)
        {
            return spec;
        }

        return ecImplicitCaParams;
    }

    public DHParameterSpec getDHDefaultParameters(int keySize)
    {
        Object params = dhThreadSpec.get();
        if (params == null)
        {
            params = dhDefaultParams;
        }

        if (params instanceof DHParameterSpec)
        {
            DHParameterSpec spec = (DHParameterSpec)params;

            if (spec.getP().bitLength() == keySize)
            {
                return spec;
            }
        }
        else if (params instanceof DHParameterSpec[])
        {
            DHParameterSpec[] specs = (DHParameterSpec[])params;

            for (int i = 0; i != specs.length; i++)
            {
                if (specs[i].getP().bitLength() == keySize)
                {
                    return specs[i];
                }
            }
        }

        return null;
    }
}
