package java.security;


class SecurityUtil
{
    static class Implementation
    {
        Object      engine;
        Provider    provider;

        Implementation(
            Object      engine,
            Provider    provider)
        {
            this.engine = engine;
            this.provider = provider;
        }

        Object getEngine()
        {
            return engine;
        }

        Provider getProvider()
        {
            return provider;
        }
    }

    /**
     * see if we can find an algorithm (or its alias and what it represents) in
     * the property table for the given provider.
     *
     * @return null if no algorithm found, an Implementation if it is.
     */
    static private Implementation getImplementation(
        String      baseName,
        String      algorithm,
        Provider    prov)
    {
        String      alias;

        while ((alias = prov.getProperty("Alg.Alias." + baseName + "." + algorithm)) != null)
        {
            algorithm = alias;
        }

        String      className = prov.getProperty(baseName + "." + algorithm);

        if (className != null)
        {
            try
            {
                return new Implementation(Class.forName(className).newInstance(), prov);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but no class found!");
            }
            catch (Exception e)
            {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but class inaccessible!");
            }
        }

        return null;
    }

    /**
     * return an implementation for a given algorithm/provider.
     * If the provider is null, we grab the first avalaible who has the required algorithm.
     *
     * @return null if no algorithm found, an Implementation if it is.
     * @exception NoSuchProviderException if a provider is specified and not found.
     */
    static Implementation getImplementation(
        String      baseName,
        String      algorithm,
        String      provider)
        throws NoSuchProviderException
    {
        if (provider == null)
        {
            Provider[] prov = Security.getProviders();

            //
            // search every provider looking for the algorithm we want.
            //
            for (int i = 0; i != prov.length; i++)
            {
                Implementation imp = getImplementation(baseName, algorithm, prov[i]);
                if (imp != null)
                {
                    return imp;
                }
            }
        }
        else
        {
            Provider prov = Security.getProvider(provider);

            if (prov == null)
            {
                throw new NoSuchProviderException("Provider " + provider + " not found");
            }

            return getImplementation(baseName, algorithm, prov);
        }

        return null;
    }
}
