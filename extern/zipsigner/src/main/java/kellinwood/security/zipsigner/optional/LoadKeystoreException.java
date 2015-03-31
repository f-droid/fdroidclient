package kellinwood.security.zipsigner.optional;

import java.io.IOException;

/**
 * Thrown by JKS.engineLoad() for errors that occur after determining the keystore is actually a JKS keystore.
 */
public class LoadKeystoreException extends IOException {

    public LoadKeystoreException() {
    }

    public LoadKeystoreException(String message) {
        super(message);
    }

    public LoadKeystoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadKeystoreException(Throwable cause) {
        super(cause);
    }
}
