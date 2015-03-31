
package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

public abstract class KeyStoreSpi extends Object
{
    public KeyStoreSpi()
    {
    }

    public abstract Enumeration engineAliases();

    public abstract boolean engineContainsAlias(String alias);

    public abstract void engineDeleteEntry(String alias)
    throws KeyStoreException;

    public abstract Certificate engineGetCertificate(String alias);

    public abstract String engineGetCertificateAlias(Certificate cert);

    public abstract Certificate[] engineGetCertificateChain(String alias);

    public abstract Date engineGetCreationDate(String alias);

    public abstract Key engineGetKey(String alias, char[] password)
    throws NoSuchAlgorithmException, UnrecoverableKeyException;

    public abstract boolean engineIsCertificateEntry(String alias);

    public abstract boolean engineIsKeyEntry(String alias);

    public abstract void engineLoad(InputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException;

    public abstract void engineSetCertificateEntry(
        String alias, Certificate cert)
    throws KeyStoreException;

    public abstract void engineSetKeyEntry(
        String alias, Key key, char[] password, Certificate[] chain)
    throws KeyStoreException;

    public abstract void engineSetKeyEntry(
        String alias, byte[] key, Certificate[] chain)
    throws KeyStoreException;

    public abstract int engineSize();

    public abstract void engineStore(OutputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException;
}
