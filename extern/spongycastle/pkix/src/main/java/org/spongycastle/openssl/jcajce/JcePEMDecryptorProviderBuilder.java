package org.spongycastle.openssl.jcajce;

import java.security.Provider;

import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.openssl.PEMDecryptor;
import org.spongycastle.openssl.PEMDecryptorProvider;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.openssl.PasswordException;

public class JcePEMDecryptorProviderBuilder
{
    private JcaJceHelper helper = new DefaultJcaJceHelper();

    public JcePEMDecryptorProviderBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderJcaJceHelper(provider);

        return this;
    }

    public JcePEMDecryptorProviderBuilder setProvider(String providerName)
    {
        this.helper = new NamedJcaJceHelper(providerName);

        return this;
    }

    public PEMDecryptorProvider build(final char[] password)
    {
        return new PEMDecryptorProvider()
        {
            public PEMDecryptor get(final String dekAlgName)
            {
                return new PEMDecryptor()
                {
                    public byte[] decrypt(byte[] keyBytes, byte[] iv)
                        throws PEMException
                    {
                        if (password == null)
                        {
                            throw new PasswordException("Password is null, but a password is required");
                        }

                        return PEMUtilities.crypt(false, helper, keyBytes, password, dekAlgName, iv);
                    }
                };
            }
        };
    }
}
