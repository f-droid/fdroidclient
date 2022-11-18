package org.fdroid.fdroid.nearby;

import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.fdroid.index.v1.IndexV1VerifierKt;

/**
 * @see <a href="https://stackoverflow.com/a/36162691">Android 5.0 DocumentFile from tree URI</a>
 */
public final class TreeUriUtils {
    public static final String TAG = "TreeUriUtils";

    static final String SIGNED_FILE_NAME = IndexV1UpdaterKt.SIGNED_FILE_NAME;
    static final String DATA_FILE_NAME = IndexV1VerifierKt.JSON_FILE_NAME;

}