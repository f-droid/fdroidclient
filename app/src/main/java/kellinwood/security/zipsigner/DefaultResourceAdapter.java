package kellinwood.security.zipsigner;

/**
 * Default resource adapter.
 */
public class DefaultResourceAdapter implements ResourceAdapter {

    @Override
    public String getString(Item item, Object... args) {

        switch (item) {
            case INPUT_SAME_AS_OUTPUT_ERROR:
                return "Input and output files are the same.  Specify a different name for the output.";
            case AUTO_KEY_SELECTION_ERROR:
                return "Unable to auto-select key for signing " + args[0];
            case LOADING_CERTIFICATE_AND_KEY:
                return "Loading certificate and private key";
            case PARSING_CENTRAL_DIRECTORY:
                return "Parsing the input's central directory";
            case GENERATING_MANIFEST:
                return "Generating manifest";
            case GENERATING_SIGNATURE_FILE:
                return "Generating signature file";
            case GENERATING_SIGNATURE_BLOCK:
                return "Generating signature block file";
            case COPYING_ZIP_ENTRY:
                return String.format("Copying zip entry %d of %d", args[0], args[1]);
            default:
                throw new IllegalArgumentException("Unknown item " + item);
        }

    }
}
