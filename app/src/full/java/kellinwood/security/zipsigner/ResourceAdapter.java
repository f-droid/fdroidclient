package kellinwood.security.zipsigner;

/**
 * Interface to obtain internationalized strings for the progress events.
 */
public interface ResourceAdapter {

    public enum Item {
        INPUT_SAME_AS_OUTPUT_ERROR,
        AUTO_KEY_SELECTION_ERROR,
        LOADING_CERTIFICATE_AND_KEY,
        PARSING_CENTRAL_DIRECTORY,
        GENERATING_MANIFEST,
        GENERATING_SIGNATURE_FILE,
        GENERATING_SIGNATURE_BLOCK,
        COPYING_ZIP_ENTRY
    };

    public String getString(Item item, Object... args);
}
