package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.util.Log;
import kellinwood.security.zipsigner.ZipSigner;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

// TODO Address exception handling in a uniform way throughout

@SuppressWarnings("LineLength")
public final class LocalRepoKeyStore {

    private static final String TAG = "LocalRepoKeyStore";

    private static final String INDEX_CERT_ALIAS = "fdroid";
    private static final String HTTP_CERT_ALIAS = "https";

    private static final String DEFAULT_SIG_ALG = "SHA1withRSA";
    private static final String DEFAULT_KEY_ALGO = "RSA";
    private static final int DEFAULT_KEY_BITS = 2048;

    private static final String DEFAULT_INDEX_CERT_INFO = "O=Kerplapp,OU=GuardianProject";

    private static LocalRepoKeyStore localRepoKeyStore;
    private KeyStore keyStore;
    private KeyManager[] keyManagers;
    private File keyStoreFile;

    public static LocalRepoKeyStore get(Context context) throws InitException {
        if (localRepoKeyStore == null) {
            localRepoKeyStore = new LocalRepoKeyStore(context);
        }
        return localRepoKeyStore;
    }

    @SuppressWarnings("serial")
    public static class InitException extends Exception {
        public InitException(String detailMessage) {
            super(detailMessage);
        }
    }

    private LocalRepoKeyStore(Context context) throws InitException {
        try {
            File appKeyStoreDir = context.getDir("keystore", Context.MODE_PRIVATE);

            Utils.debugLog(TAG, "Generating LocalRepoKeyStore instance: " + appKeyStoreDir.getAbsolutePath());
            this.keyStoreFile = new File(appKeyStoreDir, "kerplapp.bks");

            Utils.debugLog(TAG, "Using default KeyStore type: " + KeyStore.getDefaultType());
            this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            if (keyStoreFile.exists()) {
                InputStream in = null;
                try {
                    Utils.debugLog(TAG, "Keystore already exists, loading...");
                    in = new FileInputStream(keyStoreFile);
                    keyStore.load(in, "".toCharArray());
                } catch (IOException e) {
                    Log.e(TAG, "Error while loading existing keystore. Will delete and create a new one.");

                    // NOTE: Could opt to delete and then re-create the keystore here, but that may
                    // be undesirable. For example - if you were to re-connect to an existing device
                    // that you have swapped apps with in the past, then you would really want the
                    // signature to be the same as last time.
                    throw new InitException("Could not initialize local repo keystore: " + e);
                } finally {
                    Utils.closeQuietly(in);
                }
            }

            if (!keyStoreFile.exists()) {
                // If there isn't a persisted BKS keystore on disk we need to
                // create a new empty keystore
                // Init a new keystore with a blank passphrase
                Utils.debugLog(TAG, "Keystore doesn't exist, creating...");
                keyStore.load(null, "".toCharArray());
            }

            /*
             * If the keystore we loaded doesn't have an INDEX_CERT_ALIAS entry
             * we need to generate a new random keypair and a self signed
             * certificate for this slot.
             */
            if (keyStore.getKey(INDEX_CERT_ALIAS, "".toCharArray()) == null) {
                /*
                 * Generate a random key pair to associate with the
                 * INDEX_CERT_ALIAS certificate in the keystore. This keypair
                 * will be used for the HTTPS cert as well.
                 */
                KeyPair rndKeys = generateRandomKeypair();

                /*
                 * Generate a self signed certificate for signing the index.jar
                 * We can't generate the HTTPS certificate until we know what
                 * the IP address will be to use for the CN field.
                 */
                X500Name subject = new X500Name(DEFAULT_INDEX_CERT_INFO);
                Certificate indexCert = generateSelfSignedCertChain(rndKeys, subject);

                addToStore(INDEX_CERT_ALIAS, rndKeys, indexCert);
            }

            /*
             * Kerplapp uses its own KeyManager to to ensure the correct
             * keystore alias is used for the correct purpose. With the default
             * key manager it is not possible to specify that HTTP_CERT_ALIAS
             * should be used for TLS and INDEX_CERT_ALIAS for signing the
             * index.jar.
             */
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(keyStore, "".toCharArray());
            KeyManager defaultKeyManager = keyManagerFactory.getKeyManagers()[0];
            KeyManager wrappedKeyManager = new KerplappKeyManager(
                    (X509KeyManager) defaultKeyManager);
            keyManagers = new KeyManager[]{
                    wrappedKeyManager,
            };
        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | OperatorCreationException | IOException e) {
            Log.e(TAG, "Error loading keystore", e);
        }
    }

    public void setupHTTPSCertificate() {
        try {
            // Get the existing private/public keypair to use for the HTTPS cert
            KeyPair kerplappKeypair = getKerplappKeypair();

            /*
             * Once we have an IP address, that can be used as the hostname. We
             * can generate a self signed cert with a valid CN field to stash
             * into the keystore in a predictable place. If the IP address
             * changes we should run this method again to stomp old
             * HTTPS_CERT_ALIAS entries.
             */
            X500Name subject = new X500Name("CN=" + FDroidApp.ipAddressString);
            Certificate indexCert = generateSelfSignedCertChain(kerplappKeypair, subject,
                    FDroidApp.ipAddressString);
            addToStore(HTTP_CERT_ALIAS, kerplappKeypair, indexCert);
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup HTTPS certificate", e);
        }
    }

    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    public void signZip(File input, File output) {
        try {
            ZipSigner zipSigner = new ZipSigner();

            X509Certificate cert = (X509Certificate) keyStore.getCertificate(INDEX_CERT_ALIAS);

            KeyPair kp = getKerplappKeypair();
            PrivateKey priv = kp.getPrivate();

            zipSigner.setKeys("kerplapp", cert, priv, DEFAULT_SIG_ALG, null);
            zipSigner.signZip(input.getAbsolutePath(), output.getAbsolutePath());

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | GeneralSecurityException | IOException e) {
            Log.e(TAG, "Unable to sign local repo index", e);
        }
    }

