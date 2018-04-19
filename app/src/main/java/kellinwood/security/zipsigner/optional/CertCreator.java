package kellinwood.security.zipsigner.optional;

import kellinwood.security.zipsigner.KeySet;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.x509.X509V3CertificateGenerator;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;


/**
 * All methods create self-signed certificates.
 */
public class CertCreator {

    /** Creates a new keystore and self-signed key.  The key will have the same password as the key, and will be
     *  RSA 2048, with the cert signed using SHA1withRSA.  The certificate will have a validity of
     *  30 years).
     *
     * @param storePath - pathname of the new keystore file
     * @param password - keystore and key password
     * @param keyName - the new key will have this as its alias within the keystore
     * @param distinguishedNameValues - contains Country, State, Locality,...,Common Name, etc.
     */
    public static void createKeystoreAndKey( String storePath, char[] password,
                                      String keyName, DistinguishedNameValues distinguishedNameValues)
    {
        createKeystoreAndKey(storePath, password, "RSA", 2048, keyName, password, "SHA1withRSA", 30,
            distinguishedNameValues);
    }


    public static KeySet createKeystoreAndKey( String storePath, char[] storePass,
                                      String keyAlgorithm, int keySize, String keyName, char[] keyPass,
                                      String certSignatureAlgorithm, int certValidityYears, DistinguishedNameValues distinguishedNameValues) {
        try {

            KeySet keySet = createKey(keyAlgorithm, keySize, keyName, certSignatureAlgorithm, certValidityYears,
                distinguishedNameValues);


            KeyStore privateKS = KeyStoreFileManager.createKeyStore(storePath, storePass);

            privateKS.setKeyEntry(keyName, keySet.getPrivateKey(),
                keyPass,
                new java.security.cert.Certificate[]{keySet.getPublicKey()});

            File sfile = new File(storePath);
            if (sfile.exists()) {
                throw new IOException("File already exists: " + storePath);
            }
            KeyStoreFileManager.writeKeyStore( privateKS, storePath, storePass);

            return keySet;
        } catch (RuntimeException x) {
            throw x;
        } catch ( Exception x) {
            throw new RuntimeException( x.getMessage(), x);
        }
    }

    /** Create a new key and store it in an existing keystore.
     *
     */
    public static KeySet createKey( String storePath, char[] storePass,
                           String keyAlgorithm, int keySize, String keyName, char[] keyPass,
                           String certSignatureAlgorithm, int certValidityYears,
                           DistinguishedNameValues distinguishedNameValues) {
        try {

            KeySet keySet = createKey(keyAlgorithm, keySize, keyName, certSignatureAlgorithm, certValidityYears,
                distinguishedNameValues);

            KeyStore privateKS = KeyStoreFileManager.loadKeyStore(storePath, storePass);

            privateKS.setKeyEntry(keyName, keySet.getPrivateKey(),
                keyPass,
                new java.security.cert.Certificate[]{keySet.getPublicKey()});

            KeyStoreFileManager.writeKeyStore( privateKS, storePath, storePass);

            return keySet;

        } catch (RuntimeException x) {
            throw x;
        } catch ( Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    public static KeySet createKey( String keyAlgorithm, int keySize, String keyName,
                           String certSignatureAlgorithm, int certValidityYears, DistinguishedNameValues distinguishedNameValues)
    {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
            keyPairGenerator.initialize(keySize);
            KeyPair KPair = keyPairGenerator.generateKeyPair();

            X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

            X509Principal principal = distinguishedNameValues.getPrincipal();

            // generate a postitive serial number
            BigInteger serialNumber = BigInteger.valueOf(new SecureRandom().nextInt());
            while (serialNumber.compareTo(BigInteger.ZERO) < 0) {
                serialNumber = BigInteger.valueOf(new SecureRandom().nextInt());
            }
            v3CertGen.setSerialNumber(serialNumber);
            v3CertGen.setIssuerDN( principal);
            v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L));
            v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60L * 60L * 24L * 366L * (long)certValidityYears)));
            v3CertGen.setSubjectDN(principal);

            v3CertGen.setPublicKey(KPair.getPublic());
            v3CertGen.setSignatureAlgorithm(certSignatureAlgorithm);

            X509Certificate PKCertificate = v3CertGen.generate(KPair.getPrivate(),"BC");

            KeySet keySet = new KeySet();
            keySet.setName(keyName);
            keySet.setPrivateKey(KPair.getPrivate());
            keySet.setPublicKey(PKCertificate);
            return keySet;
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }
}
