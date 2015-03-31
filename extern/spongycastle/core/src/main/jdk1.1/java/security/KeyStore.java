
package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

public class KeyStore extends Object
{
    private KeyStoreSpi keyStoreSpi;
    private Provider provider;
    private String type;
    private boolean initialised;

    protected KeyStore(
        KeyStoreSpi keyStoreSpi,
        Provider provider,
        String type)
    {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = type;
        this.initialised = false;
    }

    public final Enumeration aliases() throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineAliases();
    }

    public final boolean containsAlias(String alias) throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineContainsAlias(alias);
    }

    public final void deleteEntry(String alias) throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        keyStoreSpi.engineDeleteEntry(alias);
    }

    public final Certificate getCertificate(String alias)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineGetCertificate(alias);
    }

    public final String getCertificateAlias(Certificate cert)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineGetCertificateAlias(cert);
    }

    public final Certificate[] getCertificateChain(String alias)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineGetCertificateChain(alias);
    }

    public final Date getCreationDate(String alias) throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineGetCreationDate(alias);
    }

    public static final String getDefaultType()
    {
        return "JKS";
    }

    public static KeyStore getInstance(String type) throws KeyStoreException
    {
        try
        {
            SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("KeyStore", type, null);

            if (imp != null)
            {
                return new KeyStore((KeyStoreSpi)imp.getEngine(), imp.getProvider(), type);
            }

            throw new KeyStoreException("can't find type " + type);
        }
        catch (NoSuchProviderException e)
        {
            throw new KeyStoreException(type + " not found");
        }
    }

    public static KeyStore getInstance(String type, String provider)
    throws KeyStoreException, NoSuchProviderException
    {
        SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("KeyStore", type, provider);

        if (imp != null)
        {
            return new KeyStore((KeyStoreSpi)imp.getEngine(), imp.getProvider(), type);
        }

        throw new KeyStoreException("can't find type " + type);
    }

    public final Key getKey(String alias, char[] password)
    throws KeyStoreException, NoSuchAlgorithmException,
        UnrecoverableKeyException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineGetKey(alias, password);
    }

    public final Provider getProvider()
    {
        return provider;
    }

    public final String getType()
    {
        return type;
    }

    public final boolean isCertificateEntry(String alias)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineIsCertificateEntry(alias);
    }

    public final boolean isKeyEntry(String alias) throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineIsKeyEntry(alias);
    }

    public final void load(
        InputStream stream,
        char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStoreSpi.engineLoad(stream, password);
        initialised = true;
    }

    public final void setCertificateEntry(String alias, Certificate cert)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    public final void setKeyEntry(
        String alias,
        Key key,
        char[] password,
        Certificate[] chain)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    public final void setKeyEntry(
        String alias,
        byte[] key,
        Certificate[] chain)
    throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    public final int size() throws KeyStoreException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        return keyStoreSpi.engineSize();
    }

    public final void store(
        OutputStream stream,
        char[] password)
    throws KeyStoreException, IOException, NoSuchAlgorithmException,
        CertificateException
    {
        if ( !initialised )
            throw new KeyStoreException("KeyStore not initialised.");

        keyStoreSpi.engineStore(stream, password);
    }
}
