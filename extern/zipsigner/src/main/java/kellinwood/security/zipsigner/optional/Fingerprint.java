package kellinwood.security.zipsigner.optional;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;
import kellinwood.security.zipsigner.Base64;
import org.spongycastle.util.encoders.HexTranslator;

import java.security.MessageDigest;

/**
 * User: ken
 * Date: 1/17/13
 */
public class Fingerprint {

    static LoggerInterface logger = LoggerManager.getLogger(Fingerprint.class.getName());

    static byte[] calcDigest( String algorithm, byte[] encodedCert) {
        byte[] result = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(encodedCert);
            result = messageDigest.digest();
        } catch (Exception x) {
            logger.error(x.getMessage(),x);
        }
        return result;
    }

    public static String hexFingerprint( String algorithm, byte[] encodedCert) {
        try {
            byte[] digest = calcDigest(algorithm,encodedCert);
            if (digest == null) return null;
            HexTranslator hexTranslator = new HexTranslator();
            byte[] hex = new byte[digest.length * 2];
            hexTranslator.encode(digest, 0, digest.length, hex, 0);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < hex.length; i += 2) {
                builder.append((char)hex[i]);
                builder.append((char)hex[i+1]);
                if (i != (hex.length - 2)) builder.append(':');
            }
            return builder.toString().toUpperCase();
        } catch (Exception x) {
            logger.error(x.getMessage(),x);
        }
        return null;
    }

//    public static void main(String[] args) {
//        byte[] data = "The Silence of the Lambs is a really good movie.".getBytes();
//        System.out.println(hexFingerprint("MD5", data));
//        System.out.println(hexFingerprint("SHA1", data));
//        System.out.println(base64Fingerprint("SHA1", data));
//
//    }

    public static String base64Fingerprint( String algorithm, byte[] encodedCert) {
        String result = null;
        try {
            byte[] digest = calcDigest(algorithm,encodedCert);
            if (digest == null) return result;
            return Base64.encode(digest);
        } catch (Exception x) {
            logger.error(x.getMessage(),x);
        }
        return result;
    }
}
