package org.spongycastle.jce.provider;

import javax.crypto.spec.DHParameterSpec;

import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.config.ProviderConfiguration;
import org.spongycastle.jce.spec.ECParameterSpec;

class BouncyCastleProviderConfiguration
    implements ProviderConfiguration
{
    private volatile ECParameterSpec ecImplicitCaParams;
    private volatile Object dhDefaultParams;

    void setParameter(String parameterName, Object parameter)
    {
        SecurityManager securityManager = System.getSecurityManager();

        if (parameterName.equals(ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA))
        {
            ECParameterSpec curveSpec;

            if (parameter instanceof ECParameterSpec || parameter == null)
            {
                curveSpec = (ECParameterSpec)parameter;
            }
            else
            {
                throw new IllegalArgumentException("not a valid ECParameterSpec");
            }

            ecImplicitCaParams = (ECParameterSpec)curveSpec;
        }
        else if (parameterName.equals(ConfigurableProvider.EC_IMPLICITLY_CA))
        {
            if (parameter instanceof ECParameterSpec || parameter == null)
            {
                ecImplicitCaParams = (ECParameterSpec)parameter;
            }
            else  // assume java.security.spec
            {
                throw new IllegalArgumentException("not a valid ECParameterSpec");
            }
        }
        else if (parameterName.equals(ConfigurableProvider.THREAD_LOCAL_DH_DEFAULT_PARAMS))
        {
            Object dhSpec;


            if (parameter instanceof DHParameterSpec || parameter instanceof DHParameterSpec[] || parameter == null)
            {
                dhSpec = parameter;
            }
            else
            {
                throw new IllegalArgumentException("not a valid DHParameterSpec");
            }

            dhDefaultParams = dhSpec;
        }
        else if (parameterName.equals(ConfigurableProvider.DH_DEFAULT_PARAMS))
        {

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
        return ecImplicitCaParams;
    }

    public DHParameterSpec getDHDefaultParameters(int keySize)
    {
        Object    params = dhDefaultParams;

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
