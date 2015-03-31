package kellinwood.security.zipsigner.optional;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import kellinwood.security.zipsigner.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

public class PasswordObfuscator {

    private static PasswordObfuscator instance = null;
    
    static final String x = "harold-and-maude";   

    LoggerInterface logger;
    SecretKeySpec skeySpec;
    
    private PasswordObfuscator() {
        logger = LoggerManager.getLogger( PasswordObfuscator.class.getName());
        skeySpec = new SecretKeySpec(x.getBytes(), "AES");
    }

    public static PasswordObfuscator getInstance() {
        if (instance == null) instance = new PasswordObfuscator();
        return instance;
    }

    public String encodeKeystorePassword( String keystorePath, String password) {
        return encode( keystorePath, password);
    }

    public String encodeKeystorePassword( String keystorePath, char[] password) {
        return encode( keystorePath, password);
    }

    public String encodeAliasPassword( String keystorePath, String aliasName, String password) {
        return encode( keystorePath+aliasName, password);
    }

    public String encodeAliasPassword( String keystorePath, String aliasName, char[] password) {
        return encode( keystorePath+aliasName, password);
    }

    public char[] decodeKeystorePassword( String keystorePath, String password) {
        return decode(keystorePath,password);
    }

    public char[] decodeAliasPassword( String keystorePath, String aliasName, String password) {
        return decode(keystorePath+aliasName,password);
    }

    public String encode( String junk, String password) {
        if (password == null) return null;
        char[] c = password.toCharArray();
        String result = encode( junk, c);
        flush(c);
        return result;
    }

    public String encode( String junk, char[] password) {
        if (password == null) return null;
        try {
            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer w = new OutputStreamWriter(baos);
            w.write(junk);
            w.write(password);
            w.flush();
            byte[] encoded = cipher.doFinal( baos.toByteArray());
            return Base64.encode( encoded);
        } catch (Exception x) {
            logger.error("Failed to obfuscate password", x);
        }
        return null;
    }
    
    public char[] decode( String junk, String password) {
        if (password == null) return null;
        try {
            // Instantiate the cipher  
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec skeySpec = new SecretKeySpec(x.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] bytes = cipher.doFinal( Base64.decode(password.getBytes()));
            BufferedReader r = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( bytes)));
            char[] cb = new char[128];
            int length = 0;
            int numRead;
            while ((numRead = r.read(cb, length, 128-length)) != -1) {
                length += numRead;
            }
            
            if (length <= junk.length()) return null;
            
            char[] result = new char[ length - junk.length()];
            int j = 0;
            for (int i = junk.length(); i < length; i++) {
                result[j] = cb[i];
                j += 1;
            }
            flush(cb);
            return result;
            
        } catch (Exception x) {
            logger.error("Failed to decode password", x);
        }
        return null;
    }
    
    public static void flush( char[] charArray) {
        if (charArray == null) return;
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = '\0';
        }
    }

    public static void flush( byte[] charArray) {
        if (charArray == null) return;
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = 0;
        }
    }
}
