package kellinwood.security.zipsigner;

public class AutoKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AutoKeyException( String message) {
        super(message);
    }
    
    public AutoKeyException( String message, Throwable cause) {
        super(message, cause);
    }    
}
