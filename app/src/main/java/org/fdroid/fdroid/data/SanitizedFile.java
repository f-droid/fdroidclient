package org.fdroid.fdroid.data;

import java.io.File;

/**
 * File guaranteed to have a santitized name (though not a sanitized path to the parent dir).
 * Useful so that we can use Java's type system to enforce that the file we are accessing
 * doesn't contain illegal characters.
 * Sanitized names are those which only have the following characters: [A-Za-z0-9.-_]
 */
@SuppressWarnings("serial")
public class SanitizedFile extends File {

    /**
     * Removes anything that is not an alpha numeric character, or one of "-", ".", or "_".
     */
    public static String sanitizeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9-._ ]", "");
    }

    /**
     * The "name" argument is assumed to be a file name, _not including any path separators_.
     * If it is a relative path to be appended to "parent", such as "/blah/sneh.txt", then
     * the forward slashes will be removed and it will be assumed you meant "blahsneh.txt".
     */
    public SanitizedFile(File parent, String name) {
        super(parent, sanitizeFileName(name));
    }

    /**
     * Used by the {@link org.fdroid.fdroid.data.SanitizedFile#knownSanitized(java.io.File)}
     * method, but intentionally kept private so people don't think that any sanitization
     * will occur by passing a file in - because it wont.
     */
    private SanitizedFile(File file) {
        super(file.getAbsolutePath());
    }

    /**
     * This is dangerous, but there will be some cases when all we have is an absolute file
     * path that wasn't given to us from user input. One example would be asking Android for
     * the path to an installed .apk on disk. In such situations, we can't meaningfully
     * sanitize it, but will still need to pass to a function which only allows SanitizedFile's
     * as arguments (because they interact with, e.g. shells).
     * <p>
     * To illustrate, imagine perfectly valid file path: "/tmp/../secret/file.txt",
     * one cannot distinguish between:
     * <p>
     * "/tmp/" (known safe directory) + "../secret/file.txt" (suspicious looking file name)
     * <p>
     * and
     * <p>
     * "/tmp/../secret/" (known safe directory) + "file.txt" (known safe file name)
     * <p>
     * I guess the best this method offers us is the ability to uniquely trace the different
     * ways in which files are created and handled. It should make it easier to find and
     * prevent suspect usages of methods which only expect SanitizedFile's, but are given
     * a SanitizedFile returned from this method that really originated from user input.
     */
    public static SanitizedFile knownSanitized(String path) {
        return new SanitizedFile(new File(path));
    }

    /**
     * @see org.fdroid.fdroid.data.SanitizedFile#knownSanitized(String)
     */
    public static SanitizedFile knownSanitized(File file) {
        return new SanitizedFile(file);
    }

}
