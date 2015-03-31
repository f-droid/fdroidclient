package kellinwood.security.zipsigner.optional;


import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;



/**
 */
public class KeyStoreFileManager {

    static Provider provider = new BouncyCastleProvider();

    public static Provider getProvider() { return provider; }

    public static void setProvider(Provider provider) {
        if (KeyStoreFileManager.provider != null) Security.removeProvider( KeyStoreFileManager.provider.getName());
        KeyStoreFileManager.provider = provider;
        Security.addProvider( provider);
    }

    static LoggerInterface logger = LoggerManager.getLogger( KeyStoreFileManager.class.getName());

    static {
        // Add the spongycastle version of the BC provider so that the implementation classes returned
        // from the keystore are all from the spongycastle libs.
        Security.addProvider(getProvider());
    }


    public static KeyStore loadKeyStore( String keystorePath, String encodedPassword)
        throws Exception{
        char password[] = null;
        try {
            if (encodedPassword != null) {
                password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, encodedPassword);
            }
            return loadKeyStore(keystorePath, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }
    }

    public static KeyStore createKeyStore( String keystorePath, char[] password)
        throws Exception
    {
        KeyStore ks = null;
        if (keystorePath.toLowerCase().endsWith(".bks")) {
            ks = KeyStore.getInstance("bks", new BouncyCastleProvider());
        }
        else ks = new JksKeyStore();
        ks.load(null, password);

        return ks;
    }

    public static KeyStore loadKeyStore( String keystorePath, char[] password)
        throws Exception
    {
        KeyStore ks = null;
        try {
            ks = new JksKeyStore();
            FileInputStream fis = new FileInputStream( keystorePath);
            ks.load( fis, password);
            fis.close();
            return ks;
        } catch (LoadKeystoreException x) {
            // This type of exception is thrown when the keystore is a JKS keystore, but the file is malformed
            // or the validity/password check failed.  In this case don't bother to attempt loading it as a BKS keystore.
            throw x;
        } catch (Exception x) {
            // logger.warning( x.getMessage(), x);
            try {
                ks = KeyStore.getInstance("bks", getProvider());
                FileInputStream fis = new FileInputStream( keystorePath);
                ks.load( fis, password);
                fis.close();
                return ks;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore: " + e.getMessage(), e);
            }
        }
    }

    public static void writeKeyStore( KeyStore ks, String keystorePath, String encodedPassword)
        throws Exception
    {
        char password[] = null;
        try {
            password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, encodedPassword);
            writeKeyStore( ks, keystorePath, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }
    }

    public static void writeKeyStore( KeyStore ks, String keystorePath, char[] password)
        throws Exception
    {

        File keystoreFile = new File( keystorePath);
        try {
            if (keystoreFile.exists()) {
                // I've had some trouble saving new verisons of the keystore file in which the file becomes empty/corrupt.
                // Saving the new version to a new file and creating a backup of the old version.
                File tmpFile = File.createTempFile( keystoreFile.getName(), null, keystoreFile.getParentFile());
                FileOutputStream fos = new FileOutputStream( tmpFile);
                ks.store(fos, password);
                fos.flush();
                fos.close();
                /* create a backup of the previous version
                int i = 1;
                File backup = new File( keystorePath + "." + i + ".bak");
                while (backup.exists()) {
                    i += 1;
                    backup = new File( keystorePath + "." + i + ".bak");
                }
                renameTo(keystoreFile, backup);
                */
                renameTo(tmpFile, keystoreFile);
            } else {
                FileOutputStream fos = new FileOutputStream( keystorePath);
                ks.store(fos, password);
                fos.close();
            }
        } catch (Exception x) {
            try {
                File logfile = File.createTempFile("zipsigner-error", ".log", keystoreFile.getParentFile());
                PrintWriter pw = new PrintWriter(new FileWriter( logfile));
                x.printStackTrace( pw);
                pw.flush();
                pw.close();
            } catch (Exception y) {}
            throw x;
        }
    }


    static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream input = new FileInputStream(srcFile);
        try {
            FileOutputStream output = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                long count = 0;
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                    count += n;
                }
            } finally {
                try { output.close();  } catch (IOException x) {} // Ignore
            }
        } finally {
            try { input.close(); } catch (IOException x) {}
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" +
                srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }


    public static void renameTo(File fromFile, File toFile)
        throws IOException
    {
        copyFile(fromFile, toFile, true);
        if (!fromFile.delete()) throw new IOException("Failed to delete " + fromFile);
    }

    public static void deleteKey(String storePath, String storePass, String keyName)
        throws Exception
    {
        KeyStore ks = loadKeyStore( storePath, storePass);
        ks.deleteEntry( keyName);
        writeKeyStore(ks, storePath, storePass);
    }

    public static String renameKey( String keystorePath, String storePass, String oldKeyName, String newKeyName, String keyPass)
        throws Exception
    {
        char[] keyPw = null;

        try {
            KeyStore ks = loadKeyStore(keystorePath, storePass);
            if (ks instanceof JksKeyStore) newKeyName = newKeyName.toLowerCase();

            if (ks.containsAlias(newKeyName)) throw new KeyNameConflictException();

            keyPw = PasswordObfuscator.getInstance().decodeAliasPassword( keystorePath, oldKeyName, keyPass);
            Key key = ks.getKey(oldKeyName, keyPw);
            Certificate cert = ks.getCertificate( oldKeyName);

            ks.setKeyEntry(newKeyName, key, keyPw, new Certificate[] { cert});
            ks.deleteEntry( oldKeyName);

            writeKeyStore(ks, keystorePath, storePass);
            return newKeyName;
        }
        finally {
            PasswordObfuscator.flush(keyPw);
        }
    }

    public static KeyStore.Entry getKeyEntry( String keystorePath, String storePass, String keyName, String keyPass)
        throws Exception
    {
        char[] keyPw = null;
        KeyStore.PasswordProtection passwordProtection = null;

        try {
            KeyStore ks = loadKeyStore(keystorePath, storePass);
            keyPw = PasswordObfuscator.getInstance().decodeAliasPassword( keystorePath, keyName, keyPass);
            passwordProtection = new KeyStore.PasswordProtection(keyPw);
            return ks.getEntry( keyName, passwordProtection);
        }
        finally {
            if (keyPw != null) PasswordObfuscator.flush(keyPw);
            if (passwordProtection != null) passwordProtection.destroy();
        }
    }

    public static boolean containsKey( String keystorePath, String storePass, String keyName)
        throws Exception
    {
        KeyStore ks = loadKeyStore(keystorePath, storePass);
        return ks.containsAlias( keyName);
    }


    /**
     *
     * @param keystorePath
     * @param encodedPassword
     * @throws Exception if the password is invalid
     */
    public static void validateKeystorePassword( String keystorePath, String encodedPassword)
        throws Exception
    {
        char[] password = null;
        try {
            KeyStore ks = KeyStoreFileManager.loadKeyStore( keystorePath, encodedPassword);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }

    }

    /**
     *
     * @param keystorePath
     * @param keyName
     * @param encodedPassword
     * @throws java.security.UnrecoverableKeyException if the password is invalid
     */
    public static void validateKeyPassword( String keystorePath, String keyName, String encodedPassword)
        throws Exception
    {
        char[] password = null;
        try {
            KeyStore ks = KeyStoreFileManager.loadKeyStore( keystorePath, (char[])null);
            password = PasswordObfuscator.getInstance().decodeAliasPassword(keystorePath,keyName, encodedPassword);
            ks.getKey(keyName, password);
        } finally {
            if (password != null) PasswordObfuscator.flush(password);
        }

    }
}