    private KeyPair getKerplappKeypair() throws KeyStoreException, UnrecoverableKeyException,
            NoSuchAlgorithmException {
        /*
         * You can't store a keypair without an associated certificate chain so,
         * we'll use the INDEX_CERT_ALIAS as the de-facto keypair/certificate
         * chain. This cert/key is initialized when the KerplappKeyStore is
         * constructed for the first time and should *always* be present.
         */
        Key key = keyStore.getKey(INDEX_CERT_ALIAS, "".toCharArray());

        if (key instanceof PrivateKey) {
            Certificate cert = keyStore.getCertificate(INDEX_CERT_ALIAS);
            PublicKey publicKey = cert.getPublicKey();
            return new KeyPair(publicKey, (PrivateKey) key);
        }

        return null;
    }

    public Certificate getCertificate() {
        try {
            Key key = keyStore.getKey(INDEX_CERT_ALIAS, "".toCharArray());
            if (key instanceof PrivateKey) {
                return keyStore.getCertificate(INDEX_CERT_ALIAS);
            }
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Unable to get certificate for local repo", e);
        }
        return null;
    }

    private void addToStore(String alias, KeyPair kp, Certificate cert) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        Certificate[] chain = {
                cert,
        };
        keyStore.setKeyEntry(alias, kp.getPrivate(),
                "".toCharArray(), chain);

        keyStore.store(new FileOutputStream(keyStoreFile), "".toCharArray());

        /*
         * After adding an entry to the keystore we need to create a fresh
         * KeyManager by reinitializing the KeyManagerFactory with the new key
         * store content and then rewrapping the default KeyManager with our own
         */
        KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());

        keyManagerFactory.init(keyStore, "".toCharArray());
        KeyManager defaultKeyManager = keyManagerFactory.getKeyManagers()[0];
        KeyManager wrappedKeyManager = new KerplappKeyManager((X509KeyManager) defaultKeyManager);
        keyManagers = new KeyManager[]{
                wrappedKeyManager,
        };
    }

    private KeyPair generateRandomKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_KEY_ALGO);
        keyPairGenerator.initialize(DEFAULT_KEY_BITS);
        return keyPairGenerator.generateKeyPair();
    }

    private Certificate generateSelfSignedCertChain(KeyPair kp, X500Name subject)
            throws CertificateException, OperatorCreationException, IOException {
        return generateSelfSignedCertChain(kp, subject, null);
    }

    private Certificate generateSelfSignedCertChain(KeyPair kp, X500Name subject, String hostname)
            throws CertificateException, OperatorCreationException, IOException {
        SecureRandom rand = new SecureRandom();
        PrivateKey privKey = kp.getPrivate();
        PublicKey pubKey = kp.getPublic();
        ContentSigner sigGen = new JcaContentSignerBuilder(DEFAULT_SIG_ALG).build(privKey);

        SubjectPublicKeyInfo subPubKeyInfo = new SubjectPublicKeyInfo(
                ASN1Sequence.getInstance(pubKey.getEncoded()));

        Date now = new Date(); // now

        /* force it to use a English/Gregorian dates for the cert, hardly anyone
           ever looks at the cert metadata anyway, and its very likely that they
           understand English/Gregorian dates */
        Calendar c = new GregorianCalendar(Locale.ENGLISH);
        c.setTime(now);
        c.add(Calendar.YEAR, 1);
        Time startTime = new Time(now, Locale.ENGLISH);
        Time endTime = new Time(c.getTime(), Locale.ENGLISH);

        X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(rand.nextLong()),
                startTime,
                endTime,
                subject,
                subPubKeyInfo);

        if (hostname != null) {
            GeneralNames subjectAltName = new GeneralNames(
                    new GeneralName(GeneralName.iPAddress, hostname));
            v3CertGen.addExtension(X509Extension.subjectAlternativeName, false, subjectAltName);
        }

        X509CertificateHolder certHolder = v3CertGen.build(sigGen);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    /*
     * A X509KeyManager that always returns the KerplappKeyStore.HTTP_CERT_ALIAS
     * for it's chosen server alias. All other operations are deferred to the
     * wrapped X509KeyManager.
     */
    private static final class KerplappKeyManager implements X509KeyManager {
        private final X509KeyManager wrapped;

        private KerplappKeyManager(X509KeyManager wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return wrapped.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            /*
             * Always use the HTTP_CERT_ALIAS for the server alias.
             */
            return LocalRepoKeyStore.HTTP_CERT_ALIAS;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return wrapped.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return wrapped.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return wrapped.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return wrapped.getServerAliases(keyType, issuers);
        }
    }

}
